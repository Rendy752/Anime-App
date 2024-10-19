package com.example.animeapp.ui.viewmodels

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.webkit.WebView

class YoutubePlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    init {
        settings.javaScriptEnabled = true
    }

    fun playVideo(embedUrl: String) {
        val videoId = extractVideoId(embedUrl)
        if (!videoId.isNullOrEmpty()) {
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { margin: 0; }
                        iframe { display: block; width: 100%; height: 100%; border: none; }
                    </style>
                </head>
                <body>
                    <iframe src="https://www.youtube.com/embed/$videoId?enablejsapi=1" frameborder="0" allowfullscreen></iframe>
                </body>
                </html>
            """.trimIndent()

            loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        } else {
            Log.e("YoutubePlayerView", "Invalid video URL")
        }
    }

    private fun extractVideoId(embedUrl: String): String? {
        val pattern = "(?<=embed/)([a-zA-Z0-9_-]+)".toRegex()
        val match = pattern.find(embedUrl)
        return match?.value
    }
}