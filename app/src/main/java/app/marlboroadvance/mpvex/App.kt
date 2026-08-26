package app.marlboroadvance.mpvex

import android.app.Application
import android.os.Build
import app.marlboroadvance.mpvex.database.repository.VideoMetadataCacheRepository
import app.marlboroadvance.mpvex.di.DatabaseModule
import app.marlboroadvance.mpvex.di.FileManagerModule
import app.marlboroadvance.mpvex.di.PreferencesModule
import app.marlboroadvance.mpvex.presentation.crash.CrashActivity
import app.marlboroadvance.mpvex.presentation.crash.GlobalExceptionHandler
import app.marlboroadvance.mpvex.preferences.AudioPreferences
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.DecoderPreferences
import app.marlboroadvance.mpvex.ui.player.MPVProfile
import app.marlboroadvance.mpvex.domain.anime4k.Anime4KManager
import app.marlboroadvance.mpvex.utils.media.MediaLibraryEvents
import `is`.xyz.mpv.FastThumbnails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.annotation.KoinExperimentalAPI
import java.io.File

@OptIn(KoinExperimentalAPI::class)
class App : Application() {
  private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val metadataCache: VideoMetadataCacheRepository by inject()

  override fun onCreate() {
    super.onCreate()

    // DecoderRestartActivity lives in a tiny isolated process. Initialising Koin,
    // thumbnail engines and media scanners there would duplicate application-wide
    // state just before it hands playback back to a fresh main process.
    if (isDecoderRestartProcess()) return

    // Initialize Koin
    val koinApplication = startKoin {
      androidContext(this@App)
      modules(
        PreferencesModule,
        DatabaseModule,
        FileManagerModule,
        app.marlboroadvance.mpvex.di.domainModule,
      )
    }

    // These capabilities are part of the player contract now, not user-selectable options.
    // Writing them on every startup also migrates users who disabled them in an older build.
    koinApplication.koin.get<PlayerPreferences>().apply {
      playlistMode.set(true)
      showDoubleTapOvals.set(true)
    }
    koinApplication.koin.get<AudioPreferences>().audioPitchCorrection.set(true)
    koinApplication.koin.get<DecoderPreferences>().profile.let { profile ->
      if (MPVProfile.entries.none { it.value == profile.get() }) profile.set(MPVProfile.Fast.value)
    }
    koinApplication.koin.get<DecoderPreferences>().apply {
      enableAnime4K.set(true)
      anime4kMode.set(
        when (anime4kMode.get()) {
          "ARTCNN" -> Anime4KManager.Mode.ANI4KV2_ARTCNN_C4F32_CMP.name
          "A_PLUS", "B_PLUS" -> Anime4KManager.Mode.C_PLUS.name
          else -> runCatching { Anime4KManager.Mode.valueOf(anime4kMode.get()).name }
            .getOrDefault(Anime4KManager.Mode.OFF.name)
        },
      )
    }

    Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(applicationContext, CrashActivity::class.java))

    FastThumbnails.initialize(this)

    // Perform cache maintenance on app startup (non-blocking)
    applicationScope.launch {
      runCatching {
        metadataCache.performMaintenance()
      }
    }
    
    // Trigger media scan on app launch to detect new videos
    applicationScope.launch {
      runCatching {
        triggerMediaScanOnLaunch()
      }
    }
  }

  private fun isDecoderRestartProcess(): Boolean {
    val processName =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        getProcessName()
      } else {
        runCatching { File("/proc/self/cmdline").readText().trimEnd('\u0000') }.getOrDefault("")
      }
    return processName.endsWith(":decoder_restart")
  }
  
  /**
   * Trigger a media scan on app launch to ensure MediaStore is up-to-date
   * This helps detect videos added by external apps while the app was closed
   */
  private fun triggerMediaScanOnLaunch() {
    try {
      val externalStorage = android.os.Environment.getExternalStorageDirectory()
      
      android.media.MediaScannerConnection.scanFile(
        this,
        arrayOf(externalStorage.absolutePath),
        null, // Let MediaScanner detect all media types
      ) { path, uri ->
        android.util.Log.d("App", "Launch media scan completed for: $path")
        // Notify the app that media library may have changed
        MediaLibraryEvents.notifyChanged()
      }
      
      android.util.Log.d("App", "Triggered media scan on app launch")
    } catch (e: Exception) {
      android.util.Log.e("App", "Failed to trigger media scan on launch", e)
    }
  }
}
