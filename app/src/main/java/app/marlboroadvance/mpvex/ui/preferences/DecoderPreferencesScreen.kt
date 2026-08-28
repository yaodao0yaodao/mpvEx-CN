package app.marlboroadvance.mpvex.ui.preferences

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.DecoderPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.player.Debanding
import app.marlboroadvance.mpvex.ui.player.Decoder
import app.marlboroadvance.mpvex.ui.player.MPVProfile
import app.marlboroadvance.mpvex.ui.player.VulkanCapabilities
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import org.koin.compose.koinInject

@Serializable
object DecoderPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<DecoderPreferences>()
    val backstack = LocalBackStack.current
    val context = LocalContext.current
    val isVulkanSupported = remember { VulkanCapabilities.isDeviceSupported(context) }
    var showGpuNextWarning by remember { mutableStateOf(false) }
    var showDecoderPriorityDialog by remember { mutableStateOf(false) }
    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = stringResource(R.string.pref_decoder),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary,
            )
          },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(
                Icons.AutoMirrored.Default.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
            }
          },
        )
      },
    ) { padding ->
      ProvidePreferenceLocals {
        LazyColumn(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(padding),
        ) {
          item {
            PreferenceSectionHeader(title = stringResource(R.string.pref_decoder))
          }

          item {
            PreferenceCard {
              val profile by preferences.profile.collectAsState()
              val currentProfile = MPVProfile.fromValue(profile)
              ListPreference(
                value = currentProfile,
                onValueChange = { preferences.profile.set(it.value) },
                values = MPVProfile.entries,
                valueToText = { AnnotatedString(context.getString(it.titleRes)) },
                title = { Text(stringResource(R.string.pref_decoder_profile_title)) },
                summary = {
                  Text(
                    stringResource(currentProfile.titleRes),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val decoderPriority by preferences.decoderPriority.collectAsState()
              Column(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .clickable { showDecoderPriorityDialog = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
              ) {
                Text(stringResource(R.string.pref_decoder_priority_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                  decoderPriority.joinToString(" → ") { context.getString(it.titleRes) },
                  color = MaterialTheme.colorScheme.outline,
                  style = MaterialTheme.typography.bodyMedium,
                )
              }

              PreferenceDivider()

              val gpuNext by preferences.gpuNext.collectAsState()
              val useVulkan by preferences.useVulkan.collectAsState() // Added to check Vulkan state
              SwitchPreference(
                value = gpuNext,
                onValueChange = { enabled ->
                    if (enabled && !gpuNext && !useVulkan) { // Only show warning if Vulkan is disabled
                        showGpuNextWarning = true
                    } else {
                        preferences.gpuNext.set(enabled)
                    }
                },
                title = { Text(stringResource(R.string.pref_decoder_gpu_next_title)) },
                summary = {
                  Text(
                    stringResource(R.string.pref_decoder_gpu_next_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              if (showGpuNextWarning) {
                  AlertDialog(
                      onDismissRequest = { showGpuNextWarning = false },
                      title = { Text(stringResource(R.string.pref_decoder_gpu_next_enable_title)) },
                      text = {
                          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                              Text(stringResource(R.string.pref_decoder_gpu_next_warning))
                              Text(stringResource(R.string.pref_decoder_gpu_next_purple_screen_fix))
                              
                              Surface(
                                  color = MaterialTheme.colorScheme.errorContainer,
                                  shape = MaterialTheme.shapes.small
                              ) {
                                  Column(modifier = Modifier.padding(8.dp)) {
                                      Text(
                                          text = stringResource(R.string.pref_anime4k_incompatibility),
                                          style = MaterialTheme.typography.titleSmall,
                                          color = MaterialTheme.colorScheme.onErrorContainer
                                      )
                                      Text(
                                          text = stringResource(R.string.pref_anime4k_gpu_next_error),
                                          style = MaterialTheme.typography.bodySmall,
                                          color = MaterialTheme.colorScheme.onErrorContainer
                                      )
                                  }
                              }
                          }
                      },
                      confirmButton = {
                          Button(onClick = {
                              preferences.gpuNext.set(true)
                              showGpuNextWarning = false
                          }) {
                              Text(stringResource(R.string.pref_decoder_gpu_next_enable_anyway))
                          }
                      },
                      dismissButton = {
                          TextButton(onClick = { showGpuNextWarning = false }) {
                              Text(stringResource(R.string.generic_cancel))
                          }
                      }
                  )
              }

              PreferenceDivider()

              // val useVulkan by preferences.useVulkan.collectAsState() // Moved up for gpuNext logic
              SwitchPreference(
                value = useVulkan,
                onValueChange = { enabled ->
                  preferences.useVulkan.set(enabled)
                },
                enabled = isVulkanSupported,
                title = { Text(stringResource(R.string.pref_decoder_vulkan_title) + " (Experimental)") },
                summary = {
                  Column {
                    Text(
                      stringResource(
                        if (isVulkanSupported) R.string.pref_decoder_vulkan_summary
                        else R.string.pref_decoder_vulkan_not_supported
                      ),
                      color = if (isVulkanSupported) MaterialTheme.colorScheme.outline
                             else MaterialTheme.colorScheme.error,
                    )
                    Text(
                      text = stringResource(R.string.pref_decoder_guide_link),
                      color = MaterialTheme.colorScheme.primary,
                      style = MaterialTheme.typography.bodySmall,
                      textDecoration = TextDecoration.Underline,
                      modifier = Modifier.clickable {
                        val intent = Intent(
                          Intent.ACTION_VIEW,
                          Uri.parse("https://github.com/yaodao0yaodao/mpvEx-CN/blob/master/docs/Decoder-and-Battery.zh-CN.md"),
                        )
                        context.startActivity(intent)
                      },
                    )
                  }
                },
              )

              PreferenceDivider()

              val debanding by preferences.debanding.collectAsState()
              ListPreference(
                value = debanding,
                onValueChange = { preferences.debanding.set(it) },
                values = Debanding.entries,
                title = { Text(stringResource(R.string.pref_decoder_debanding_title)) },
                summary = {
                  Text(
                    debanding.name,
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val useYUV420p by preferences.useYUV420P.collectAsState()
              SwitchPreference(
                value = useYUV420p,
                onValueChange = {
                  preferences.useYUV420P.set(it)
                },
                title = { Text(stringResource(R.string.pref_decoder_yuv420p_title)) },
                summary = {
                  Text(
                    stringResource(R.string.pref_decoder_yuv420p_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

            }
          }
        }
      }
    }

    if (showDecoderPriorityDialog) {
      val priority by preferences.decoderPriority.collectAsState()
      AlertDialog(
        onDismissRequest = { showDecoderPriorityDialog = false },
        title = { Text(stringResource(R.string.pref_decoder_priority_title)) },
        text = {
          Column {
            Text(
              stringResource(R.string.pref_decoder_priority_summary),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.outline,
              modifier = Modifier.padding(bottom = 8.dp),
            )
            priority.forEachIndexed { index, decoder ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = context.getString(decoder.titleRes),
                  style = MaterialTheme.typography.bodyLarge,
                  modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                )
                IconButton(
                  onClick = {
                    val updated = priority.toMutableList()
                    val item = updated.removeAt(index)
                    updated.add(index - 1, item)
                    preferences.decoderPriority.set(updated)
                  },
                  enabled = index > 0,
                ) {
                  Icon(Icons.Default.ExpandLess, contentDescription = null)
                }
                IconButton(
                  onClick = {
                    val updated = priority.toMutableList()
                    val item = updated.removeAt(index)
                    updated.add(index + 1, item)
                    preferences.decoderPriority.set(updated)
                  },
                  enabled = index < priority.lastIndex,
                ) {
                  Icon(Icons.Default.ExpandMore, contentDescription = null)
                }
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { showDecoderPriorityDialog = false }) {
            Text(stringResource(R.string.generic_ok))
          }
        },
      )
    }

  }
}
