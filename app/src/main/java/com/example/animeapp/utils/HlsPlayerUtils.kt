package com.example.animeapp.utils

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.AudioFocusRequest
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
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
import androidx.media3.common.C
import com.example.animeapp.models.EpisodeSourcesResponse

object HlsPlayerUtil {

    private var audioFocusChangeListener: OnAudioFocusChangeListener? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusRequested = false

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

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioFocusChangeListener = OnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        player.volume = 1f
                        if (player.playbackState == Player.STATE_READY) {
                            player.play()
                        }
                    }

                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        player.pause()
                    }

                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        player.volume = 0.5f
                    }
                }
            }

            player.addListener(object : Player.Listener {

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        handler.removeCallbacks(runnable)
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)

                    if (isPlaying) {
                        requestAudioFocus(audioManager)
                    } else {
                        abandonAudioFocus(audioManager)
                    }

                    (context as Activity).setPictureInPictureParams(
                        PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(16, 9))
                            .build()
                    )
                }
            })
        }
    }

    private fun requestAudioFocus(audioManager: AudioManager) {
        if (!audioFocusRequested) {
            if (audioFocusRequest == null) {
                Media3AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build()

                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(
                        audioFocusChangeListener ?: OnAudioFocusChangeListener { })
                    .build()
            }

            val result = audioManager.requestAudioFocus(audioFocusRequest!!)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocusRequested = true
            }
        }
    }

    private fun abandonAudioFocus(audioManager: AudioManager) {
        if (audioFocusRequested) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
            audioFocusRequested = false
        }
    }

    fun releasePlayer(playerView: PlayerView) {
        playerView.player?.release()
        playerView.player = null
    }
}