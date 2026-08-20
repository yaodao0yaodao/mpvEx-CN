package app.marlboroadvance.mpvex.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PositionPollPolicyTest {
  @Test
  fun `polling follows UI demand`() {
    assertEquals(100L, positionPollInterval(paused = false, controlsVisible = true))
    assertEquals(500L, positionPollInterval(paused = false, controlsVisible = false))
    assertEquals(1_000L, positionPollInterval(paused = true, controlsVisible = true))
  }
}
