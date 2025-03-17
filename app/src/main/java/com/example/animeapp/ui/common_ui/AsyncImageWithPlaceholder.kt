package com.example.animeapp.ui.common_ui

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun AsyncImageWithPlaceholder(
    model: Any?,
    contentDescription: String?,
    isAiring: Boolean? = null
) {
    var isImageLoading by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier.size(100.dp, 150.dp),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
            onSuccess = { isImageLoading = false },
            onError = { isImageLoading = false }
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
                val tint = if (isAiring) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer

                Icon(
                    imageVector = icon,
                    contentDescription = if (isAiring) "Airing" else "Finished",
                    tint = tint,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(24.dp)
                )
            }
        }
    }
}