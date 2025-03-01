package com.example.animeapp.utils

import com.example.animeapp.models.AnimeAniwatch
import com.example.animeapp.models.AnimeAniwatchSearchResponse
import com.example.animeapp.models.AnimeDetail

object FindAnimeTitle {
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

        val closestMatches = animeSearchData.animes.mapNotNull { anime ->
            val normalizedAnimeName = anime.name.normalizeForComparison()
            val matchScore = allNormalizedTitles.minOfOrNull { title ->
                levenshteinDistance(normalizedAnimeName, title)
            } ?: Int.MAX_VALUE

            if (matchScore <= 5) anime to matchScore else null
        }.sortedBy { it.second }

        return closestMatches.firstOrNull()?.first
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) {
            dp[i][0] = i
        }
        for (j in 0..n) {
            dp[0][j] = j
        }

        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }

        return dp[m][n]
    }

    private fun String.normalizeForComparison(): String {
        return this.replace(Regex("[^a-zA-Z0-9\\s]"), "").trim()
    }
}