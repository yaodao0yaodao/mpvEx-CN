package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.marlboroadvance.mpvex.domain.anime4k.Anime4KManager
import app.marlboroadvance.mpvex.preferences.DecoderPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.ui.theme.spacing
import org.koin.compose.koinInject

@Composable
fun AiUpscaleSheet(
  hardwarePlusMode: Boolean,
  onChanged: () -> Unit,
  onDismissRequest: () -> Unit,
) {
  val preferences = koinInject<DecoderPreferences>()
  val storedMode by preferences.anime4kMode.collectAsState()
  val storedQuality by preferences.anime4kQuality.collectAsState()
  val mode = runCatching { Anime4KManager.Mode.valueOf(storedMode) }.getOrDefault(Anime4KManager.Mode.OFF)
  val quality = runCatching { Anime4KManager.Quality.valueOf(storedQuality) }.getOrDefault(Anime4KManager.Quality.BALANCED)

  PlayerSheet(onDismissRequest) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
      Text("AI 超分辨率", style = MaterialTheme.typography.headlineMedium)
      if (hardwarePlusMode) {
        Text(
          "硬件解码增强模式下 Anime4K / ArtCNN 不可用。切换解码器后会恢复当前选择。",
          color = MaterialTheme.colorScheme.error,
        )
      }

      Anime4KManager.Mode.entries.forEach { option ->
        Row(
          modifier = Modifier.fillMaxWidth().clickable(enabled = !hardwarePlusMode) {
            preferences.anime4kMode.set(option.name)
            onChanged()
          }.padding(vertical = MaterialTheme.spacing.extraSmall),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          RadioButton(
            selected = mode == option,
            enabled = !hardwarePlusMode,
            onClick = {
              preferences.anime4kMode.set(option.name)
              onChanged()
            },
          )
          Text(stringResource(option.titleRes))
        }
      }

      if (mode.usesAnime4KQuality) {
        Text("Anime4K 质量档", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Anime4KManager.Quality.entries.forEach { option ->
          Row(
            modifier = Modifier.fillMaxWidth().clickable(enabled = !hardwarePlusMode) {
              preferences.anime4kQuality.set(option.name)
              onChanged()
            }.padding(vertical = MaterialTheme.spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = quality == option,
              enabled = !hardwarePlusMode,
              onClick = {
                preferences.anime4kQuality.set(option.name)
                onChanged()
              },
            )
            Text(stringResource(option.titleRes))
          }
        }
      }
    }
  }
}
