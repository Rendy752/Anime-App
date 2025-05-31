package com.luminoverse.animevibe.ui.common

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.animeDetailPlaceholder

@Preview
@Composable
fun ScreenshotDisplay(
    modifier: Modifier = Modifier,
    imageUrl: String? = animeDetailPlaceholder.images.webp.large_image_url,
    screenshot: String? = "",
    onClick: (() -> Unit)? = null,
) {
    var showDialog by remember { mutableStateOf(false) }
    val screenshotBitmap = screenshot?.let { base64String ->
        try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (_: Exception) {
            null
        }
    }

    when {
        screenshotBitmap != null -> {
            Image(
                bitmap = screenshotBitmap.asImageBitmap(),
                contentDescription = "Episode Screenshot",
                modifier = modifier
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        if (onClick != null) {
                            onClick()
                        } else {
                            showDialog = true
                        }
                    }
            )
        }

        imageUrl != null -> {
            AsyncImageWithPlaceholder(
                model = imageUrl,
                contentDescription = "Episode Image URL",
                modifier = modifier
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp)),
                onClick = onClick,
            )
        }

        else -> {
            Text(
                text = "No screenshot available",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = modifier
                    .aspectRatio(16f / 9f)
            )
        }
    }

    if (showDialog) {
        ImagePreviewDialog(
            image = screenshotBitmap ?: imageUrl,
            contentDescription = "Episode Screenshot Preview",
            onDismiss = { showDialog = false }
        )
    }
}