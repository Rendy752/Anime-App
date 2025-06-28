package com.luminoverse.animevibe.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter

enum class ImageRoundedCorner { NONE, START, END, TOP, BOTTOM, ALL }

@Composable
fun AsyncImage(
    model: Any?,
    modifier: Modifier = Modifier,
    contentDescription: String?,
    isAiring: Boolean? = null,
    roundedCorners: ImageRoundedCorner = ImageRoundedCorner.ALL,
    onClick: (() -> Unit)? = null,
    onSizeLoaded: ((Size) -> Unit)? = null
) {
    var isImageLoading by remember { mutableStateOf(true) }

    val cornerRadius = 8.dp
    val shape: Shape = when (roundedCorners) {
        ImageRoundedCorner.NONE -> RoundedCornerShape(0.dp)
        ImageRoundedCorner.START -> RoundedCornerShape(
            topStart = cornerRadius,
            bottomStart = cornerRadius
        )

        ImageRoundedCorner.END -> RoundedCornerShape(
            topEnd = cornerRadius,
            bottomEnd = cornerRadius
        )

        ImageRoundedCorner.TOP -> RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
        ImageRoundedCorner.BOTTOM -> RoundedCornerShape(
            bottomStart = cornerRadius,
            bottomEnd = cornerRadius
        )

        ImageRoundedCorner.ALL -> RoundedCornerShape(cornerRadius)
    }

    Box(
        modifier = modifier
            .size(100.dp, 150.dp)
            .clip(shape)
            .then(onClick?.let { Modifier.clickable { it() } } ?: Modifier),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            onState = { state ->
                isImageLoading = state is AsyncImagePainter.State.Loading
                if (state is AsyncImagePainter.State.Success) {
                    onSizeLoaded?.invoke(state.painter.intrinsicSize)
                }
            }
        )

        if (isImageLoading) {
            Icon(
                imageVector = Icons.Filled.Image,
                contentDescription = "Placeholder",
                tint = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp)
            )
        } else {
            if (isAiring != null) {
                val icon = if (isAiring) Icons.Filled.NotificationsActive else Icons.Filled.Done
                val tint =
                    if (isAiring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = if (isAiring) "Airing" else "Finished",
                        tint = tint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}