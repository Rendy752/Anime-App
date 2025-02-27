package com.example.animeapp.utils

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.example.animeapp.models.EpisodeSourcesResponse

object HlsPlayerUtil {

    @OptIn(UnstableApi::class)
    fun initializePlayer(
        context: Context,
        playerView: PlayerView,
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
                    .setLabel(track.label)
                    .build()
                subtitleConfigurations.add(subtitleConfiguration)
            }

            mediaItemBuilder.setSubtitleConfigurations(subtitleConfigurations)

            player.setMediaItem(mediaItemBuilder.build())
            player.prepare()
            player.play()
        }
    }

    fun releasePlayer(playerView: PlayerView) {
        playerView.player?.release()
        playerView.player = null
    }
}