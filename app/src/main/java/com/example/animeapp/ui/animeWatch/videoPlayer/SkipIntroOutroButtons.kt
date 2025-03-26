package com.example.animeapp.ui.animeWatch.videoPlayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SkipIntroOutroButtons(
    showIntro: Boolean,
    showOutro: Boolean,
    introEnd: Long,
    outroEnd: Long,
    onSkipIntro: (Long) -> Unit,
    onSkipOutro: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (showIntro) SkipButton(
        label = "Skip Intro",
        skipTime = introEnd,
        onSkip = onSkipIntro,
        modifier = modifier
    )

    if (showOutro) SkipButton(
        label = "Skip Outro",
        skipTime = outroEnd,
        onSkip = onSkipOutro,
        modifier = modifier
    )
}