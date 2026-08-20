package app.marlboroadvance.mpvex.preferences

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Segment
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Segment
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Headset
import androidx.compose.material.icons.outlined.HdrOn
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import app.marlboroadvance.mpvex.R

/**
 * Represents a customizable button in the player controls.
 * Now includes an icon for the preference UI.
 */
enum class PlayerButton(
  val icon: ImageVector,
) {
  BACK_ARROW(Icons.AutoMirrored.Outlined.ArrowBack),
  VIDEO_TITLE(Icons.Outlined.Title),
  BOOKMARKS_CHAPTERS(Icons.Outlined.Bookmarks),
  PLAYBACK_SPEED(Icons.Outlined.Speed),
  DECODER(Icons.Outlined.Memory),
  HDR_MODE(Icons.Outlined.HdrOn),
  SCREEN_ROTATION(Icons.Outlined.ScreenRotation),
  FRAME_NAVIGATION(Icons.Outlined.Camera),
  VIDEO_ZOOM(Icons.Outlined.ZoomIn),
  PICTURE_IN_PICTURE(Icons.Outlined.PictureInPictureAlt),
  ASPECT_RATIO(Icons.Outlined.AspectRatio),
  LOCK_CONTROLS(Icons.Outlined.LockOpen),
  AUDIO_TRACK(Icons.Outlined.Audiotrack),
  SUBTITLES(Icons.Outlined.Subtitles),
  MORE_OPTIONS(Icons.Outlined.MoreVert),
  CURRENT_CHAPTER(Icons.Outlined.Bookmarks), // <-- CHANGED ICON
  REPEAT_MODE(Icons.Outlined.Repeat),
  SHUFFLE(Icons.Outlined.Shuffle),
  MIRROR(Icons.Outlined.Flip),
  VERTICAL_FLIP(Icons.Outlined.Flip),
  AB_LOOP(Icons.Outlined.Autorenew),
  CUSTOM_SKIP(Icons.Outlined.FastForward),
  BACKGROUND_PLAYBACK(Icons.Outlined.Headset),
  NONE(Icons.Outlined.Bookmarks),
}

/**
 * A list of all buttons that the user can choose from in the customization menu.
 * Excludes NONE (placeholder) and constant buttons (BACK_ARROW, VIDEO_TITLE).
 */
val allPlayerButtons =
  PlayerButton.values().filter {
    it != PlayerButton.NONE &&
      it != PlayerButton.BACK_ARROW &&
      it != PlayerButton.VIDEO_TITLE
  }

/**
 * Gets the localized, human-readable label for a player button.
 */
@Composable
fun getPlayerButtonLabel(button: PlayerButton): String =
  when (button) {
    PlayerButton.BACK_ARROW -> stringResource(R.string.player_button_back)
    PlayerButton.VIDEO_TITLE -> stringResource(R.string.player_button_video_title)
    PlayerButton.BOOKMARKS_CHAPTERS -> stringResource(R.string.player_button_chapters_bookmarks)
    PlayerButton.PLAYBACK_SPEED -> stringResource(R.string.player_button_playback_speed)
    PlayerButton.DECODER -> stringResource(R.string.player_button_decoder)
    PlayerButton.HDR_MODE -> stringResource(R.string.player_button_hdr)
    PlayerButton.SCREEN_ROTATION -> stringResource(R.string.player_button_screen_rotation)
    PlayerButton.FRAME_NAVIGATION -> stringResource(R.string.player_button_frame_navigation)
    PlayerButton.VIDEO_ZOOM -> stringResource(R.string.player_button_video_zoom)
    PlayerButton.PICTURE_IN_PICTURE -> stringResource(R.string.player_button_picture_in_picture)
    PlayerButton.ASPECT_RATIO -> stringResource(R.string.player_button_aspect_ratio)
    PlayerButton.LOCK_CONTROLS -> stringResource(R.string.player_button_lock_controls)
    PlayerButton.AUDIO_TRACK -> stringResource(R.string.player_button_audio_track)
    PlayerButton.SUBTITLES -> stringResource(R.string.player_button_subtitles)
    PlayerButton.MORE_OPTIONS -> stringResource(R.string.player_button_more_options)
    PlayerButton.CURRENT_CHAPTER -> stringResource(R.string.player_button_current_chapter)
    PlayerButton.REPEAT_MODE -> stringResource(R.string.player_button_repeat_mode)
    PlayerButton.SHUFFLE -> stringResource(R.string.player_button_shuffle)
    PlayerButton.MIRROR -> stringResource(R.string.player_button_horizontal_flip)
    PlayerButton.VERTICAL_FLIP -> stringResource(R.string.player_button_vertical_flip)
    PlayerButton.AB_LOOP -> stringResource(R.string.player_button_ab_loop)
    PlayerButton.CUSTOM_SKIP -> stringResource(R.string.player_button_custom_skip)
    PlayerButton.BACKGROUND_PLAYBACK -> stringResource(R.string.player_button_background_playback)
    PlayerButton.NONE -> stringResource(R.string.player_button_none)
  }
