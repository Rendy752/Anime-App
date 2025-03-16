package com.example.animeapp.ui.common_ui

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import com.example.animeapp.models.NameAndUrl
import androidx.core.net.toUri

@Composable
fun NameAndUrlList(items: List<NameAndUrl>) {
    val context = LocalContext.current
    Column {
        items.forEach { item ->
            UnorderedListItem(
                text = item.name,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, item.url.toUri())
                    context.startActivity(intent)
                },
                textColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}