package app.marlboroadvance.mpvex.ui.player

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Restarts playback in a new application process after a decoder change.
 *
 * libmpv and Android's GPU/Surface stack both keep process-global native state.
 * Reinitialising libmpv in the same process while RenderThread still owns old
 * buffers can abort inside pthread_mutex_lock. This no-UI activity runs in an
 * isolated process, survives termination of the old player process, then starts
 * PlayerActivity after Android has reaped all of the old native threads.
 */
class DecoderRestartActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val playerIntent =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(EXTRA_PLAYER_INTENT, Intent::class.java)
      } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra(EXTRA_PLAYER_INTENT)
      }

    if (playerIntent == null) {
      Log.e(TAG, "Decoder restart requested without a player intent")
      finish()
      return
    }

    Handler(Looper.getMainLooper()).postDelayed(
      {
        runCatching {
          playerIntent.setClass(this, PlayerActivity::class.java)
          playerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
          startActivity(playerIntent)
        }.onFailure { Log.e(TAG, "Unable to relaunch player after decoder change", it) }
        finishAndRemoveTask()
      },
      RESTART_DELAY_MS,
    )
  }

  companion object {
    const val EXTRA_PLAYER_INTENT = "mpvex.decoder_restart.player_intent"
    private const val RESTART_DELAY_MS = 300L
    private const val TAG = "DecoderRestart"
  }
}
