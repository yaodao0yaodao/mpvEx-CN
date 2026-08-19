package app.marlboroadvance.mpvex.preferences

import androidx.annotation.StringRes
import app.marlboroadvance.mpvex.R

enum class SeekbarStyle(
    @StringRes val titleRes: Int,
) {
    Standard(R.string.seekbar_style_standard),
    Wavy(R.string.seekbar_style_wavy),
    Thick(R.string.seekbar_style_thick),
}
