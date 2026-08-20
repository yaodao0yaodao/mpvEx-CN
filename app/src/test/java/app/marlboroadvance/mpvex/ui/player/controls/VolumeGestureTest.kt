package app.marlboroadvance.mpvex.ui.player.controls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeGestureTest {
  @Test
  fun `full height swipe covers the complete volume range on fine grained devices`() {
    val sensitivity = calculateVolumeGestureSensitivity(maxValue = 150, viewportHeightPx = 2400)
    val result = calculateNewVerticalGestureValue(
      originalValue = 0,
      startingY = 2400f,
      newY = 0f,
      sensitivity = sensitivity,
    )

    assertTrue(result >= 150)
  }

  @Test
  fun `sensitivity adapts to system volume steps and screen height`() {
    val fineGrained = calculateVolumeGestureSensitivity(maxValue = 150, viewportHeightPx = 2400)
    val conventional = calculateVolumeGestureSensitivity(maxValue = 15, viewportHeightPx = 2400)

    assertEquals(10f, fineGrained / conventional, 0.0001f)
  }

  @Test
  fun `invalid dimensions disable volume gesture changes`() {
    assertEquals(0f, calculateVolumeGestureSensitivity(150, 0), 0f)
    assertEquals(0f, calculateVolumeGestureSensitivity(0, 2400), 0f)
  }
}
