package com.example.animeapp.utils

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.media3.exoplayer.ExoPlayer
import com.example.animeapp.models.EpisodeSourcesResponse

class IntroOutroHandler(
    private val player: ExoPlayer,
    private val introButton: Button,
    private val outroButton: Button,
    private val videoData: EpisodeSourcesResponse
) : Runnable {

    private var introSkipped = false
    private var outroSkipped = false

    override fun run() {
        val currentPositionSec = player.currentPosition / 1000
        val intro = videoData.intro
        val outro = videoData.outro

        if (intro != null && currentPositionSec in intro.start..intro.end && !introSkipped) {
            if (introButton.visibility != View.VISIBLE) {
                introButton.visibility = View.VISIBLE
                setupIntroSkipButton(intro.end.toLong())
            }
        } else {
            if (introButton.visibility == View.VISIBLE) {
                introButton.visibility = View.GONE
            }
        }

        if (outro != null && currentPositionSec in outro.start..outro.end && !outroSkipped) {
            if (outroButton.visibility != View.VISIBLE) {
                outroButton.visibility = View.VISIBLE
                setupOutroSkipButton(outro.end.toLong())
            }
        } else {
            if (outroButton.visibility == View.VISIBLE) {
                outroButton.visibility = View.GONE
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
        introButton.setOnClickListener {
            player.seekTo(endTime * 1000L)
            introButton.visibility = View.GONE
            introSkipped = true
        }
    }

    private fun setupOutroSkipButton(endTime: Long) {
        outroButton.setOnClickListener {
            player.seekTo(endTime * 1000L)
            outroButton.visibility = View.GONE
            outroSkipped = true
        }
    }
}