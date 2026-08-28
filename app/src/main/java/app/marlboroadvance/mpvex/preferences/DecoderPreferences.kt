package app.marlboroadvance.mpvex.preferences

import app.marlboroadvance.mpvex.preferences.preference.PreferenceStore
import app.marlboroadvance.mpvex.preferences.preference.getEnum
import app.marlboroadvance.mpvex.ui.player.Debanding
import app.marlboroadvance.mpvex.ui.player.Decoder

class DecoderPreferences(
  preferenceStore: PreferenceStore,
) {
  val profile = preferenceStore.getString("mpv_profile", "fast")
  val decoderPriority =
    preferenceStore.getObject(
      key = "decoder_priority",
      defaultValue = listOf(Decoder.HWPlus, Decoder.HW, Decoder.SW),
      serializer = { order -> order.joinToString(",") { it.name } },
      deserializer = { stored ->
        val parsed = stored.split(",").mapNotNull { name -> Decoder.entries.firstOrNull { it.name == name.trim() } }
        (parsed + listOf(Decoder.HWPlus, Decoder.HW, Decoder.SW)).distinct().filter { it in Decoder.priorityModes }
      },
    )
  val gpuNext = preferenceStore.getBoolean("gpu_next")
  val useVulkan = preferenceStore.getBoolean("use_vulkan", false)
  val boostSdrToHdr = preferenceStore.getBoolean("boost_sdr_to_hdr", false)
  val useYUV420P = preferenceStore.getBoolean("use_yuv420p", false)

  val debanding = preferenceStore.getEnum("debanding", Debanding.None)
  val debandIterations = preferenceStore.getInt("deband_iterations", 1)
  val debandThreshold = preferenceStore.getInt("deband_threshold", 48)
  val debandRange = preferenceStore.getInt("deband_range", 16)
  val debandGrain = preferenceStore.getInt("deband_grain", 32)

  val brightnessFilter = preferenceStore.getInt("filter_brightness")
  val saturationFilter = preferenceStore.getInt("filter_saturation")
  val gammaFilter = preferenceStore.getInt("filter_gamma")
  val contrastFilter = preferenceStore.getInt("filter_contrast")
  val hueFilter = preferenceStore.getInt("filter_hue")
  val sharpnessFilter = preferenceStore.getInt("filter_sharpness")

  // Anime4K Preferences
  val enableAnime4K = preferenceStore.getBoolean("enable_anime4k", false)
  val anime4kMode = preferenceStore.getString("anime4k_mode", "OFF")
  val anime4kQuality = preferenceStore.getString("anime4k_quality", "FAST")

  fun effectiveDecoderPriority(): List<Decoder> {
    val configured = decoderPriority.get()
    return (configured + Decoder.priorityModes).distinct().filter { it in Decoder.priorityModes }
  }
}
