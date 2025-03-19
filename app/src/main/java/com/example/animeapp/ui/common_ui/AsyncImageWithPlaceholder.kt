package com.example.animeapp.ui.common_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsyncImageWithPlaceholder(
    model: Any?,
    modifier: Modifier = Modifier,
    contentDescription: String?,
    isAiring: Boolean? = null,
) {
    var isImageLoading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(100.dp, 150.dp)
            .clickable {
                if (model != null) {
                    showDialog = true
                }
            },
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

    if (showDialog && model != null) {
        BasicAlertDialog(
            onDismissRequest = { showDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        .clickable(onClick = { showDialog = false }),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center){
                            AsyncImage(
                                model = model,
                                contentDescription = contentDescription,
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentScale = ContentScale.Fit,
                                onSuccess = { isImageLoading = false },
                                onError = { isImageLoading = false }
                            )
                        }

                    }
                }
            }
        )
    }
}