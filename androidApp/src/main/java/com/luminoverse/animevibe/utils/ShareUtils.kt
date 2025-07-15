package com.luminoverse.animevibe.utils

import android.content.Context
import android.content.Intent
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.utils.TextUtils.formatSynopsis

object ShareUtils {
    fun shareAnimeDetail(context: Context, animeDetail: AnimeDetail?) {
        animeDetail?.let { detail ->
            val animeUrl = detail.url
            val animeTitle = detail.title
            val animeScore = detail.score ?: "0"
            val animeGenres = detail.genres?.joinToString(", ") { it.name }

            val animeSynopsis = detail.synopsis?.formatSynopsis()
            val animeTrailerUrl = detail.trailer.url ?: ""
            val malId = detail.mal_id
            val customUrl = "animevibe://anime/detail/$malId"

            val trailerSection = if (animeTrailerUrl.isNotEmpty()) {
                """
                    
            -------
            Trailer
            -------
            $animeTrailerUrl
            """
            } else {
                ""
            }

            val sharedText = """
            Check out this anime on AnimeVibe!

            Title: $animeTitle
            Score: $animeScore
            Genres: $animeGenres

            --------
            Synopsis
            --------
            ${animeSynopsis ?: "No synopsis available."}
            $trailerSection

            Web URL: $animeUrl
            App URL: $customUrl
            Download the app now: https://play.google.com/store/apps/details?id=com.luminoverse.animevibe
        """.trimIndent()

            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, sharedText)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            context.startActivity(shareIntent)
        }
    }

    fun shareText(context: Context, shareText: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Episode")
        context.startActivity(shareIntent)
    }
}