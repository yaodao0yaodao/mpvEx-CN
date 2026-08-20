package app.marlboroadvance.mpvex.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class HdrScreenOutputTest {
  @Test
  fun `native hdr transfers are detected`() {
    assertEquals(VideoDynamicRange.HDR, classifyVideoDynamicRange("pq", 1.0))
    assertEquals(VideoDynamicRange.HDR, classifyVideoDynamicRange("HLG", null))
  }

  @Test
  fun `signal peak detects hdr independently of codec`() {
    assertEquals(VideoDynamicRange.HDR, classifyVideoDynamicRange("bt.1886", 4.0))
    assertEquals(VideoDynamicRange.SDR, classifyVideoDynamicRange("bt.1886", 1.0))
  }

  @Test
  fun `sdr boost only changes inverse tone mapping`() {
    val normal = linearHdrSettings(true, false).toMap()
    val boosted = linearHdrSettings(true, true).toMap()
    assertEquals("no", normal["inverse-tone-mapping"])
    assertEquals("yes", boosted["inverse-tone-mapping"])
    assertEquals(normal - "inverse-tone-mapping", boosted - "inverse-tone-mapping")
  }
}
