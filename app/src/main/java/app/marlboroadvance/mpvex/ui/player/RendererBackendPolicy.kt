package app.marlboroadvance.mpvex.ui.player

data class RendererBackend(
  val videoOutput: String,
  val gpuApi: String,
  val gpuContext: String,
)

internal fun initialHwdecValue(
  selectedDecoder: Decoder?,
  decoderPriority: List<Decoder>,
): String =
  selectedDecoder?.value ?: decoderPriorityHwdecValue(decoderPriority)

internal fun decoderPriorityHwdecValue(priority: List<Decoder>): String {
  val normalized = (priority + Decoder.priorityModes).distinct().filter { it in Decoder.priorityModes }
  val enabled = normalized.takeWhile { it != Decoder.SW }
  return if (enabled.isEmpty()) Decoder.SW.value else (enabled.map(Decoder::value) + Decoder.SW.value).joinToString(",")
}

internal fun selectRendererBackend(
  gpuNextEnabled: Boolean,
  vulkanEnabled: Boolean,
  vulkanSupported: Boolean,
  anime4kActive: Boolean,
  hdrActive: Boolean,
  hardwarePlusMode: Boolean = false,
): RendererBackend {
  // HW+ keeps gpu-next when requested, but uses the OpenGL path because Vulkan,
  // linear HDR and Anime4K are suspended for this playback mode.
  if (hardwarePlusMode) {
    return RendererBackend(if (gpuNextEnabled) "gpu-next" else "gpu", "opengl", "android")
  }

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
