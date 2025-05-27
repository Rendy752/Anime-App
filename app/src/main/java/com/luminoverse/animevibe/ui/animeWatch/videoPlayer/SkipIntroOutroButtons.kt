package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SkipIntroOutroButtons(
    showIntro: Boolean,
    showOutro: Boolean,
    onSkipIntro: () -> Unit,
    onSkipOutro: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (showIntro) SkipButton(
        label = "Skip Intro",
        onSkip = onSkipIntro,
        modifier = modifier
    )

    if (showOutro) SkipButton(
        label = "Skip Outro",
        onSkip = onSkipOutro,
        modifier = modifier
    )
}