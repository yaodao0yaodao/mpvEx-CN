package app.marlboroadvance.mpvex.ui.player

import app.marlboroadvance.mpvex.domain.hdr.HdrToysProfile
import `is`.xyz.mpv.MPVLib

enum class VideoDynamicRange { UNKNOWN, SDR, HDR }

enum class VideoHdrType {
  UNKNOWN,
  SDR,
  PQ,
  HLG,
  BT2020,
  ;

  val dynamicRange: VideoDynamicRange
    get() =
      when (this) {
        UNKNOWN -> VideoDynamicRange.UNKNOWN
        SDR -> VideoDynamicRange.SDR
        PQ, HLG, BT2020 -> VideoDynamicRange.HDR
      }

  val hdrToysProfile: HdrToysProfile?
    get() =
      when (this) {
        PQ -> HdrToysProfile.BT_2100_PQ
        HLG -> HdrToysProfile.BT_2100_HLG
        BT2020 -> HdrToysProfile.BT_2020
        UNKNOWN, SDR -> null
      }
}

private val PQ_TRANSFERS = setOf("pq", "st2084", "smpte2084")
private val HLG_TRANSFERS = setOf("hlg", "aribstdb67")

internal fun classifyVideoHdrType(
  transfer: String?,
  primaries: String?,
  signalPeak: Double?,
): VideoHdrType {
  val normalizedTransfer = transfer?.trim()?.lowercase()?.replace(".", "")?.replace("-", "")
  val normalizedPrimaries = primaries?.trim()?.lowercase()?.replace(".", "")?.replace("-", "")
  return when {
    normalizedTransfer in PQ_TRANSFERS -> VideoHdrType.PQ
    normalizedTransfer in HLG_TRANSFERS -> VideoHdrType.HLG
    signalPeak != null && signalPeak > 1.01 -> VideoHdrType.BT2020
    !normalizedTransfer.isNullOrEmpty() || !normalizedPrimaries.isNullOrEmpty() || signalPeak != null -> VideoHdrType.SDR
    else -> VideoHdrType.UNKNOWN
  }
}

internal fun classifyVideoDynamicRange(
  transfer: String?,
  signalPeak: Double?,
): VideoDynamicRange = classifyVideoHdrType(transfer, null, signalPeak).dynamicRange

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

internal fun hardwarePlusHdrSettings(sourceType: VideoHdrType): List<Pair<String, String>> {
  val profile = sourceType.hdrToysProfile
  return if (profile == null) {
    listOf(
      "target-colorspace-hint" to "yes",
      "target-colorspace-hint-mode" to "target",
      "target-prim" to "auto",
      "target-trc" to "auto",
      "target-peak" to "auto",
      "inverse-tone-mapping" to "no",
      "tone-mapping" to "auto",
      "gamut-mapping-mode" to "auto",
      "hdr-compute-peak" to "auto",
      "hdr-reference-white" to "203",
      "tone-mapping-visualize" to "no",
      "glsl-shader-opts" to "",
    )
  } else {
    listOf(
      "target-colorspace-hint" to "yes",
      "target-colorspace-hint-mode" to "target",
      "target-prim" to profile.targetPrim,
      "target-trc" to profile.targetTrc,
      "target-peak" to "auto",
      "inverse-tone-mapping" to "no",
      "tone-mapping" to "clip",
      "gamut-mapping-mode" to "clip",
      "hdr-compute-peak" to "no",
      "hdr-reference-white" to "203",
      "tone-mapping-visualize" to "no",
      "glsl-shader-opts" to profile.shaderOptionsValue,
    )
  }
}

fun applyHardwarePlusHdrOutputOptions(sourceType: VideoHdrType, runtime: Boolean) {
  hardwarePlusHdrSettings(sourceType).forEach { (property, value) ->
    if (runtime) MPVLib.setPropertyString(property, value) else MPVLib.setOptionString(property, value)
  }
}
