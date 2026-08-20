package app.marlboroadvance.mpvex.ui.player

import android.os.PowerManager
import app.marlboroadvance.mpvex.domain.anime4k.Anime4KManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Anime4KSafetyPolicyTest {
  @Test
  fun `ArtCNN is never replaced by a regular Anime4K mode`() {
    val preferred = Anime4KSelection(Anime4KManager.Mode.ARTCNN, Anime4KManager.Quality.BALANCED)

    assertEquals(preferred, effectiveAnime4KSelection(preferred, Anime4KGuardLevel.FULL))
    assertEquals(preferred, effectiveAnime4KSelection(preferred, Anime4KGuardLevel.REDUCED))
    assertEquals(
      Anime4KManager.Mode.OFF,
      effectiveAnime4KSelection(preferred, Anime4KGuardLevel.DISABLED).mode,
    )
  }

  @Test
  fun `ArtCNN turns off under sustained pressure and restores as ArtCNN`() {
    val controller = Anime4KSafetyController(recoverySamplesRequired = 3, framePressureSamplesRequired = 2)
    val preferred = Anime4KSelection(Anime4KManager.Mode.ARTCNN, Anime4KManager.Quality.BALANCED)

    val firstPressure = controller.update(false, ThermalPressure.NORMAL, framePressure = true)
    assertEquals(Anime4KManager.Mode.ARTCNN, effectiveAnime4KSelection(preferred, firstPressure).mode)

    val sustainedPressure = controller.update(false, ThermalPressure.NORMAL, framePressure = true)
    assertEquals(Anime4KManager.Mode.OFF, effectiveAnime4KSelection(preferred, sustainedPressure).mode)

    repeat(3) { controller.update(false, ThermalPressure.NORMAL, framePressure = false) }
    assertEquals(Anime4KManager.Mode.ARTCNN, effectiveAnime4KSelection(preferred, controller.level).mode)
  }

  @Test
  fun `regular enhanced mode reduces to base fast mode`() {
    val preferred = Anime4KSelection(Anime4KManager.Mode.A_PLUS, Anime4KManager.Quality.HIGH)
    val effective = effectiveAnime4KSelection(preferred, Anime4KGuardLevel.REDUCED)

    assertEquals(Anime4KManager.Mode.A, effective.mode)
    assertEquals(Anime4KManager.Quality.FAST, effective.quality)
  }

  @Test
  fun `high resolution immediately disables Anime4K`() {
    val controller = Anime4KSafetyController()

    assertEquals(
      Anime4KGuardLevel.DISABLED,
      controller.update(true, ThermalPressure.NORMAL, framePressure = false),
    )
  }

  @Test
  fun `controller waits for stable recovery`() {
    val controller = Anime4KSafetyController(recoverySamplesRequired = 3)
    controller.update(false, ThermalPressure.SEVERE, false)

    repeat(2) {
      assertEquals(Anime4KGuardLevel.DISABLED, controller.update(false, ThermalPressure.NORMAL, false))
    }
    assertEquals(Anime4KGuardLevel.REDUCED, controller.update(false, ThermalPressure.NORMAL, false))
    repeat(2) { controller.update(false, ThermalPressure.NORMAL, false) }
    assertEquals(Anime4KGuardLevel.FULL, controller.update(false, ThermalPressure.NORMAL, false))
  }

  @Test
  fun `frame pressure uses counter deltas instead of lifetime totals`() {
    val tracker = FramePressureTracker()

    assertFalse(tracker.sample(FrameCounters(100, 100, 100)))
    assertFalse(tracker.sample(FrameCounters(101, 102, 104)))
    assertTrue(tracker.sample(FrameCounters(105, 102, 104)))
  }

  @Test
  fun `resolution guard covers landscape and portrait 4K`() {
    assertTrue(isAnime4KHighResolution(3840, 1600))
    assertTrue(isAnime4KHighResolution(1600, 2160))
    assertFalse(isAnime4KHighResolution(1920, 1080))
  }

  @Test
  fun `Android thermal load increases toward the severe threshold`() {
    assertEquals(
      ThermalPressure.NORMAL,
      classifyThermalPressure(0.2f, PowerManager.THERMAL_STATUS_NONE),
    )
    assertEquals(
      ThermalPressure.ELEVATED,
      classifyThermalPressure(0.8f, PowerManager.THERMAL_STATUS_NONE),
    )
    assertEquals(
      ThermalPressure.SEVERE,
      classifyThermalPressure(1.0f, PowerManager.THERMAL_STATUS_NONE),
    )
    assertEquals(
      ThermalPressure.NORMAL,
      classifyThermalPressure(Float.NaN, PowerManager.THERMAL_STATUS_NONE),
    )
  }
}
