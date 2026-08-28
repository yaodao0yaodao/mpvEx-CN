package app.marlboroadvance.mpvex.ui.player

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import app.marlboroadvance.mpvex.MainActivity

object LowBatteryAdvisor {
  private const val PREFS = "mpvex_battery_advice"
  private const val KEY_DISABLED = "disabled"

  fun show(activity: PlayerActivity, hardwarePlusEnabled: Boolean) {
    val preferences = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    if (preferences.getBoolean(KEY_DISABLED, false) || activity.isFinishing || activity.isDestroyed) return

    val builder =
      AlertDialog.Builder(activity)
        .setTitle("电量低于 30%")
        .setMessage("硬件解码增强通常能以最低功耗播放高码率视频，并可与 Vulkan、AI 超分辨率等功能配合使用；部分设备在此解码模式下可能无法激活屏幕真实 HDR。")
        .setNegativeButton("不再提醒") { dialog, _ ->
          preferences.edit().putBoolean(KEY_DISABLED, true).apply()
          dialog.dismiss()
        }
        .setNeutralButton("关闭") { dialog, _ -> dialog.dismiss() }

    if (!hardwarePlusEnabled) {
      builder.setPositiveButton("设置解码器优先级") { dialog, _ ->
        activity.startActivity(
          Intent(activity, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_DECODER_SETTINGS, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        dialog.dismiss()
      }
    } else {
      builder.setPositiveButton("知道了") { dialog, _ -> dialog.dismiss() }
    }
    builder.show()
  }
}
