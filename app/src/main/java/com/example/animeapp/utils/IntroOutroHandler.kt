package com.example.animeapp.utils

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.media3.exoplayer.ExoPlayer
import com.example.animeapp.models.EpisodeSourcesResponse

class IntroOutroHandler(
    private val player: ExoPlayer,
    private val skipButton: Button,
    private val videoData: EpisodeSourcesResponse
) : Runnable {

    private var introSkipped = false
    private var outroSkipped = false

    override fun run() {
        val currentPositionSec = player.currentPosition / 1000
        val intro = videoData.intro
        val outro = videoData.outro

        if (intro != null && currentPositionSec in intro.start..intro.end && !introSkipped) {
            if (skipButton.visibility != View.VISIBLE) {
                skipButton.visibility = View.VISIBLE
                "Skip Intro".also { skipButton.text = it }
                setupIntroSkipButton(intro.end.toLong())
            }
        } else if (outro != null && currentPositionSec in outro.start..outro.end && !outroSkipped) {
            if (skipButton.visibility != View.VISIBLE) {
                skipButton.visibility = View.VISIBLE
                "Skip Outro".also { skipButton.text = it }
                setupOutroSkipButton(outro.end.toLong())
            }
        } else {
            if (skipButton.visibility == View.VISIBLE) {
                skipButton.visibility = View.GONE
            }
        }

        if (intro != null && (currentPositionSec < intro.start || currentPositionSec > intro.end)) {
            introSkipped = false
        }

        if (outro != null && (currentPositionSec < outro.start || currentPositionSec > outro.end)) {
            outroSkipped = false
        }

        Handler(Looper.getMainLooper()).postDelayed(this, 1000)
    }

    private fun setupIntroSkipButton(endTime: Long) {
        skipButton.setOnClickListener {
            player.seekTo(endTime * 1000L)
            skipButton.visibility = View.GONE
            introSkipped = true
        }
    }

    private fun setupOutroSkipButton(endTime: Long) {
        skipButton.setOnClickListener {
            player.seekTo(endTime * 1000L)
            skipButton.visibility = View.GONE
            outroSkipped = true
        }
    }
}