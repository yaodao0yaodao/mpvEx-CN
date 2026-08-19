package app.marlboroadvance.mpvex.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubtitleTitleMatcherTest {
  @Test
  fun `selects special effects simplified Chinese using ordered preferences`() {
    val titles = listOf(
      "官方简体中文",
      "特效字幕简体中文",
      "特效字幕繁体中文",
    )

    val result = SubtitleTitleMatcher.findBestMatchIndex(
      titles,
      listOf("特效", "Simplified", "chs", "CN", "简", "ch", "zh", "中"),
    )

    assertEquals(1, result)
  }

  @Test
  fun `earlier keyword cannot be outweighed by several later keywords`() {
    val titles = listOf("特效字幕", "Simplified chs CN 简中")

    val result = SubtitleTitleMatcher.findBestMatchIndex(
      titles,
      listOf("特效", "Simplified", "chs", "CN", "简", "中"),
    )

    assertEquals(0, result)
  }

  @Test
  fun `short language code requires an ascii token boundary`() {
    val titles = listOf("French", "Chinese [CH]")

    val result = SubtitleTitleMatcher.findBestMatchIndex(titles, listOf("ch"))

    assertEquals(1, result)
  }

  @Test
  fun `returns null when no title keyword matches`() {
    val result = SubtitleTitleMatcher.findBestMatchIndex(
      listOf("English", "日本語"),
      listOf("简", "繁"),
    )

    assertNull(result)
  }
}
