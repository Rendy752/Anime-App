package com.example.animeapp.utils

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.AudioFocusRequest
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.C
import com.example.animeapp.models.EpisodeSourcesResponse
import androidx.core.net.toUri

object HlsPlayerUtil {

    private var audioFocusChangeListener: OnAudioFocusChangeListener? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusRequested = false

    @OptIn(UnstableApi::class)
    fun initializePlayer(
        player: ExoPlayer,
        videoData: EpisodeSourcesResponse
    ) {
        if (videoData.sources.isNotEmpty() && videoData.sources[0].type == "hls") {
            val mediaItemUri = videoData.sources[0].url.toUri()
            val mediaItemBuilder = MediaItem.Builder().setUri(mediaItemUri)

            if (videoData.tracks.any { it.kind == "captions" }) {
                val subtitleConfigurations = mutableListOf<SubtitleConfiguration>()
                videoData.tracks.filter { it.kind == "captions" }.forEach { track ->
                    val subtitleConfiguration = SubtitleConfiguration.Builder(track.file.toUri())
                        .setMimeType(MimeTypes.TEXT_VTT)
                        .setLanguage(track.label?.substringBefore("-")?.trim())
                        .setSelectionFlags(if (track.default == true) C.SELECTION_FLAG_DEFAULT else 0)
                        .setLabel(track.label?.substringBefore("-")?.trim())
                        .build()
                    subtitleConfigurations.add(subtitleConfiguration)
                }
                mediaItemBuilder.setSubtitleConfigurations(subtitleConfigurations)
            }

            player.setMediaItem(mediaItemBuilder.build())
            player.prepare()
            player.pause()
        }
    }

    fun requestAudioFocus(audioManager: AudioManager) {
        if (!audioFocusRequested) {
            if (audioFocusRequest == null) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(
                        audioFocusChangeListener ?: OnAudioFocusChangeListener { }
                    )
                    .build()
            }

            val result = audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocusRequested = true
            }
        }
    }

    fun abandonAudioFocus(audioManager: AudioManager) {
        if (audioFocusRequested) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequested = false
        }
    }
}