package com.example.animeapp.utils

import com.example.animeapp.models.AnimeAniwatch
import com.example.animeapp.models.AnimeAniwatchSearchResponse
import com.example.animeapp.models.AnimeDetail
import org.apache.commons.text.similarity.LevenshteinDistance

object FindAnimeTitle {
    private val levenshteinDistance = LevenshteinDistance.getDefaultInstance()
    private const val MIN_SIMILARITY_THRESHOLD = 0.6

    private val seasonRegex = Regex(
        "Season\\s*(\\d+)|(\\d+)(?:st|nd|rd|th)?\\s*Season|(\\w+)\\s*(\\d+)|Movie\\s*(\\d+)",
        RegexOption.IGNORE_CASE
    )

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
            println("Comparing '${anime.name}' with '${animeDetail.title}'")
            val bestScore = allNormalizedTitles.maxOf { detailTitle ->
                println("Comparing normalized name '${normalizedAnimeName}' with normalized title '${detailTitle}'.")
                calculateSimilarityWithNumber(normalizedAnimeName, detailTitle)
            }
            println("Best score for '${anime.name}': $bestScore")
            anime to bestScore
        }

        val bestMatch = scoredMatches.maxByOrNull { it.second }

        println("Best match: ${bestMatch?.first?.name}, Score: ${bestMatch?.second}")

        return if (bestMatch != null && bestMatch.second >= MIN_SIMILARITY_THRESHOLD) bestMatch.first else null
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        val cleanedS1 = cleanString(s1)
        val cleanedS2 = cleanString(s2)

        val processedS1 = addDefaultSeasonIfNeeded(cleanedS1)
        val processedS2 = addDefaultSeasonIfNeeded(cleanedS2)

        val distance = levenshteinDistance.apply(processedS1, processedS2)
        val maxLength = maxOf(processedS1.length, processedS2.length).toDouble()
        return 1.0 - (distance / maxLength)
    }

    private fun addDefaultSeasonIfNeeded(s: String): String {
        val seasonMovieRegex = Regex("(Season|Movie)(?!\\s*\\d+)", RegexOption.IGNORE_CASE)
        return if (seasonMovieRegex.containsMatchIn(s)) {
            seasonMovieRegex.replace(s, "$1 1")
        } else {
            s
        }
    }

    private fun calculateSimilarityWithNumber(s1: String, s2: String): Double {
        val number1 = extractNumber(s1)
        val number2 = extractNumber(s2)

        if (number1 != null && number2 != null) {
            if (number1 == number2) {
                println("Numbers match: $number1 vs $number2. Boosting score.")
                return calculateSimilarity(s1, s2) * 1.2
            } else {
                println("Numbers don't match: $number1 vs $number2. Returning 0.")
                return 0.0
            }
        }

        return calculateSimilarity(s1, s2)
    }

    private fun cleanString(s: String): String {
        return seasonRegex.replace(s, "").trim().lowercase()
    }

    private fun extractNumber(title: String): Int? {
        seasonRegex.find(title)?.let { matchResult ->
            for (group in matchResult.groupValues.drop(1)) {
                if (group.isNotEmpty()) {
                    group.toIntOrNull()?.let { return it }

                    val number = group.replace(Regex("[^0-9]"), "").toIntOrNull()
                    if (number != null) {
                        return number
                    }
                }
            }
        }

        return null
    }

    private fun String.normalizeForComparison(): String {
        var normalized = this.replace(Regex("[^a-zA-Z0-9\\s]"), "").trim().lowercase()
        normalized = addDefaultSeasonIfNeeded(normalized)
        return normalized
    }
}