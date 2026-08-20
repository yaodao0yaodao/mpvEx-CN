package app.marlboroadvance.mpvex.domain.hdr

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class HdrToysManager(private val context: Context) {
  private var initialized = false

  @Synchronized
  fun initialize(): Boolean {
    if (initialized && requiredShadersExist()) return true

    return runCatching {
      val destination = File(context.filesDir, TARGET_DIR)
      destination.mkdirs()
      copyAssetDirectory(ASSET_DIR, destination)
      requiredShadersExist().also { initialized = it }
    }.onFailure { error ->
      initialized = false
      Log.w(TAG, "Failed to initialize hdr-toys shaders", error)
    }.getOrDefault(false)
  }

  fun getShaderPaths(profile: HdrToysProfile): List<String> {
    if (!initialize()) return emptyList()
    return profile.shaderPaths.map { relativePath ->
      File(context.filesDir, "$TARGET_DIR/$relativePath").absolutePath
    }
  }

  private fun requiredShadersExist(): Boolean =
    HdrToysProfile.entries
      .flatMap { it.shaderPaths }
      .distinct()
      .all { relativePath ->
        File(context.filesDir, "$TARGET_DIR/$relativePath").let { it.exists() && it.length() > 0L }
      }

  private fun copyAssetDirectory(assetPath: String, destination: File) {
    destination.mkdirs()
    context.assets.list(assetPath).orEmpty().forEach { child ->
      val childAssetPath = "$assetPath/$child"
      val childDestination = File(destination, child)
      val nestedChildren = context.assets.list(childAssetPath).orEmpty()
      if (nestedChildren.isEmpty()) {
        copyAssetFile(childAssetPath, childDestination)
      } else {
        copyAssetDirectory(childAssetPath, childDestination)
      }
    }
  }

  private fun copyAssetFile(assetPath: String, destination: File) {
    destination.parentFile?.mkdirs()
    context.assets.open(assetPath).use { input ->
      FileOutputStream(destination).use { output -> input.copyTo(output) }
    }
  }

  private companion object {
    const val TAG = "HdrToysManager"
    const val ASSET_DIR = "shaders/hdr-toys"
    const val TARGET_DIR = "shaders/hdr-toys"
  }
}
