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
  // Each decoder is attempted explicitly by the application. Letting mpv see
  // the complete list would silently skip priority entries placed after SW.
  return priority.firstOrNull { it in Decoder.priorityModes }?.value ?: Decoder.SW.value
}

internal fun decoderFallbackOrder(priority: List<Decoder>, first: Decoder): List<Decoder> {
  // The caller passes the effective priority, which has already removed HW+
  // when the user did not opt in. Never add disabled modes back here.
  val normalized = priority.distinct().filter { it in Decoder.priorityModes }.ifEmpty { listOf(Decoder.SW) }
  val start = normalized.indexOf(first).takeIf { it >= 0 } ?: 0
  return List(normalized.size) { normalized[(start + it) % normalized.size] }
}

internal fun nextDecoderFallback(
  priority: List<Decoder>,
  first: Decoder,
  attempted: Set<Decoder>,
): Decoder? = decoderFallbackOrder(priority, first).firstOrNull { it !in attempted }

internal fun meetsAiUpscaleThreshold(
  widthScale: Float,
  heightScale: Float,
): Boolean = widthScale >= 1.3f && heightScale >= 1.3f

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
