package app.marlboroadvance.mpvex.ui.player

import androidx.annotation.StringRes
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.domain.hdr.HdrToysProfile
import `is`.xyz.mpv.MPVLib

enum class HdrScreenMode(
  @StringRes val titleRes: Int,
  @StringRes val descriptionRes: Int,
  val hdrToysProfile: HdrToysProfile? = null,
) {
  OFF(R.string.hdr_mode_off, R.string.hdr_mode_off_description),
  BT_2100_PQ(
    R.string.hdr_mode_bt2100_pq,
    R.string.hdr_mode_bt2100_pq_description,
    HdrToysProfile.BT_2100_PQ,
  ),
  BT_2100_HLG(
    R.string.hdr_mode_bt2100_hlg,
    R.string.hdr_mode_bt2100_hlg_description,
    HdrToysProfile.BT_2100_HLG,
  ),
  BT_2020(
    R.string.hdr_mode_bt2020,
    R.string.hdr_mode_bt2020_description,
    HdrToysProfile.BT_2020,
  ),
  LINEAR(R.string.hdr_mode_linear, R.string.hdr_mode_linear_description),
  ;

  companion object {
    val selectableModes = listOf(BT_2100_PQ, BT_2100_HLG, BT_2020, LINEAR)
    val defaultEnabledMode = BT_2020
  }
}

private val HDR_OWNED_PROPERTIES =
  setOf(
    "target-colorspace-hint",
    "target-colorspace-hint-mode",
    "target-prim",
    "target-trc",
    "target-peak",
    "inverse-tone-mapping",
    "tone-mapping",
    "gamut-mapping-mode",
    "hdr-compute-peak",
    "hdr-reference-white",
    "tone-mapping-visualize",
    "glsl-shader-opts",
  )

internal fun hdrScreenOutputSettings(
  mode: HdrScreenMode,
  pipelineReady: Boolean,
  boostSdrToHdr: Boolean = false,
): List<Pair<String, String>> {
  val activeMode = if (pipelineReady) mode else HdrScreenMode.OFF
  val settings =
    when (activeMode) {
      HdrScreenMode.OFF -> offSettings()
      HdrScreenMode.LINEAR -> linearHdrSettings(boostSdrToHdr)
      else -> hdrToysSettings(requireNotNull(activeMode.hdrToysProfile))
    }

  check(settings.map { it.first }.toSet() == HDR_OWNED_PROPERTIES) {
    "Incomplete HDR output settings for $activeMode"
  }
  return settings
}

private fun commonSettings(
  targetColorspaceHint: String,
  targetPrim: String,
  targetTrc: String,
  targetPeak: String,
  inverseToneMapping: String,
  toneMapping: String,
  gamutMappingMode: String,
  hdrComputePeak: String,
  shaderOptions: String,
): List<Pair<String, String>> =
  listOf(
    "target-colorspace-hint" to targetColorspaceHint,
    "target-colorspace-hint-mode" to "target",
    "target-prim" to targetPrim,
    "target-trc" to targetTrc,
    "target-peak" to targetPeak,
    "inverse-tone-mapping" to inverseToneMapping,
    "tone-mapping" to toneMapping,
    "gamut-mapping-mode" to gamutMappingMode,
    "hdr-compute-peak" to hdrComputePeak,
    "hdr-reference-white" to "203",
    "tone-mapping-visualize" to "no",
    "glsl-shader-opts" to shaderOptions,
  )

private fun offSettings() =
  commonSettings("auto", "auto", "auto", "auto", "auto", "auto", "auto", "auto", "")

private fun hdrToysSettings(profile: HdrToysProfile) =
  commonSettings(
    targetColorspaceHint = "no",
    targetPrim = profile.targetPrim,
    targetTrc = profile.targetTrc,
    targetPeak = "auto",
    inverseToneMapping = "no",
    toneMapping = "clip",
    gamutMappingMode = "clip",
    hdrComputePeak = "no",
    shaderOptions = profile.shaderOptionsValue,
  )

private fun linearHdrSettings(boostSdrToHdr: Boolean) =
  commonSettings(
    targetColorspaceHint = "yes",
    targetPrim = "auto",
    targetTrc = "auto",
    targetPeak = "auto",
    inverseToneMapping = if (boostSdrToHdr) "yes" else "no",
    toneMapping = "clip",
    gamutMappingMode = "clip",
    hdrComputePeak = "yes",
    shaderOptions = "",
  )

fun applyHdrScreenOutputOptions(
  mode: HdrScreenMode,
  pipelineReady: Boolean,
  boostSdrToHdr: Boolean = false,
) {
  hdrScreenOutputSettings(mode, pipelineReady, boostSdrToHdr).forEach { (property, value) ->
    MPVLib.setOptionString(property, value)
  }
}
