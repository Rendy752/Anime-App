package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import android.graphics.Rect
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
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.luminoverse.animevibe.utils.media.CropTransformation
import com.luminoverse.animevibe.utils.media.ThumbnailCue
import com.luminoverse.animevibe.utils.media.findThumbnailCueForPosition

private enum class ImageLoadingState {
    Loading, Success, Error
}

@Composable
fun ThumbnailPreview(
    modifier: Modifier = Modifier,
    seekPosition: Long,
    cues: List<ThumbnailCue>?,
) {
    val context = LocalContext.current
    var imageLoadingState by remember { mutableStateOf(ImageLoadingState.Loading) }

    val currentCue = cues?.let { findThumbnailCueForPosition(seekPosition, it) }
    val imageUrl = currentCue?.imageUrl
    val cropRect = currentCue?.let { Rect(it.x, it.y, it.x + it.width, it.y + it.height) }

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .size(Size.ORIGINAL)
            .transformations(
                if (cropRect != null) listOf(CropTransformation(cropRect)) else emptyList()
            )
            .build()
    )

    imageLoadingState = when (painter.state) {
        is AsyncImagePainter.State.Loading -> ImageLoadingState.Loading
        is AsyncImagePainter.State.Success -> ImageLoadingState.Success
        is AsyncImagePainter.State.Error, is AsyncImagePainter.State.Empty -> ImageLoadingState.Error
    }

    Box(
        modifier = if (imageLoadingState == ImageLoadingState.Error) {
            modifier
        } else {
            modifier.background(Color.Black)
        },
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null && imageLoadingState == ImageLoadingState.Success) {
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