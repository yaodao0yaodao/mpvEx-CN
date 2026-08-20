package app.marlboroadvance.mpvex.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackResumeTest {
  @Test
  fun `saved position is supplied as a file-local start option`() {
    assertEquals(
      listOf("loadfile", "video.mkv", "replace", "-1", "start=42"),
      buildLoadFileCommand("video.mkv", 42),
    )
  }

  @Test
  fun `new playback uses the ordinary loadfile command`() {
    assertEquals(listOf("loadfile", "video.mkv"), buildLoadFileCommand("video.mkv", null))
    assertEquals(listOf("loadfile", "video.mkv"), buildLoadFileCommand("video.mkv", 0))
  }
}
