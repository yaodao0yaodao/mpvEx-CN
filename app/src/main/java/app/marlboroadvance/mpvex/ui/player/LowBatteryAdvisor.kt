package app.marlboroadvance.mpvex.ui.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import app.marlboroadvance.mpvex.MainActivity
import app.marlboroadvance.mpvex.R

object LowBatteryAdvisor {
  private const val CHANNEL_ID = "mpvex_battery_advice"
  private const val NOTIFICATION_ID = 31
  private const val PREFS = "mpvex_battery_advice"
  private const val KEY_DISABLED = "disabled"

  fun show(context: Context, hardwarePlusEnabled: Boolean) {
    if (context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DISABLED, false)) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(
      NotificationChannel(CHANNEL_ID, "播放续航建议", NotificationManager.IMPORTANCE_DEFAULT).apply {
        description = "电量较低时提供更省电的解码建议"
      },
    )
    fun receiver(action: String, requestCode: Int) =
      PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, LowBatteryActionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val builder =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("电量低于 30%")
        .setContentText("硬件解码增强通常能以最低功耗播放高码率视频。")
        .setStyle(NotificationCompat.BigTextStyle().bigText("硬件解码增强通常能以最低功耗播放高码率视频；它会停用 Vulkan、AI 超分辨率和 SDR→HDR 增强。"))
        .setAutoCancel(true)
        .addAction(0, "不再提醒", receiver(LowBatteryActionReceiver.ACTION_NEVER, 310))
        .addAction(0, "关闭通知", receiver(LowBatteryActionReceiver.ACTION_DISMISS, 311))
    if (!hardwarePlusEnabled) {
      val settingsIntent =
        Intent(context, MainActivity::class.java)
          .putExtra(MainActivity.EXTRA_OPEN_DECODER_SETTINGS, true)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
      builder.addAction(
        0,
        "设置解码器优先级",
        PendingIntent.getActivity(context, 312, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
      )
    }
    manager.notify(NOTIFICATION_ID, builder.build())
  }

  internal fun dismiss(context: Context, never: Boolean) {
    if (never) context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DISABLED, true).apply()
    (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
  }
}

class LowBatteryActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    LowBatteryAdvisor.dismiss(context, intent.action == ACTION_NEVER)
  }

  companion object {
    const val ACTION_NEVER = "io.github.yaodao0yaodao.mpvex.BATTERY_NEVER"
    const val ACTION_DISMISS = "io.github.yaodao0yaodao.mpvex.BATTERY_DISMISS"
  }
}
