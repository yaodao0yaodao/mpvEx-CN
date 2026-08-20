package app.marlboroadvance.mpvex.ui.player

import app.marlboroadvance.mpvex.domain.anime4k.Anime4KManager

enum class ThermalPressure {
  NORMAL,
  ELEVATED,
  SEVERE,
}

enum class Anime4KGuardLevel {
  FULL,
  REDUCED,
  DISABLED,
}

data class Anime4KSelection(
  val mode: Anime4KManager.Mode,
  val quality: Anime4KManager.Quality,
)

class Anime4KSafetyController(
  private val recoverySamplesRequired: Int = 3,
  private val framePressureSamplesRequired: Int = 2,
) {
  var level: Anime4KGuardLevel = Anime4KGuardLevel.FULL
    private set

  private var normalSamples = 0
  private var framePressureSamples = 0

  fun reset() {
    level = Anime4KGuardLevel.FULL
    normalSamples = 0
    framePressureSamples = 0
  }

  fun update(
    highResolution: Boolean,
    thermalPressure: ThermalPressure,
    framePressure: Boolean,
  ): Anime4KGuardLevel {
    if (highResolution || thermalPressure == ThermalPressure.SEVERE) {
      level = Anime4KGuardLevel.DISABLED
      normalSamples = 0
      framePressureSamples = 0
      return level
    }

    framePressureSamples = if (framePressure) framePressureSamples + 1 else 0
    if (framePressureSamples >= framePressureSamplesRequired) {
      level = Anime4KGuardLevel.DISABLED
      normalSamples = 0
      return level
    }

    if (thermalPressure == ThermalPressure.ELEVATED || framePressure) {
      if (level == Anime4KGuardLevel.FULL) level = Anime4KGuardLevel.REDUCED
      normalSamples = 0
      return level
    }

    normalSamples++
    if (normalSamples >= recoverySamplesRequired) {
      level =
        when (level) {
          Anime4KGuardLevel.DISABLED -> Anime4KGuardLevel.REDUCED
          Anime4KGuardLevel.REDUCED -> Anime4KGuardLevel.FULL
          Anime4KGuardLevel.FULL -> Anime4KGuardLevel.FULL
        }
      normalSamples = 0
    }
    return level
  }
}

fun effectiveAnime4KSelection(
  preferred: Anime4KSelection,
  guardLevel: Anime4KGuardLevel,
): Anime4KSelection {
  if (preferred.mode == Anime4KManager.Mode.OFF || guardLevel == Anime4KGuardLevel.DISABLED) {
    return preferred.copy(mode = Anime4KManager.Mode.OFF)
  }

  if (preferred.mode == Anime4KManager.Mode.ARTCNN) {
    return preferred
  }

  if (guardLevel == Anime4KGuardLevel.FULL) return preferred

  val reducedMode =
    when (preferred.mode) {
      Anime4KManager.Mode.A_PLUS -> Anime4KManager.Mode.A
      Anime4KManager.Mode.B_PLUS -> Anime4KManager.Mode.B
      Anime4KManager.Mode.C_PLUS -> Anime4KManager.Mode.C
      else -> preferred.mode
    }
  return Anime4KSelection(reducedMode, Anime4KManager.Quality.FAST)
}

fun isAnime4KHighResolution(width: Int, height: Int): Boolean = width >= 3840 || height >= 2160

data class FrameCounters(
  val dropped: Int,
  val delayed: Int,
  val mistimed: Int,
  val averageDelayMs: Double,
)

class FramePressureTracker {
  private var previous: FrameCounters? = null

  fun reset() {
    previous = null
  }

  fun sample(current: FrameCounters): Boolean {
    val old = previous.also { previous = current } ?: return false
    val droppedDelta = (current.dropped - old.dropped).coerceAtLeast(0)
    val delayedDelta = (current.delayed - old.delayed).coerceAtLeast(0)
    val mistimedDelta = (current.mistimed - old.mistimed).coerceAtLeast(0)
    return droppedDelta >= 3 || delayedDelta >= 8 || mistimedDelta >= 12 || current.averageDelayMs >= 12.0
  }
}
