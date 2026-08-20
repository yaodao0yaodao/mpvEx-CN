package app.marlboroadvance.mpvex.ui.player

import android.content.Context
import android.os.Environment
import android.util.AttributeSet
import android.util.Log

import android.view.KeyCharacterMap
import android.view.KeyEvent
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import app.marlboroadvance.mpvex.preferences.AudioPreferences
import app.marlboroadvance.mpvex.preferences.DecoderPreferences
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.SubtitlesPreferences
import app.marlboroadvance.mpvex.domain.anime4k.Anime4KManager
import app.marlboroadvance.mpvex.domain.hdr.HdrToysManager
import app.marlboroadvance.mpvex.ui.player.PlayerActivity.Companion.TAG
import app.marlboroadvance.mpvex.ui.player.controls.components.panels.toColorHexString
import `is`.xyz.mpv.BaseMPVView
import `is`.xyz.mpv.KeyMapping
import `is`.xyz.mpv.MPVLib
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.reflect.KProperty

class MPVView(
  context: Context,
  attributes: AttributeSet,
) : BaseMPVView(context, attributes),
  KoinComponent {
  private val audioPreferences: AudioPreferences by inject()
  private val playerPreferences: PlayerPreferences by inject()
  private val decoderPreferences: DecoderPreferences by inject()
  private val advancedPreferences: AdvancedPreferences by inject()
  private val subtitlesPreferences: SubtitlesPreferences by inject()
  private val anime4kManager: Anime4KManager by inject()
  private val hdrToysManager: HdrToysManager by inject()
  private val anime4kSafetyController = Anime4KSafetyController()
  private val framePressureTracker = FramePressureTracker()
  private var appliedShaderChain: String? = null
  private var hardwarePlusHdrShaderPaths: List<String> = emptyList()

  var isExiting = false
  var decoderOverride: Decoder? = null
  var activeDecoder: Decoder = Decoder.HWPlus
    private set

  fun getVideoOutAspect(): Double? {
    // Try to get aspect from video-params/aspect first
    val rawAspect = MPVLib.getPropertyDouble("video-params/aspect")
    val rotate = MPVLib.getPropertyInt("video-params/rotate") ?: 0

    // If aspect is not available or 0, calculate from width and height
    val finalAspect = if (rawAspect == null || rawAspect < 0.001) {
      val width = runCatching {
        MPVLib.getPropertyInt("width") ?: MPVLib.getPropertyInt("video-params/w") ?: 0
      }.getOrDefault(0)

      val height = runCatching {
        MPVLib.getPropertyInt("height") ?: MPVLib.getPropertyInt("video-params/h") ?: 0
      }.getOrDefault(0)

      if (width > 0 && height > 0) {
        width.toDouble() / height.toDouble()
      } else {
        null
      }
    } else {
      rawAspect
    }

    return finalAspect?.let { aspect ->
      if (aspect <= 0.001) {
        return null
      }
      val isRotated = (rotate % 180 == 90)
      val correctedAspect = if (isRotated) 1.0 / aspect else aspect
      correctedAspect
    }
  }

  class TrackDelegate(
    private val name: String,
  ) {
    operator fun getValue(
      thisRef: Any?,
      property: KProperty<*>,
    ): Int {
      val v = MPVLib.getPropertyString(name)
      // we can get null here for "no" or other invalid value
      return v?.toIntOrNull() ?: -1
    }

    operator fun setValue(
      thisRef: Any?,
      property: KProperty<*>,
      value: Int,
    ) {
      if (value == -1) MPVLib.setPropertyString(name, "no") else MPVLib.setPropertyInt(name, value)
    }
  }

  var sid: Int by TrackDelegate("sid")
  var secondarySid: Int by TrackDelegate("secondary-sid")
  var aid: Int by TrackDelegate("aid")

  override fun initOptions() {
    isExiting = false
    appliedShaderChain = null
    hardwarePlusHdrShaderPaths = emptyList()
    val profile = decoderPreferences.profile.get()
    MPVLib.setOptionString("profile", profile)
    val priority = decoderPreferences.decoderPriority.get()
    activeDecoder = decoderOverride ?: priority.firstOrNull() ?: Decoder.HWPlus
    val hardwarePlusMode = activeDecoder == Decoder.HWPlus
    val linearHdrSupported = VulkanCapabilities.isDeviceSupported(context)
    val anime4kActive =
      !hardwarePlusMode && decoderPreferences.enableAnime4K.get() && decoderPreferences.anime4kMode.get() != "OFF"
    val backend =
      selectRendererBackend(
        gpuNextEnabled = decoderPreferences.gpuNext.get(),
        vulkanEnabled = decoderPreferences.useVulkan.get(),
        vulkanSupported = linearHdrSupported,
        anime4kActive = anime4kActive,
        hdrActive = linearHdrSupported,
        hardwarePlusMode = hardwarePlusMode,
      )
    setVo(backend.videoOutput)
    MPVLib.setOptionString("gpu-api", backend.gpuApi)
    MPVLib.setOptionString("gpu-context", backend.gpuContext)

    if (hardwarePlusMode) {
      applyHardwarePlusHdrOutputOptions(VideoHdrType.UNKNOWN, runtime = false)
    } else {
      val hdrPipelineReady = backend.videoOutput == "gpu-next" && backend.gpuApi == "vulkan"
      applyLinearHdrOutputOptions(
        pipelineReady = hdrPipelineReady,
        // The source is still unknown here. Enabling inverse tone mapping before
        // detecting SDR can make the first HDR frames appear washed out.
        boostSdrToHdr = false,
      )
    }

    // HW+ is a distinct zero-copy/OpenGL mode. The regular renderer must not try
    // mediacodec first, otherwise Vulkan silently turns the selection into a copy path.
    MPVLib.setOptionString(
      "hwdec",
      initialHwdecValue(
        selectedDecoder = decoderOverride,
        decoderPriority = priority,
      ),
    )
    MPVLib.setOptionString("hwdec-codecs", "all")

    if (decoderPreferences.useYUV420P.get()) {
      MPVLib.setOptionString("vf", "format=yuv420p")
    }
    
    // Cap demuxer cache for mobile to prevent memory issues
    val cacheMegs = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) 64 else 32
    MPVLib.setOptionString("demuxer-max-bytes", "${cacheMegs * 1024 * 1024}")
    MPVLib.setOptionString("demuxer-max-back-bytes", "${cacheMegs * 1024 * 1024}")
    
    val logLevel = if (advancedPreferences.verboseLogging.get()) "v" else "warn"
    MPVLib.setOptionString("msg-level", "all=$logLevel")

    MPVLib.setPropertyBoolean("keep-open", true)
    MPVLib.setPropertyBoolean("input-default-bindings", true)

    MPVLib.setOptionString("tls-verify", "yes")
    MPVLib.setOptionString("tls-ca-file", "${context.filesDir.path}/cacert.pem")

    val screenshotDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    screenshotDir.mkdirs()
    MPVLib.setOptionString("screenshot-directory", screenshotDir.path)

    VideoFilters.entries.forEach {
      MPVLib.setOptionString(it.mpvProperty, it.preference(decoderPreferences).get().toString())
    }

    MPVLib.setOptionString("speed", playerPreferences.defaultSpeed.get().toString())
    MPVLib.setOptionString("vd-lavc-film-grain", "cpu")

    // Apply the saved aspect ratio before mpv loads the first file. PlayerActivity
    // reapplies it after FILE_LOADED as well, because some containers reset video
    // properties while their tracks are initialized.
    applySavedAspectRatioOptions()

    val preciseSeek = playerPreferences.usePreciseSeeking.get()
    MPVLib.setOptionString("hr-seek", if (preciseSeek) "yes" else "no")
    MPVLib.setOptionString("hr-seek-framedrop", if (preciseSeek) "no" else "yes")

    applyShaderStack(backend, runtime = false)

    setupSubtitlesOptions()
    setupAudioOptions()
  }

  private fun applySavedAspectRatioOptions() {
    val customRatio = playerPreferences.defaultCustomAspectRatio.get()
    if (customRatio > 0.0) {
      MPVLib.setOptionString("panscan", "0")
      MPVLib.setOptionString("video-aspect-override", customRatio.toString())
      return
    }

    when (playerPreferences.defaultVideoAspect.get()) {
      VideoAspect.Fit -> {
        MPVLib.setOptionString("panscan", "0")
        MPVLib.setOptionString("video-aspect-override", "-1")
      }
      VideoAspect.Crop -> {
        MPVLib.setOptionString("video-aspect-override", "-1")
        MPVLib.setOptionString("panscan", "1")
      }
      VideoAspect.Stretch -> {
        val metrics = resources.displayMetrics
        val screenRatio = metrics.widthPixels.toDouble() / metrics.heightPixels.toDouble()
        MPVLib.setOptionString("panscan", "0")
        MPVLib.setOptionString("video-aspect-override", screenRatio.toString())
      }
    }
  }

  override fun observeProperties() {
    for ((name, format) in observedProps) MPVLib.observeProperty(name, format)
  }

  override fun postInitOptions() {
    when (decoderPreferences.debanding.get()) {
      Debanding.None -> {}
      Debanding.CPU -> MPVLib.command("vf", "add", "@deband:gradfun=radius=12")
      Debanding.GPU -> MPVLib.setOptionString("deband", "yes")
    }

    advancedPreferences.enabledStatisticsPage.get().let {
      if (it != 0) {
        MPVLib.command("script-binding", "stats/display-stats-toggle")
        MPVLib.command("script-binding", "stats/display-page-$it")
      }
    }
  }

  @Suppress("ReturnCount", "DEPRECATION")
  fun onKey(event: KeyEvent): Boolean {
    if (event.action == KeyEvent.ACTION_MULTIPLE || KeyEvent.isModifierKey(event.keyCode)) {
      return false
    }

    var mapped = KeyMapping[event.keyCode]
    if (mapped == null) {
      // Fallback to produced glyph
      if (!event.isPrintingKey) {
        return false
      }

      val ch = event.unicodeChar
      if (ch.and(KeyCharacterMap.COMBINING_ACCENT) != 0) {
        return false // dead key
      }
      mapped = ch.toChar().toString()
    }

    if (event.repeatCount > 0) {
      return true
    }

    val mod: MutableList<String> = mutableListOf()
    event.isShiftPressed && mod.add("shift")
    event.isCtrlPressed && mod.add("ctrl")
    event.isAltPressed && mod.add("alt")
    event.isMetaPressed && mod.add("meta")

    val action = if (event.action == KeyEvent.ACTION_DOWN) "keydown" else "keyup"
    mod.add(mapped)
    MPVLib.command(action, mod.joinToString("+"))

    return true
  }

  private val observedProps =
    mapOf(
      "pause" to MPVLib.MpvFormat.MPV_FORMAT_FLAG,
      "paused-for-cache" to MPVLib.MpvFormat.MPV_FORMAT_FLAG,
      "video-params/aspect" to MPVLib.MpvFormat.MPV_FORMAT_DOUBLE,
      "video-params/w" to MPVLib.MpvFormat.MPV_FORMAT_INT64,
      "video-params/h" to MPVLib.MpvFormat.MPV_FORMAT_INT64,
      "video-params/gamma" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "video-params/primaries" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "video-params/sig-peak" to MPVLib.MpvFormat.MPV_FORMAT_DOUBLE,
      "eof-reached" to MPVLib.MpvFormat.MPV_FORMAT_FLAG,
      "user-data/mpvex/show_text" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/toggle_ui" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/show_panel" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/set_button_title" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/reset_button_title" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/toggle_button" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/seek_by" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/seek_to" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/seek_by_with_text" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/seek_to_with_text" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/software_keyboard" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
    )

  private fun setupAudioOptions() {
    // Disable MPV's automatic audio selection
    // App will handle track selection manually via TrackSelector to respect user choices
    MPVLib.setOptionString("alang", "")
    MPVLib.setOptionString("audio-delay", (audioPreferences.defaultAudioDelay.get() / 1000.0).toString())
    MPVLib.setOptionString("audio-pitch-correction", audioPreferences.audioPitchCorrection.get().toString())
    MPVLib.setOptionString("volume-max", (audioPreferences.volumeBoostCap.get() + 100).toString())
    
    // Volume normalization using dynamic audio normalization filter
    if (audioPreferences.volumeNormalization.get()) {
      MPVLib.setOptionString("af", "dynaudnorm")
    }
  }

  // Setup
  private fun setupSubtitlesOptions() {
    // Disable MPV's automatic subtitle selection
    // App will handle track selection manually via TrackSelector to respect user choices
    MPVLib.setOptionString("slang", "")
    MPVLib.setOptionString("sub-auto", "no")
    MPVLib.setOptionString("sub-file-paths", "")
    MPVLib.setOptionString("subs-fallback", "no")

    val fontsDirPath = "${context.filesDir.path}/fonts/"
    MPVLib.setOptionString("sub-fonts-dir", fontsDirPath)
    
    // Delay and speed for both primary and secondary
    val subDelay = (subtitlesPreferences.defaultSubDelay.get() / 1000.0).toString()
    val subSpeed = subtitlesPreferences.defaultSubSpeed.get().toString()
    MPVLib.setOptionString("sub-delay", subDelay)
    MPVLib.setOptionString("sub-speed", subSpeed)
    MPVLib.setOptionString("secondary-sub-delay", subDelay)
    MPVLib.setOptionString("secondary-sub-speed", subSpeed)

    val preferredFont = subtitlesPreferences.font.get()
    if (preferredFont.isNotBlank()) {
      MPVLib.setOptionString("sub-font", preferredFont)
      MPVLib.setOptionString("secondary-sub-font", preferredFont)
    }
    // If blank, MPV uses its default font

    if (subtitlesPreferences.overrideAssSubs.get()) {
      MPVLib.setOptionString("sub-ass-override", "force")
      MPVLib.setOptionString("sub-ass-justify", "yes")
      MPVLib.setOptionString("secondary-sub-ass-override", "force")
    } else {
      MPVLib.setOptionString("sub-ass-override", "no")
      MPVLib.setOptionString("secondary-sub-ass-override", "no")
    }

    // Typography and styling for both primary and secondary
    val fontSize = subtitlesPreferences.fontSize.get().toString()
    val bold = if (subtitlesPreferences.bold.get()) "yes" else "no"
    val italic = if (subtitlesPreferences.italic.get()) "yes" else "no"
    val justify = subtitlesPreferences.justification.get().value
    val textColor = subtitlesPreferences.textColor.get().toColorHexString()
    val backgroundColor = subtitlesPreferences.backgroundColor.get().toColorHexString()
    val borderColor = subtitlesPreferences.borderColor.get().toColorHexString()
    val borderSize = subtitlesPreferences.borderSize.get().toString()
    val borderStyle = subtitlesPreferences.borderStyle.get().value
    val shadowOffset = subtitlesPreferences.shadowOffset.get().toString()
    val subPos = subtitlesPreferences.subPos.get().toString()
    val subScale = subtitlesPreferences.subScale.get().toString()

    MPVLib.setOptionString("sub-font-size", fontSize)
    MPVLib.setOptionString("sub-bold", bold)
    MPVLib.setOptionString("sub-italic", italic)
    MPVLib.setOptionString("sub-justify", justify)
    MPVLib.setOptionString("sub-color", textColor)
    MPVLib.setOptionString("sub-back-color", backgroundColor)
    MPVLib.setOptionString("sub-border-color", borderColor)
    MPVLib.setOptionString("sub-border-size", borderSize)
    MPVLib.setOptionString("sub-border-style", borderStyle)
    MPVLib.setOptionString("sub-shadow-offset", shadowOffset)
    MPVLib.setOptionString("sub-scale", subScale)
    MPVLib.setOptionString("sub-pos", subPos)
    
    MPVLib.setOptionString("secondary-sub-font-size", fontSize)
    MPVLib.setOptionString("secondary-sub-bold", bold)
    MPVLib.setOptionString("secondary-sub-italic", italic)
    MPVLib.setOptionString("secondary-sub-justify", justify)
    MPVLib.setOptionString("secondary-sub-color", textColor)
    MPVLib.setOptionString("secondary-sub-back-color", backgroundColor)
    MPVLib.setOptionString("secondary-sub-border-color", borderColor)
    MPVLib.setOptionString("secondary-sub-border-size", borderSize)
    MPVLib.setOptionString("secondary-sub-border-style", borderStyle)
    MPVLib.setOptionString("secondary-sub-shadow-offset", shadowOffset)
    MPVLib.setOptionString("secondary-sub-scale", subScale)
    // Position secondary subtitle at top (10) instead of bottom to avoid overlap with primary
    MPVLib.setOptionString("secondary-sub-pos", "10")

    val scaleByWindow = if (subtitlesPreferences.scaleByWindow.get()) "yes" else "no"
    MPVLib.setOptionString("sub-scale-by-window", scaleByWindow)
    MPVLib.setOptionString("sub-use-margins", scaleByWindow)
    MPVLib.setOptionString("secondary-sub-scale-by-window", scaleByWindow)
    MPVLib.setOptionString("secondary-sub-use-margins", scaleByWindow)
  }


  fun applyAnime4KShaders() {
    val hardwarePlusMode = activeDecoder == Decoder.HWPlus
    val linearHdrSupported = VulkanCapabilities.isDeviceSupported(context)
    val backend =
      selectRendererBackend(
        gpuNextEnabled = decoderPreferences.gpuNext.get(),
        vulkanEnabled = decoderPreferences.useVulkan.get(),
        vulkanSupported = linearHdrSupported,
        anime4kActive =
          !hardwarePlusMode && decoderPreferences.enableAnime4K.get() && decoderPreferences.anime4kMode.get() != "OFF",
        hdrActive = linearHdrSupported,
        hardwarePlusMode = hardwarePlusMode,
      )
    applyShaderStack(backend, runtime = true)
  }

  fun resetAnime4KSafety() {
    anime4kSafetyController.reset()
    framePressureTracker.reset()
  }

  fun isAnime4KConfigured(): Boolean =
    activeDecoder != Decoder.HWPlus &&
      decoderPreferences.enableAnime4K.get() &&
      decoderPreferences.anime4kMode.get() != Anime4KManager.Mode.OFF.name

  fun refreshAnime4KSafety(thermalPressure: ThermalPressure) {
    if (!isAnime4KConfigured()) return
    val highResolution = isCurrentVideoHighResolution()
    val previousLevel = anime4kSafetyController.level
    val preferredSelection = preferredAnime4KSelection()
    val previousSelection =
      effectiveAnime4KSelection(
        preferredSelection,
        if (highResolution) Anime4KGuardLevel.DISABLED else previousLevel,
      )
    val framePressure = framePressureTracker.sample(readFrameCounters())
    val currentLevel =
      anime4kSafetyController.update(
        highResolution = highResolution,
        thermalPressure = thermalPressure,
        framePressure = framePressure,
      )
    if (currentLevel != previousLevel) {
      Log.i(TAG, "Anime4K safety level changed: $previousLevel -> $currentLevel")
    }
    val currentSelection =
      effectiveAnime4KSelection(
        preferredSelection,
        if (highResolution) Anime4KGuardLevel.DISABLED else currentLevel,
      )
    if (currentSelection != previousSelection) {
      applyAnime4KShaders()
    }
  }

  private fun applyShaderStack(
    backend: RendererBackend,
    runtime: Boolean,
  ) {
    runCatching {
      val animeShaderPaths = anime4kShaderPaths(backend)
      val shaderChain = (animeShaderPaths + hardwarePlusHdrShaderPaths).joinToString(":")
      if (runtime && shaderChain == appliedShaderChain) return@runCatching
      if (runtime) {
        MPVLib.setPropertyString("glsl-shaders", shaderChain)
      } else if (shaderChain.isNotEmpty()) {
        MPVLib.setOptionString("glsl-shaders", shaderChain)
      }
      appliedShaderChain = shaderChain
    }
  }

  fun currentVideoDynamicRange(): VideoDynamicRange =
    currentVideoHdrType().dynamicRange

  fun currentVideoHdrType(): VideoHdrType =
    classifyVideoHdrType(
      transfer = MPVLib.getPropertyString("video-params/gamma"),
      primaries = MPVLib.getPropertyString("video-params/primaries"),
      signalPeak = MPVLib.getPropertyDouble("video-params/sig-peak"),
    )

  fun applyHardwarePlusHdrForSource(sourceType: VideoHdrType) {
    if (activeDecoder != Decoder.HWPlus) return
    val profile = sourceType.hdrToysProfile
    hardwarePlusHdrShaderPaths = profile?.let(hdrToysManager::getShaderPaths).orEmpty()
    applyHardwarePlusHdrOutputOptions(sourceType, runtime = true)
    applyShaderStack(
      backend = RendererBackend(if (decoderPreferences.gpuNext.get()) "gpu-next" else "gpu", "opengl", "android"),
      runtime = true,
    )
  }

  private fun anime4kShaderPaths(backend: RendererBackend): List<String> {
    if (activeDecoder == Decoder.HWPlus) return emptyList()
    if (!decoderPreferences.enableAnime4K.get()) return emptyList()
    if (backend.videoOutput == "gpu-next" && backend.gpuApi != "vulkan") return emptyList()
    if (!anime4kManager.initialize()) return emptyList()

    val guardLevel =
      if (isCurrentVideoHighResolution()) Anime4KGuardLevel.DISABLED else anime4kSafetyController.level
    val selection = effectiveAnime4KSelection(preferredAnime4KSelection(), guardLevel)
    if (selection.mode == Anime4KManager.Mode.OFF) return emptyList()
    val shaderChain = anime4kManager.getShaderChain(selection.mode, selection.quality)
    if (shaderChain.isEmpty()) return emptyList()

    if (backend.gpuApi == "opengl") {
      MPVLib.setOptionString("opengl-pbo", "yes")
      MPVLib.setOptionString("opengl-early-flush", "no")
    }
    MPVLib.setOptionString("vd-lavc-dr", "yes")
    return shaderChain.split(":")
  }

  private fun preferredAnime4KSelection(): Anime4KSelection =
    Anime4KSelection(
      mode =
        runCatching { Anime4KManager.Mode.valueOf(decoderPreferences.anime4kMode.get()) }
          .getOrDefault(Anime4KManager.Mode.OFF),
      quality =
        runCatching { Anime4KManager.Quality.valueOf(decoderPreferences.anime4kQuality.get()) }
          .getOrDefault(Anime4KManager.Quality.BALANCED),
    )

  private fun isCurrentVideoHighResolution(): Boolean =
    isAnime4KHighResolution(
      width = MPVLib.getPropertyInt("video-params/w") ?: 0,
      height = MPVLib.getPropertyInt("video-params/h") ?: 0,
    )

  private fun readFrameCounters(): FrameCounters =
    FrameCounters(
      dropped =
        (MPVLib.getPropertyInt("frame-drop-count") ?: 0) +
          (MPVLib.getPropertyInt("decoder-frame-drop-count") ?: 0),
      delayed = MPVLib.getPropertyInt("vo-delayed-frame-count") ?: 0,
      mistimed = MPVLib.getPropertyInt("mistimed-frame-count") ?: 0,
    )
}
