package app.marlboroadvance.mpvex.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.marlboroadvance.mpvex.ui.player.PlayerActivity

class DebugControlReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val command = intent.getStringExtra("command").orEmpty()
    val value = intent.getStringExtra("value")
    val result = PlayerActivity.activeInstance?.handleDebugCommand(command, value) ?: "ERROR: no active player"
    Log.i(TAG, result)
    resultData = result
  }

  companion object {
    private const val TAG = "mpvex-adb"
  }
}
