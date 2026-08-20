package app.marlboroadvance.mpvex.ui.player

import android.content.Context
import android.os.Build
import android.os.PowerManager

object ThermalMonitor {
  fun readPressure(context: Context): ThermalPressure {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ThermalPressure.NORMAL
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
      ?: return ThermalPressure.NORMAL
    val thermalStatus = runCatching { powerManager.currentThermalStatus }.getOrDefault(PowerManager.THERMAL_STATUS_NONE)
    val forecastLoad =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching { powerManager.getThermalHeadroom(FORECAST_SECONDS) }
          .getOrNull()
          ?.takeIf(Float::isFinite)
      } else {
        null
      }

    return classifyThermalPressure(forecastLoad, thermalStatus)
  }

  private const val FORECAST_SECONDS = 10
}

internal fun classifyThermalPressure(
  forecastLoad: Float?,
  thermalStatus: Int,
): ThermalPressure =
  when {
    thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE ||
      (forecastLoad?.takeIf(Float::isFinite) ?: 0f) >= 1.0f -> ThermalPressure.SEVERE
    thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE ||
      (forecastLoad?.takeIf(Float::isFinite) ?: 0f) >= 0.8f -> ThermalPressure.ELEVATED
    else -> ThermalPressure.NORMAL
  }
