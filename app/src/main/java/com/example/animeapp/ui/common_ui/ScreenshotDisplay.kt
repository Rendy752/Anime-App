package com.example.animeapp.ui.common_ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.animeDetailPlaceholder

@Preview
@Composable
fun ScreenshotDisplayPreview() {
    ScreenshotDisplay(
        imageUrl = animeDetailPlaceholder.images.webp.large_image_url,
        screenshot = "")
}

@Composable
fun ScreenshotDisplay(
    imageUrl: String?,
    screenshot: String?,
    modifier: Modifier = Modifier
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        if (screenshotBitmap != null) {
            Image(
                bitmap = screenshotBitmap.asImageBitmap(),
                contentDescription = "Episode Screenshot",
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .clickable { showDialog = true }
            )
        } else if (imageUrl != null) {
            AsyncImageWithPlaceholder(
                model = imageUrl,
                contentDescription = "Episode Image URL",
                modifier = Modifier
                    .matchParentSize()
                    .padding(8.dp),
                isClickable = true
            )
        } else {
            Text(
                text = "No screenshot available",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
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