package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import android.graphics.Rect
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.luminoverse.animevibe.utils.media.CropTransformation
import com.luminoverse.animevibe.utils.media.VttCue
import com.luminoverse.animevibe.utils.media.findCueForPosition

private enum class ImageLoadingState {
    Loading, Success, Error
}

@Composable
fun ThumbnailPreview(
    modifier: Modifier = Modifier,
    seekPosition: Long,
    cues: List<VttCue>?,
) {
    val context = LocalContext.current
    var imageLoadingState by remember { mutableStateOf(ImageLoadingState.Loading) }

    val currentCue = cues?.let { findCueForPosition(seekPosition, it) }

    val imageUrl = currentCue?.imageUrl
    val cropRect = currentCue?.let { Rect(it.x, it.y, it.x + it.width, it.y + it.height) }

    LaunchedEffect(imageUrl) {
        if (imageUrl != null) {
            imageLoadingState = ImageLoadingState.Loading
        }
    }

    Box(
        modifier = if (imageLoadingState == ImageLoadingState.Error) {
            modifier
        } else {
            modifier.background(Color(0xFF14161A))
        },
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null && cropRect != null) {
            val imageRequest = remember(imageUrl, cropRect) {
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(Size.ORIGINAL)
                    .transformations(listOf(CropTransformation(cropRect)))
                    .crossfade(true)
                    .listener(
                        onStart = { imageLoadingState = ImageLoadingState.Loading },
                        onSuccess = { _, _ -> imageLoadingState = ImageLoadingState.Success },
                        onError = { _, result ->
                            Log.e("ThumbnailPreview", "Image failed to load: ${result.throwable}")
                            imageLoadingState = ImageLoadingState.Error
                        }
                    )
                    .build()
            }
            val painter = rememberAsyncImagePainter(
                model = imageRequest,
                imageLoader = ImageLoader(context)
            )

            if (imageLoadingState == ImageLoadingState.Success) {
                Image(
                    painter = painter,
                    contentDescription = "Thumbnail Preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}