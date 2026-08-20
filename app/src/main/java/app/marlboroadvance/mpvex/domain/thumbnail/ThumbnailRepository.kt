package app.marlboroadvance.mpvex.domain.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.LruCache
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.utils.media.MediaInfoOps
import `is`.xyz.mpv.FastThumbnails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

class ThumbnailRepository(
  private val context: Context,
) {
  private val appearancePreferences by lazy { 
    org.koin.java.KoinJavaComponent.get<app.marlboroadvance.mpvex.preferences.AppearancePreferences>(
      app.marlboroadvance.mpvex.preferences.AppearancePreferences::class.java
    ) 
  }
  private val diskCacheDimension = 1024
  private val diskJpegQuality = 100
  private val memoryCache: LruCache<String, Bitmap>
  private val diskDir: File = File(context.filesDir, "thumbnails").apply { mkdirs() }
  private val ongoingOperations = ConcurrentHashMap<String, Deferred<Bitmap?>>()

  private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val maxconcurrentfolders = 3

  private data class FolderState(
    val signature: String,
    @Volatile var nextIndex: Int = 0,
  )

  private val folderStates = ConcurrentHashMap<String, FolderState>()
  private val folderJobs = ConcurrentHashMap<String, Job>()
  
  // Track videos that failed with FastThumbnails and should use MediaStore
  private val useMediaStoreForVideo = ConcurrentHashMap<String, Boolean>()

  private val _thumbnailReadyKeys =
    MutableSharedFlow<String>(
      extraBufferCapacity = 256,
    )
  val thumbnailReadyKeys: SharedFlow<String> = _thumbnailReadyKeys.asSharedFlow()

  init {
    val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
    val cacheSizeKb = maxMemoryKb / 6
    memoryCache =
      object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(
          key: String,
          value: Bitmap,
        ): Int = value.byteCount / 1024
      }
  }

  suspend fun getThumbnail(
    video: Video,
    widthPx: Int,
    heightPx: Int,
  ): Bitmap? =
    withContext(Dispatchers.IO) {
      val key = thumbnailKey(video, widthPx, heightPx)

      if (isNetworkUrl(video.path) && !appearancePreferences.showNetworkThumbnails.get()) {
        return@withContext null
      }

      memoryCache.get(key)?.let { return@withContext it }

      ongoingOperations[key]?.let {
        return@withContext it.await()
      }

      val deferred =
        async {
          try {
            loadFromDisk(video)?.let { thumbnail ->
              memoryCache.put(key, thumbnail)
              _thumbnailReadyKeys.tryEmit(key)
              return@async thumbnail
            }

            if (isNetworkUrl(video.path) && !appearancePreferences.showNetworkThumbnails.get()) {
              return@async null
            }

            // Check if this video should use MediaStore
            val videoKey = videoBaseKey(video)
            val thumbnail = if (useMediaStoreForVideo.containsKey(videoKey)) {
              // Use MediaStore for this video
              android.util.Log.d("ThumbnailRepository", "Using MediaStore for ${video.displayName}")
              generateWithMediaStore(video, diskCacheDimension)
            } else {
              // Try FastThumbnails first
              val fastResult = generateWithFastThumbnails(video, diskCacheDimension)
              if (fastResult == null) {
                // FastThumbnails failed, mark for MediaStore and try it
                android.util.Log.w("ThumbnailRepository", "FastThumbnails failed for ${video.displayName}, falling back to MediaStore")
                useMediaStoreForVideo[videoKey] = true
                generateWithMediaStore(video, diskCacheDimension)
              } else {
                fastResult
              }
            }

            if (thumbnail == null) {
              return@async null
            }

            memoryCache.put(key, thumbnail)
            _thumbnailReadyKeys.tryEmit(key)
            writeToDisk(video, thumbnail)

            thumbnail
          } finally {
            ongoingOperations.remove(key)
          }
        }

      ongoingOperations[key] = deferred
      return@withContext deferred.await()
    }

  suspend fun getThumbnailForUri(
    uri: Uri,
    path: String,
    displayName: String,
    widthPx: Int,
    heightPx: Int,
  ): Bitmap? {
    val video = withContext(Dispatchers.IO) { resolveVideo(uri, path, displayName) }
    return getThumbnail(video, widthPx, heightPx)
  }

  suspend fun getCachedThumbnail(
    video: Video,
    widthPx: Int,
    heightPx: Int,
  ): Bitmap? =
    withContext(Dispatchers.IO) {
      if (isNetworkUrl(video.path) && !appearancePreferences.showNetworkThumbnails.get()) {
        return@withContext null
      }
      
      val key = thumbnailKey(video, widthPx, heightPx)
      synchronized(memoryCache) { memoryCache.get(key) }?.let { return@withContext it }
      loadFromDisk(video)?.let { thumbnail ->
        synchronized(memoryCache) { memoryCache.put(key, thumbnail) }
        return@withContext thumbnail
      }
      null
    }

  fun getThumbnailFromMemory(
    video: Video,
    widthPx: Int,
    heightPx: Int,
  ): Bitmap? {
    if (isNetworkUrl(video.path) && !appearancePreferences.showNetworkThumbnails.get()) {
      return null
    }
    
    val key = thumbnailKey(video, widthPx, heightPx)
    return synchronized(memoryCache) { memoryCache.get(key) }
  }

  fun clearThumbnailCache() {
    folderJobs.values.forEach { it.cancel() }
    folderJobs.clear()
    folderStates.clear()
    ongoingOperations.clear()
    useMediaStoreForVideo.clear()

    synchronized(memoryCache) {
      memoryCache.evictAll()
    }

    runCatching {
      if (diskDir.exists()) {
        diskDir.listFiles()?.forEach { it.delete() }
      }
    }
  }

  fun startFolderThumbnailGeneration(
    folderId: String,
    videos: List<Video>,
    widthPx: Int,
    heightPx: Int,
  ) {
    val filteredVideos = if (appearancePreferences.showNetworkThumbnails.get()) {
      videos
    } else {
      videos.filterNot { isNetworkUrl(it.path) }
    }
    
    if (filteredVideos.isEmpty()) return
    
    folderJobs.entries.removeAll { !it.value.isActive }
    
    if (folderJobs.size >= maxconcurrentfolders && !folderJobs.containsKey(folderId)) {
      folderJobs.entries.firstOrNull()?.let { (oldestId, job) ->
        job.cancel()
        folderJobs.remove(oldestId)
        folderStates.remove(oldestId)
      }
    }
    
    val signature = folderSignature(filteredVideos, widthPx, heightPx)
    val state =
      folderStates.compute(folderId) { _, existing ->
        if (existing == null || existing.signature != signature) {
          FolderState(signature = signature, nextIndex = 0)
        } else {
          existing
        }
      }!!

    folderJobs.remove(folderId)?.cancel()
    folderJobs[folderId] =
      repositoryScope.launch {
        var i = state.nextIndex
        while (i < filteredVideos.size) {
          val video = filteredVideos[i]
          getThumbnail(video, widthPx, heightPx)
          i++
          state.nextIndex = i
        }
      }
  }

  fun thumbnailKey(
    video: Video,
    width: Int,
    height: Int,
  ): String {
    val base = videoBaseKey(video)
    return "$base|$width|$height"
  }

  private fun videoBaseKey(video: Video): String {
    if (isNetworkUrl(video.path)) {
      val base = video.path.ifBlank { video.uri.toString() }
      return "$base|network"
    }
    
    val source = video.path.ifBlank { video.uri.toString() }
    return "$source|${video.size}|${video.dateModified}|${video.duration}"
  }

  private fun keyToFileName(key: String): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(key.toByteArray())
    val hex = digest.joinToString("") { b -> "%02x".format(b) }
    return "$hex.jpg"
  }

  private fun diskKey(video: Video): String {
    val baseKey = videoBaseKey(video)
    return if (isNetworkUrl(video.path)) {
      "$baseKey|disk|d$diskCacheDimension|pos3"
    } else {
      "$baseKey|disk|d$diskCacheDimension"
    }
  }

  private fun loadFromDisk(video: Video): Bitmap? {
    val diskFile = File(diskDir, keyToFileName(diskKey(video)))
    if (!diskFile.exists()) return null
    return runCatching {
      val options =
        BitmapFactory.Options().apply {
          inPreferredConfig = Bitmap.Config.ARGB_8888
        }
      BitmapFactory.decodeFile(diskFile.absolutePath, options)
    }.getOrNull()
  }

  private fun writeToDisk(video: Video, bitmap: Bitmap) {
    val diskFile = File(diskDir, keyToFileName(diskKey(video)))
    runCatching {
      FileOutputStream(diskFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, diskJpegQuality, out)
        out.flush()
      }
    }
  }

  private suspend fun rotateIfNeeded(
    video: Video,
    bitmap: Bitmap
  ): Bitmap {
    val rotation = MediaInfoOps.getRotation(context, video.uri, video.displayName)
    if (rotation == 0) return bitmap
    val matrix = android.graphics.Matrix()
    matrix.postRotate(rotation.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }

  private suspend fun generateWithFastThumbnails(
    video: Video,
    dimension: Int,
  ): Bitmap? {
    return runCatching {
      val positionSec = preferredPositionSeconds(video)
      
      val bmp = FastThumbnails.generateAsync(
          video.path.ifBlank { video.uri.toString() },
          positionSec,
          dimension,
          useHwDec = false
      ) ?: return@runCatching null
      rotateIfNeeded(video, bmp)
    }.getOrNull()
  }

  private suspend fun generateWithMediaStore(
    video: Video,
    dimension: Int,
  ): Bitmap? {
    // MediaStore only works for local files, not network URLs
    if (isNetworkUrl(video.path)) {
      android.util.Log.w("ThumbnailRepository", "Cannot use MediaStore for network URL: ${video.path}")
      return null
    }
    
    return withContext(Dispatchers.IO) {
      // Try MediaStore first
      val mediaStoreThumbnail = runCatching {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
          // Use modern API for Android Q+
          // Build proper MediaStore content URI
          val contentUri = if (video.uri.scheme == "content") {
            video.uri
          } else {
            android.content.ContentUris.withAppendedId(
              android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
              video.id
            )
          }
          android.util.Log.d("ThumbnailRepository", "Generating MediaStore thumbnail for ${video.displayName} using loadThumbnail")
          val thumbnail = context.contentResolver.loadThumbnail(
            contentUri,
            android.util.Size(dimension, dimension),
            null
          )
          android.util.Log.d("ThumbnailRepository", "MediaStore thumbnail generated successfully for ${video.displayName}")
          rotateIfNeeded(video, thumbnail)
        } else {
          // Use legacy API for older versions
          android.util.Log.d("ThumbnailRepository", "Generating MediaStore thumbnail for ${video.displayName} using getThumbnail")
          @Suppress("DEPRECATION")
          val thumbnail = android.provider.MediaStore.Video.Thumbnails.getThumbnail(
            context.contentResolver,
            video.id,
            android.provider.MediaStore.Video.Thumbnails.MINI_KIND,
            null
          )
          if (thumbnail != null) {
            // Scale to desired dimension
            val scaled = Bitmap.createScaledBitmap(
              thumbnail,
              dimension,
              (dimension * thumbnail.height) / thumbnail.width,
              true
            )
            if (scaled != thumbnail) {
              thumbnail.recycle()
            }
            android.util.Log.d("ThumbnailRepository", "MediaStore thumbnail generated successfully for ${video.displayName}")
            rotateIfNeeded(video, scaled)
          } else {
            android.util.Log.w("ThumbnailRepository", "MediaStore returned null thumbnail for ${video.displayName}")
            null
          }
        }
      }.onFailure { e ->
        android.util.Log.w("ThumbnailRepository", "MediaStore thumbnail failed for ${video.displayName}, will try ThumbnailUtils: ${e.message}")
      }.getOrNull()
      
      // If MediaStore failed, try ThumbnailUtils as last resort
      if (mediaStoreThumbnail != null) {
        return@withContext mediaStoreThumbnail
      }
      
      // Fallback to ThumbnailUtils (extracts directly from file)
      runCatching {
        android.util.Log.d("ThumbnailRepository", "Generating thumbnail using ThumbnailUtils for ${video.displayName}")
        val file = java.io.File(video.path)
        if (!file.exists()) {
          android.util.Log.e("ThumbnailRepository", "File does not exist: ${video.path}")
          return@runCatching null
        }
        
        val thumbnail = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
          android.media.ThumbnailUtils.createVideoThumbnail(
            file,
            android.util.Size(dimension, dimension),
            null
          )
        } else {
          @Suppress("DEPRECATION")
          android.media.ThumbnailUtils.createVideoThumbnail(
            video.path,
            android.provider.MediaStore.Video.Thumbnails.MINI_KIND
          )?.let { thumb ->
            // Scale to desired dimension
            Bitmap.createScaledBitmap(
              thumb,
              dimension,
              (dimension * thumb.height) / thumb.width,
              true
            ).also {
              if (it != thumb) thumb.recycle()
            }
          }
        }
        
        if (thumbnail != null) {
          android.util.Log.d("ThumbnailRepository", "ThumbnailUtils thumbnail generated successfully for ${video.displayName}")
          rotateIfNeeded(video, thumbnail)
        } else {
          android.util.Log.e("ThumbnailRepository", "ThumbnailUtils returned null for ${video.displayName}")
          null
        }
      }.onFailure { e ->
        android.util.Log.e("ThumbnailRepository", "ThumbnailUtils thumbnail generation failed for ${video.displayName}", e)
      }.getOrNull()
    }
  }

  private fun preferredPositionSeconds(video: Video): Double {
    val isNetworkUrl = isNetworkUrl(video.path)
    
    if (isNetworkUrl) {
      val durationSec = video.duration / 1000.0
      
      if (durationSec > 0.0) {
        return 2.0.coerceIn(0.0, max(0.0, durationSec - 0.1))
      }
      
      return 2.0
    }
    
    val durationSec = video.duration / 1000.0
    
    if (durationSec <= 0.0) return 3.0
    if (durationSec < 20.0) return 0.0
    
    val candidate = 3.0
    
    return candidate.coerceIn(0.0, max(0.0, durationSec - 0.1))
  }
  
  private fun isNetworkUrl(path: String): Boolean {
    return path.startsWith("http://", ignoreCase = true) ||
      path.startsWith("https://", ignoreCase = true) ||
      path.startsWith("rtmp://", ignoreCase = true) ||
      path.startsWith("rtsp://", ignoreCase = true) ||
      path.startsWith("ftp://", ignoreCase = true) ||
      path.startsWith("sftp://", ignoreCase = true)
  }

  private fun resolveVideo(uri: Uri, suppliedPath: String, displayName: String): Video {
    val projection = arrayOf(
      MediaStore.Video.Media._ID,
      MediaStore.Video.Media.DATA,
      MediaStore.Video.Media.DURATION,
      MediaStore.Video.Media.SIZE,
      MediaStore.Video.Media.DATE_MODIFIED,
      MediaStore.Video.Media.DATE_ADDED,
      MediaStore.Video.Media.WIDTH,
      MediaStore.Video.Media.HEIGHT,
      MediaStore.Video.Media.MIME_TYPE,
    )

    var id = runCatching { android.content.ContentUris.parseId(uri) }.getOrDefault(-1L)
    var resolvedPath = when {
      uri.scheme == "file" -> uri.path.orEmpty()
      suppliedPath.startsWith("file:", ignoreCase = true) -> Uri.parse(suppliedPath).path.orEmpty()
      suppliedPath.startsWith("content:", ignoreCase = true) -> ""
      else -> suppliedPath
    }
    var duration = 0L
    var size = 0L
    var dateModified = 0L
    var dateAdded = 0L
    var width = 0
    var height = 0
    var mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()

    runCatching {
      val queryUri: Uri
      val selection: String?
      val selectionArgs: Array<String>?
      if (uri.scheme == "content") {
        queryUri = uri
        selection = null
        selectionArgs = null
      } else {
        queryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        selection = "${MediaStore.Video.Media.DATA} = ?"
        selectionArgs = arrayOf(resolvedPath)
      }

      context.contentResolver.query(queryUri, projection, selection, selectionArgs, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          fun long(column: String): Long = cursor.getColumnIndex(column).takeIf { it >= 0 }?.let(cursor::getLong) ?: 0L
          fun int(column: String): Int = cursor.getColumnIndex(column).takeIf { it >= 0 }?.let(cursor::getInt) ?: 0
          fun string(column: String): String? = cursor.getColumnIndex(column).takeIf { it >= 0 }?.let(cursor::getString)

          id = long(MediaStore.Video.Media._ID).takeIf { it > 0 } ?: id
          resolvedPath = string(MediaStore.Video.Media.DATA).orEmpty().ifBlank { resolvedPath }
          duration = long(MediaStore.Video.Media.DURATION)
          size = long(MediaStore.Video.Media.SIZE)
          dateModified = long(MediaStore.Video.Media.DATE_MODIFIED)
          dateAdded = long(MediaStore.Video.Media.DATE_ADDED)
          width = int(MediaStore.Video.Media.WIDTH)
          height = int(MediaStore.Video.Media.HEIGHT)
          mimeType = string(MediaStore.Video.Media.MIME_TYPE).orEmpty().ifBlank { mimeType }
        }
      }
    }

    val file = resolvedPath.takeIf { it.isNotBlank() }?.let(::File)
    if (size <= 0L) size = file?.takeIf(File::exists)?.length() ?: 0L
    if (dateModified <= 0L) dateModified = file?.takeIf(File::exists)?.lastModified()?.div(1000L) ?: 0L

    return Video(
      id = id,
      title = displayName,
      displayName = displayName,
      path = resolvedPath.ifBlank { uri.toString() },
      uri = uri,
      duration = duration,
      durationFormatted = "",
      size = size,
      sizeFormatted = "",
      dateModified = dateModified,
      dateAdded = dateAdded,
      mimeType = mimeType,
      bucketId = "",
      bucketDisplayName = "",
      width = width,
      height = height,
      fps = 0f,
      resolution = if (width > 0 && height > 0) "${width}x$height" else "",
    )
  }

  private fun folderSignature(
    videos: List<Video>,
    widthPx: Int,
    heightPx: Int,
  ): String {
    val md = MessageDigest.getInstance("MD5")
    md.update("$widthPx|$heightPx|".toByteArray())
    for (v in videos) {
      md.update(v.path.toByteArray())
      md.update("|".toByteArray())
      md.update(v.size.toString().toByteArray())
      md.update("|".toByteArray())
      md.update(v.dateModified.toString().toByteArray())
      md.update(";".toByteArray())
    }
    return md.digest().joinToString("") { b -> "%02x".format(b) }
  }
}
