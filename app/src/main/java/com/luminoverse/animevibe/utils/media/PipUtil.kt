package com.luminoverse.animevibe.utils.media

import android.app.RemoteAction
import android.content.Context
import android.graphics.drawable.Icon
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media3.session.R

object PipUtil {
    fun buildPipActions(context: Context, isPlaying: Boolean?): List<RemoteAction> {
        return listOf(
            RemoteAction(
                Icon.createWithResource(
                    context,
                    R.drawable.media3_icon_skip_back_10
                ),
                "Rewind",
                "Seek back 10 seconds",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    PlaybackStateCompat.ACTION_REWIND
                )
            ),
            RemoteAction(
                Icon.createWithResource(
                    context,
                    if (isPlaying == true) R.drawable.media3_icon_pause else R.drawable.media3_icon_play
                ),
                if (isPlaying == true) "Pause" else "Play",
                if (isPlaying == true) "Pause playback" else "Resume playback",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
            ),
            RemoteAction(
                Icon.createWithResource(
                    context,
                    R.drawable.media3_icon_skip_forward_10
                ),
                "Fast Forward",
                "Seek forward 10 seconds",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    PlaybackStateCompat.ACTION_FAST_FORWARD
                )
            )
        )
    }
}