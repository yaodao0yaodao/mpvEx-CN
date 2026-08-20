package app.marlboroadvance.mpvex.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class HdrScreenOutputTest {
  @Test
  fun `every mode owns the complete HDR property set`() {
    HdrScreenMode.entries.forEach { mode ->
      val settings = hdrScreenOutputSettings(mode, pipelineReady = true)

      assertEquals(12, settings.size)
      assertEquals(12, settings.map { it.first }.toSet().size)
    }
  }

  @Test
  fun `unavailable pipeline resets HDR options`() {
    val settings = hdrScreenOutputSettings(HdrScreenMode.BT_2100_PQ, pipelineReady = false).toMap()

    assertEquals("auto", settings.getValue("target-trc"))
    assertEquals("", settings.getValue("glsl-shader-opts"))
  }

  @Test
  fun `linear mode only boosts SDR when requested`() {
    val normal = hdrScreenOutputSettings(HdrScreenMode.LINEAR, true, false).toMap()
    val boosted = hdrScreenOutputSettings(HdrScreenMode.LINEAR, true, true).toMap()

    assertEquals("no", normal.getValue("inverse-tone-mapping"))
    assertEquals("yes", boosted.getValue("inverse-tone-mapping"))
  }
}
