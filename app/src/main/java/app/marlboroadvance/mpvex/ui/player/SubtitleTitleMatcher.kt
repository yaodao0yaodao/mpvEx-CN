package app.marlboroadvance.mpvex.ui.player

/** Selects a subtitle title by applying comma-separated preferences from left to right. */
internal object SubtitleTitleMatcher {
  fun findBestMatchIndex(titles: List<String>, orderedKeywords: List<String>): Int? {
    var candidates = titles.indices.toList()
    var matchedAnyKeyword = false

    for (keyword in orderedKeywords.map { it.trim() }.filter { it.isNotEmpty() }) {
      val matches = candidates.filter { index -> matchesKeyword(titles[index], keyword) }
      if (matches.isNotEmpty()) {
        candidates = matches
        matchedAnyKeyword = true
      }
    }

    return candidates.firstOrNull().takeIf { matchedAnyKeyword }
  }

  private fun matchesKeyword(title: String, keyword: String): Boolean {
    val isShortAsciiCode = keyword.length <= 3 && keyword.all { it.isAsciiLetterOrDigit() }
    if (!isShortAsciiCode) return title.contains(keyword, ignoreCase = true)

    // Treat short values such as chs, CN, ch and zh as codes. Boundaries prevent
    // "ch" from accidentally matching unrelated words such as "French".
    val codePattern = Regex(
      pattern = "(?i)(?<![a-z0-9])${Regex.escape(keyword)}(?![a-z0-9])",
    )
    return codePattern.containsMatchIn(title)
  }

  private fun Char.isAsciiLetterOrDigit(): Boolean =
    this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9'
}
