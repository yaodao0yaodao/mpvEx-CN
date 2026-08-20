package app.marlboroadvance.mpvex.ui.player

data class RendererBackend(
  val videoOutput: String,
  val gpuApi: String,
  val gpuContext: String,
)

internal fun selectRendererBackend(
  gpuNextEnabled: Boolean,
  vulkanEnabled: Boolean,
  vulkanSupported: Boolean,
  anime4kActive: Boolean,
  hdrActive: Boolean,
): RendererBackend {
  val useVulkan = vulkanSupported && (vulkanEnabled || hdrActive)
  val useGpuNext = gpuNextEnabled || hdrActive

  if (anime4kActive && useGpuNext && !useVulkan && !hdrActive) {
    return RendererBackend("gpu", "opengl", "android")
  }
  if (useGpuNext && useVulkan) return RendererBackend("gpu-next", "vulkan", "androidvk")
  if (useGpuNext) return RendererBackend("gpu-next", "opengl", "android")
  if (useVulkan) return RendererBackend("gpu", "vulkan", "androidvk")
  return RendererBackend("gpu", "opengl", "android")
}
