package com.example.animeapp.ui.common_ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.webkit.WebView
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.animeapp.utils.basicContainer

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubePreview(embedUrl: String?) {
    if (!embedUrl.isNullOrBlank()) {
        Column(
            modifier = Modifier
                .basicContainer(outerPadding = PaddingValues(0.dp))
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                val videoId = extractVideoId(embedUrl)
                if (!videoId.isNullOrEmpty()) {
                    val youtubeUrl = "https://www.youtube.com/embed/$videoId"
                    AndroidView(
                        factory = {
                            WebView(it).apply {
                                settings.javaScriptEnabled = true
                                val html = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <style>
                                            body { margin: 0; }
                                            iframe { display: block; width: 100%; height: calc(100vw / 1.7777); border: none; }
                                        </style>
                                    </head>
                                    <body>
                                        <iframe src="$youtubeUrl?enablejsapi=1" frameborder="0" allowfullscreen></iframe>
                                    </body>
                                    </html>
                                """.trimIndent()
                                loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun extractVideoId(embedUrl: String): String? {
    val pattern = "(?<=embed/)([a-zA-Z0-9_-]+)".toRegex()
    val match = pattern.find(embedUrl)
    return match?.value
}

@Preview
@Composable
fun YoutubePreviewSkeleton() {
    Column(
        modifier = Modifier
            .basicContainer(outerPadding = PaddingValues(0.dp))
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 200.dp)
        }
    }
}