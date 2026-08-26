package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.ui.theme.spacing
import kotlin.math.pow
import kotlin.math.roundToInt

@Suppress("UNUSED_PARAMETER")
@Composable
fun PlaybackSpeedSheet(
  speed: Float,
  speedPresets: List<Float>,
  onSpeedChange: (Float) -> Unit,
  onAddSpeedPreset: (Float) -> Unit,
  onRemoveSpeedPreset: (Float) -> Unit,
  onResetPresets: () -> Unit,
  onMakeDefault: (Float) -> Unit,
  onResetDefault: () -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  PlayerSheet(onDismissRequest = onDismissRequest) {
    Column(
      modifier
        .verticalScroll(rememberScrollState())
        .padding(vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = stringResource(R.string.player_sheets_speed_slider_label),
          style = MaterialTheme.typography.bodyMedium,
        )
        Text(
          text = "${speed.toFixed(2)}x",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
      ) {
        Slider(
          value = speed.coerceIn(0.1f, 4.0f),
          onValueChange = { onSpeedChange((it * 20).roundToInt() / 20f) },
          valueRange = 0.1f..4.0f,
          modifier = Modifier.weight(1f),
        )
        Button(onClick = { onSpeedChange(1.0f) }) {
          Text(text = stringResource(R.string.generic_reset))
        }
      }
    }
  }
}

fun Float.toFixed(precision: Int = 1): Float {
  val factor = 10.0f.pow(precision)
  return (this * factor).roundToInt() / factor
}
