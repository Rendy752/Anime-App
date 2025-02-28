package com.example.animeapp.utils

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import androidx.media3.common.Player
import android.util.Rational
import com.example.animeapp.models.EpisodeSourcesResponse

object HlsPlayerUtil {

    @OptIn(UnstableApi::class)
    fun initializePlayer(
        context: Context,
        playerView: PlayerView,
        skipButton: Button,
        videoData: EpisodeSourcesResponse
    ) {
        val player = ExoPlayer.Builder(context).build()
        playerView.player = player

        if (videoData.sources.isNotEmpty() && videoData.sources[0].type == "hls") {
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("AnimeApp")

            val mediaItem = MediaItem.fromUri(Uri.parse(videoData.sources[0].url))
            HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)

            val mediaItemBuilder = mediaItem.buildUpon()

            val subtitleConfigurations = mutableListOf<SubtitleConfiguration>()

            videoData.tracks.filter { it.kind == "captions" }.forEach { track ->
                val subtitleConfiguration = SubtitleConfiguration.Builder(Uri.parse(track.file))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage(track.label?.substringBefore("-")?.trim())
                    .setLabel(track.label?.substringBefore("-")?.trim())
                    .build()
                subtitleConfigurations.add(subtitleConfiguration)
            }

            mediaItemBuilder.setSubtitleConfigurations(subtitleConfigurations)

            player.setMediaItem(mediaItemBuilder.build())
            player.prepare()
            player.play()

            val handler = Handler(Looper.getMainLooper())
            val runnable = object : Runnable {
                private var introSkipped = false
                private var outroSkipped = false

                override fun run() {
                    val currentPositionSec = player.currentPosition / 1000
                    val intro = videoData.intro
                    val outro = videoData.outro

                    if (intro != null && currentPositionSec >= intro.start && currentPositionSec <= intro.end && !introSkipped) {
                        skipButton.visibility = View.VISIBLE
                        "Skip Intro".also { skipButton.text = it }
                        skipButton.setOnClickListener {
                            player.seekTo(intro.end * 1000L)
                            skipButton.visibility = View.GONE
                            introSkipped = true
                        }
                    } else if (outro != null && currentPositionSec >= outro.start && currentPositionSec <= outro.end && !outroSkipped) {
                        skipButton.visibility = View.VISIBLE
                        "Skip Outro".also { skipButton.text = it }
                        skipButton.setOnClickListener {
                            player.seekTo(outro.end * 1000L)
                            skipButton.visibility = View.GONE
                            outroSkipped = true
                        }
                    } else {
                        skipButton.visibility = View.GONE
                    }

                    if (intro != null && (currentPositionSec < intro.start || currentPositionSec > intro.end)) {
                        introSkipped = false
                    }

                    if (outro != null && (currentPositionSec < outro.start || currentPositionSec > outro.end)) {
                        outroSkipped = false
                    }

                    handler.postDelayed(this, 1000)
                }
            }
            handler.post(runnable)

            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        handler.removeCallbacks(runnable)
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    (context as Activity).setPictureInPictureParams(
                        PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(16, 9))
                            .build()
                    )

                }
            })
        }
    }

    fun releasePlayer(playerView: PlayerView) {
        playerView.player?.release()
        playerView.player = null
    }
}