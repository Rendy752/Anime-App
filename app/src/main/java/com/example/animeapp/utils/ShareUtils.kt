package com.example.animeapp.utils

import android.content.Context
import android.content.Intent
import com.example.animeapp.models.AnimeDetail

object ShareUtils {
    fun shareAnimeDetail(context: Context, animeDetail: AnimeDetail?) {
        animeDetail?.let { detail ->
            val animeUrl = detail.url
            val animeTitle = detail.title
            val animeScore = detail.score ?: "0"
            val animeGenres = detail.genres?.joinToString(", ") { it.name }

            val animeSynopsis = TextUtils.formatSynopsis(detail.synopsis ?: "-")
            val animeTrailerUrl = detail.trailer.url ?: ""
            val malId = detail.mal_id
            val customUrl = "animeapp://anime/detail/$malId"

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
            Check out this anime on AnimeApp!

            Title: $animeTitle
            Score: $animeScore
            Genres: $animeGenres

            --------
            Synopsis
            --------
            $animeSynopsis
            $trailerSection

            Web URL: $animeUrl
            App URL: $customUrl
            Download the app now: https://play.google.com/store/apps/details?id=com.example.animeapp
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
}