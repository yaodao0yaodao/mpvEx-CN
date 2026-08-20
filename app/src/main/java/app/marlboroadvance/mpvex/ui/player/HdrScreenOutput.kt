package app.marlboroadvance.mpvex.ui.player

import `is`.xyz.mpv.MPVLib

enum class VideoDynamicRange { UNKNOWN, SDR, HDR }

private val HDR_TRANSFERS = setOf("pq", "hlg", "st2084", "smpte2084")

internal fun classifyVideoDynamicRange(
  transfer: String?,
  signalPeak: Double?,
): VideoDynamicRange {
  val normalizedTransfer = transfer?.trim()?.lowercase()?.replace(".", "")
  if (normalizedTransfer in HDR_TRANSFERS || (signalPeak != null && signalPeak > 1.01)) {
    return VideoDynamicRange.HDR
  }
  return if (!normalizedTransfer.isNullOrEmpty() || signalPeak != null) VideoDynamicRange.SDR
  else VideoDynamicRange.UNKNOWN
}

internal fun linearHdrSettings(
  pipelineReady: Boolean,
  boostSdrToHdr: Boolean,
): List<Pair<String, String>> =
  listOf(
    "target-colorspace-hint" to if (pipelineReady) "yes" else "auto",
    "target-colorspace-hint-mode" to "target",
    "target-prim" to "auto",
    "target-trc" to "auto",
    "target-peak" to "auto",
    "inverse-tone-mapping" to if (pipelineReady && boostSdrToHdr) "yes" else "no",
    "tone-mapping" to if (pipelineReady) "clip" else "auto",
    "gamut-mapping-mode" to if (pipelineReady) "clip" else "auto",
    "hdr-compute-peak" to if (pipelineReady) "yes" else "auto",
    "hdr-reference-white" to "203",
    "tone-mapping-visualize" to "no",
    "glsl-shader-opts" to "",
  )

fun applyLinearHdrOutputOptions(pipelineReady: Boolean, boostSdrToHdr: Boolean) {
  linearHdrSettings(pipelineReady, boostSdrToHdr).forEach { (property, value) ->
    MPVLib.setOptionString(property, value)
  }
}

fun applySdrHdrBoostProperty(enabled: Boolean) {
  MPVLib.setPropertyString("inverse-tone-mapping", if (enabled) "yes" else "no")
}
