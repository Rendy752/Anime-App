package com.example.animeapp.utils

import com.example.animeapp.models.AnimeAniwatch
import com.example.animeapp.models.AnimeAniwatchSearchResponse
import com.example.animeapp.models.AnimeDetail
import org.apache.commons.text.similarity.LevenshteinDistance

object FindAnimeTitle {
    private val levenshteinDistance = LevenshteinDistance.getDefaultInstance()
    private const val MIN_SIMILARITY_THRESHOLD = 0.6

    fun findClosestAnime(
        animeSearchData: AnimeAniwatchSearchResponse,
        animeDetail: AnimeDetail?
    ): AnimeAniwatch? {
        if (animeDetail == null) {
            return animeSearchData.animes.minByOrNull { it.name.normalizeForComparison() }
        }

        val normalizedDetailTitle = animeDetail.title.normalizeForComparison()
        val normalizedDetailEnglishTitle = animeDetail.title_english?.normalizeForComparison()
        val normalizedDetailSynonyms =
            animeDetail.title_synonyms?.map { it.normalizeForComparison() } ?: emptyList()

        val allNormalizedTitles = listOfNotNull(
            normalizedDetailTitle,
            normalizedDetailEnglishTitle,
            *normalizedDetailSynonyms.toTypedArray()
        )

        val scoredMatches = animeSearchData.animes.map { anime ->
            val normalizedAnimeName = anime.name.normalizeForComparison()
            val bestScore = allNormalizedTitles.maxOf { detailTitle ->
                calculateSimilarity(normalizedAnimeName, detailTitle)
            }
            anime to bestScore
        }

        val bestMatch = scoredMatches.maxByOrNull { it.second }

        return if (bestMatch != null && bestMatch.second >= MIN_SIMILARITY_THRESHOLD) bestMatch.first else null
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        val distance = levenshteinDistance.apply(s1, s2)
        val maxLength = maxOf(s1.length, s2.length).toDouble()
        return 1.0 - (distance / maxLength)
    }

    private fun String.normalizeForComparison(): String {
        return this.replace(Regex("[^a-zA-Z0-9\\s]"), "").trim().lowercase()
    }
}