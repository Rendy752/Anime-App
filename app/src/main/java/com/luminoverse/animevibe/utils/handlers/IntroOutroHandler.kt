package com.luminoverse.animevibe.utils.handlers

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.luminoverse.animevibe.models.EpisodeSourcesResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IntroOutroHandler(
    private val player: ExoPlayer,
    private val videoData: EpisodeSourcesResponse
) {
    private var introSkipped = false
    private var outroSkipped = true
    private var job: Job? = null
    val showIntroButton: MutableState<Boolean> = mutableStateOf(false)
    val showOutroButton: MutableState<Boolean> = mutableStateOf(false)

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) start() else stop()
        }
    }

    fun start() {
        if (job?.isActive != true) {
            player.addListener(playerListener)
            job = CoroutineScope(Dispatchers.Main).launch {
                while (true) {
                    runHandlerLogic()
                    delay(1000)
                }
            }
        }
    }

    private fun runHandlerLogic() {
        val currentPositionSec = player.currentPosition / 1000
        Log.d("IntroOutroHandler", "Current Position: $currentPositionSec")

        val intro = videoData.intro
        val outro = videoData.outro

        showIntroButton.value =
            intro != null && currentPositionSec in intro.start..intro.end && !introSkipped

        showOutroButton.value = outro != null && currentPositionSec in outro.start..outro.end && !outroSkipped

        if (intro != null && (currentPositionSec < intro.start || currentPositionSec > intro.end)) {
            introSkipped = false
        }

        if (outro != null && (currentPositionSec < outro.start || currentPositionSec > outro.end)) {
            outroSkipped = false
        }
    }

    fun skipIntro(endTime: Long) {
        player.seekTo(endTime * 1000L)
        introSkipped = true
        showIntroButton.value = false
    }

    fun skipOutro(endTime: Long) {
        player.seekTo(endTime * 1000L)
        outroSkipped = true
        showOutroButton.value = false
    }

    fun stop() {
        job?.cancel()
        job = null
        player.removeListener(playerListener)
        showIntroButton.value = false
        showOutroButton.value = false
        introSkipped = false
        outroSkipped = true
    }
}