package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.ui.player.MPVProfile
import app.marlboroadvance.mpvex.ui.theme.spacing
import `is`.xyz.mpv.MPVLib
import org.koin.compose.koinInject

@Composable
fun MoreSheet(
  currentProfile: MPVProfile,
  onProfileChanged: (MPVProfile) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val advancedPreferences = koinInject<AdvancedPreferences>()
  val statisticsPage by advancedPreferences.enabledStatisticsPage.collectAsState()

  PlayerSheet(onDismissRequest, modifier) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(MaterialTheme.spacing.medium)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
      Text(text = "更多", style = MaterialTheme.typography.headlineMedium)

      Text(
        text = stringResource(R.string.player_sheets_stats_page_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      LazyRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
        items((0..7).toList(), key = { it }) { page ->
          FilterChip(
            selected = statisticsPage == page,
            onClick = {
              val consoleOpen = MPVLib.getPropertyBoolean("user-data/mpv/console/open") == true
              if (page != 7 && consoleOpen) MPVLib.command("script-message-to", "console", "disable")
              when (page) {
                0, 6 -> if (statisticsPage in 1..5) MPVLib.command("script-binding", "stats/display-stats-toggle")
                7 -> {
                  if (statisticsPage in 1..5) MPVLib.command("script-binding", "stats/display-stats-toggle")
                  if (!consoleOpen) MPVLib.command("script-message-to", "console", "enable")
                }
                else -> {
                  if (statisticsPage == 0 || statisticsPage >= 6) {
                    MPVLib.command("script-binding", "stats/display-stats-toggle")
                  }
                  MPVLib.command("script-binding", "stats/display-page-$page")
                }
              }
              advancedPreferences.enabledStatisticsPage.set(page)
            },
            label = {
              Text(
                when (page) {
                  0 -> "关闭"
                  7 -> "控制台"
                  else -> "第 $page 页"
                },
              )
            },
          )
        }
      }

      Text("MPV 配置档（临时生效）", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
        items(MPVProfile.entries, key = { it.value }) { profile ->
          FilterChip(
            selected = currentProfile == profile,
            onClick = { onProfileChanged(profile) },
            label = { Text(stringResource(profile.titleRes)) },
          )
        }
      }

    }
  }
}
