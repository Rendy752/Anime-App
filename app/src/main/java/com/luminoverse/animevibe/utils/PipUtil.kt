package com.luminoverse.animevibe.utils

import android.app.RemoteAction
import android.graphics.drawable.Icon
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media3.session.R
import com.luminoverse.animevibe.ui.main.MainActivity

object PipUtil {
    fun buildPipActions(activity: MainActivity, isPlaying: Boolean?): List<RemoteAction> {
        return listOf(
            RemoteAction(
                Icon.createWithResource(
                    activity,
                    R.drawable.media3_icon_skip_back_10
                ),
                "Rewind",
                "Seek back 10 seconds",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    activity,
                    PlaybackStateCompat.ACTION_REWIND
                )
            ),
            RemoteAction(
                Icon.createWithResource(
                    activity,
                    if (isPlaying == true) R.drawable.media3_icon_pause else R.drawable.media3_icon_play
                ),
                if (isPlaying == true) "Pause" else "Play",
                if (isPlaying == true) "Pause playback" else "Resume playback",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    activity,
                    PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
            ),
            RemoteAction(
                Icon.createWithResource(
                    activity,
                    R.drawable.media3_icon_skip_forward_10
                ),
                "Fast Forward",
                "Seek forward 10 seconds",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    activity,
                    PlaybackStateCompat.ACTION_FAST_FORWARD
                )
            )
        )
    }
}