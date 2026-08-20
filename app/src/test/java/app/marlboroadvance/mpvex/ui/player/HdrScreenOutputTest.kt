package app.marlboroadvance.mpvex.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class HdrScreenOutputTest {
  @Test
  fun `native hdr transfers are detected`() {
    assertEquals(VideoDynamicRange.HDR, classifyVideoDynamicRange("pq", 1.0))
    assertEquals(VideoDynamicRange.HDR, classifyVideoDynamicRange("HLG", null))
    assertEquals(VideoHdrType.PQ, classifyVideoHdrType("smpte2084", "bt.2020", 4.0))
    assertEquals(VideoHdrType.HLG, classifyVideoHdrType("arib-std-b67", "bt.2020", 4.0))
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

  @Test
  fun `hardware plus hdr follows the source transfer`() {
    val pq = hardwarePlusHdrSettings(VideoHdrType.PQ).toMap()
    val hlg = hardwarePlusHdrSettings(VideoHdrType.HLG).toMap()
    val bt2020 = hardwarePlusHdrSettings(VideoHdrType.BT2020).toMap()
    val sdr = hardwarePlusHdrSettings(VideoHdrType.SDR).toMap()

    assertEquals("pq", pq["target-trc"])
    assertEquals("hlg", hlg["target-trc"])
    assertEquals("bt.1886", bt2020["target-trc"])
    assertEquals("no", sdr["inverse-tone-mapping"])
    assertEquals("auto", sdr["target-trc"])
  }
}
