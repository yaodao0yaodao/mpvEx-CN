package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.DecoderPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.player.Decoder
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.koinInject

@Composable
fun DecodersSheet(
  selectedDecoder: Decoder,
  onSelect: (Decoder) -> Unit,
  onDismissRequest: () -> Unit,
) {
  val preferences = koinInject<DecoderPreferences>()
  val hardwarePlusEnabled by preferences.hardwarePlusEnabled.collectAsState()
  val availableDecoders =
    Decoder.priorityModes.filter { it != Decoder.HWPlus || hardwarePlusEnabled }
  GenericTracksSheet(
    availableDecoders.toImmutableList(),
    track = {
      AudioTrackRow(
        title = stringResource(R.string.player_sheets_decoder_formatted, stringResource(it.titleRes), it.value),
        isSelected = selectedDecoder == it,
        onClick = { onSelect(it) },
      )
    },
    onDismissRequest = onDismissRequest,
  )
}
