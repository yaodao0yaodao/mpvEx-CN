package app.marlboroadvance.mpvex.ui.player

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object VulkanCapabilities {
  fun isDeviceSupported(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false

    return runCatching {
      val packageManager = context.packageManager
      val openGlVersion =
        packageManager.systemAvailableFeatures
          .firstOrNull { it.name == null }
          ?.reqGlEsVersion ?: 0
      openGlVersion >= 0x00030001 &&
        packageManager.hasSystemFeature(
          PackageManager.FEATURE_VULKAN_HARDWARE_VERSION,
          0x00403000,
        )
    }.getOrDefault(false)
  }
}
