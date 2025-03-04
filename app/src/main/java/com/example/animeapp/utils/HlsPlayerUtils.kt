package com.example.animeapp.utils

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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.common.Player
import androidx.media3.common.C
import com.example.animeapp.models.EpisodeSourcesResponse

object HlsPlayerUtil {

    private var audioFocusChangeListener: OnAudioFocusChangeListener? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusRequested = false

    @OptIn(UnstableApi::class)
    fun initializePlayer(
        player: ExoPlayer,
        skipButton: Button,
        videoData: EpisodeSourcesResponse
    ) {

        if (videoData.sources.isNotEmpty() && videoData.sources[0].type == "hls") {

            val mediaItemUri = Uri.parse(videoData.sources[0].url)
            val mediaItemBuilder = MediaItem.Builder()
                .setUri(mediaItemUri)

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

            val handler = Handler(Looper.getMainLooper())
            val runnable = object : Runnable {
                private var introSkipped = false
                private var outroSkipped = true

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
            })
        }
    }

    fun requestAudioFocus(audioManager: AudioManager) {
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

    fun abandonAudioFocus(audioManager: AudioManager) {
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