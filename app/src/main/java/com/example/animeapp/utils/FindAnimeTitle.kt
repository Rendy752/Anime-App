package com.example.animeapp.utils

import android.util.Log
import com.example.animeapp.models.AnimeAniwatch
import com.example.animeapp.models.AnimeAniwatchSearchResponse
import com.example.animeapp.models.AnimeDetail
import org.apache.commons.text.similarity.LevenshteinDistance
import java.util.regex.Pattern

object FindAnimeTitle {

    private val levenshteinDistance = LevenshteinDistance.getDefaultInstance()
    private const val MIN_SIMILARITY_THRESHOLD = 0.3
    private val titleRegex = Pattern.compile(
        "^(.*?)(?:\\s+(?:(?:Season|Movie)\\s*(\\d+)|(\\d+)(?:st|nd|rd|th)?\\s*(?:Season)?|(\\d+)))?\\s*$",
        Pattern.CASE_INSENSITIVE
    )
    private val numberRegex = Pattern.compile("\\d+")

    fun findClosestAnime(animeSearchData: AnimeAniwatchSearchResponse, animeDetail: AnimeDetail?): AnimeAniwatch? {
        if (animeDetail == null) {
            return animeSearchData.animes.minByOrNull { it.name.normalizeForComparison() }
        }

        val normalizedDetailTitle = animeDetail.title.normalizeForComparison()
        val normalizedDetailEnglishTitle = animeDetail.title_english?.normalizeForComparison()
        val normalizedDetailSynonyms = animeDetail.title_synonyms?.map { it.normalizeForComparison() } ?: emptyList()

        val allNormalizedTitles = listOfNotNull(normalizedDetailTitle, normalizedDetailEnglishTitle) + normalizedDetailSynonyms

        return animeSearchData.animes.map { anime ->
            val normalizedAnimeName = anime.name.normalizeForComparison()
            Log.d("FindAnimeTitle", "Comparing '${anime.name}' with '${animeDetail.title}'")

            val bestScore = allNormalizedTitles.maxOf { detailTitle ->
                calculateEnhancedSimilarity(normalizedAnimeName, detailTitle)
            }

            Log.d("FindAnimeTitle", "Best score for '${anime.name}': $bestScore")
            ScoredAnime(anime, bestScore)
        }.maxByOrNull { it.score }
            ?.takeIf { it.score >= MIN_SIMILARITY_THRESHOLD }
            ?.anime
    }

    private fun extractCoreTitleAndNumber(title: String): Pair<String, Pair<Int?, String?>> {
        val modifiedTitle = title.trim()
        val matcher = titleRegex.matcher(modifiedTitle)
        var seasonNumber: Int? = null
        var coreTitle = modifiedTitle
        var part: String? = null

        if (matcher.find()) {
            coreTitle = matcher.group(1)?.trim() ?: ""
            seasonNumber = (2..matcher.groupCount()).firstNotNullOfOrNull { index ->
                matcher.group(index)?.toIntOrNull()
            }
            part = modifiedTitle.replace(coreTitle, "").replace(seasonNumber?.toString() ?: "", "").trim()
            if (!part.lowercase().contains("part")) {
                part = null
            }

        } else {
            val numberMatcher = numberRegex.matcher(modifiedTitle)
            if (numberMatcher.find()) {
                seasonNumber = numberMatcher.group().toIntOrNull()
                coreTitle = modifiedTitle.replace(numberMatcher.group(), "").trim()
            }
        }

        if (seasonNumber == null) {
            val numbers = numberRegex.toRegex().findAll(modifiedTitle).map { it.value.toInt() }.toList()
            seasonNumber = numbers.lastOrNull()
            if (seasonNumber != null) {
                coreTitle = modifiedTitle.replace(seasonNumber.toString(), "").trim()
            } else if (coreTitle.lowercase().contains("movie")) {
                seasonNumber = 1 // Treat "Movie" without number as "Movie 1"
            }
        }

        return Pair(coreTitle, Pair(seasonNumber, part))
    }

    private fun calculateEnhancedSimilarity(s1: String, s2: String): Double {
        val (coreTitle1, numberPair1) = extractCoreTitleAndNumber(s1)
        val (coreTitle2, numberPair2) = extractCoreTitleAndNumber(s2)
        val number1 = numberPair1.first
        val number2 = numberPair2.first
        val part1 = numberPair1.second
        val part2 = numberPair2.second

        println("Core titles: '$coreTitle1' <=> '$coreTitle2', Numbers: $number1 <=> $number2, Parts: $part1 <=> $part2")

        val coreSimilarity = calculateCombinedSimilarity(coreTitle1, coreTitle2)

        var score = coreSimilarity * 0.7 // Increased core similarity weight
        var numberScore = 0.0

        if (number1 != null && number2 != null) {
            if (number1 == number2) {
                numberScore = 0.3 // Increased number match weight
                if (part1 != null && part2 != null && part1 == part2){
                    numberScore = 0.3
                } else if (part1 != null || part2 != null){
                    score *= 0.95
                }
            } else {
                score *= 0.8 // slight penalty for different numbers
            }
        } else if (number1 != null || number2 != null) {
            score *= 0.9 // slight penalty for one number missing
        }

        return (score + numberScore).coerceAtMost(1.0)
    }

    private fun calculateCombinedSimilarity(s1: String, s2: String): Double {
        val levenshteinScore = calculateLevenshteinSimilarity(s1, s2)
        val jaccardScore = calculateJaccardSimilarity(s1, s2)

        return (levenshteinScore * 0.7 + jaccardScore * 0.3).coerceAtMost(1.0)
    }

    private fun calculateLevenshteinSimilarity(s1: String, s2: String): Double {
        val distance = levenshteinDistance.apply(s1, s2)
        val maxLength = maxOf(s1.length, s2.length)
        return 1.0 - distance.toDouble() / maxLength
    }

    private fun calculateJaccardSimilarity(s1: String, s2: String): Double {
        val set1 = s1.split("\\s+").toSet()
        val set2 = s2.split("\\s+").toSet()
        val intersection = set1.intersect(set2)
        val union = set1.union(set2)
        return if (union.isNotEmpty()) intersection.size.toDouble() / union.size.toDouble() else 0.0
    }

    private data class ScoredAnime(val anime: AnimeAniwatch, val score: Double)

    private fun String.normalizeForComparison(): String {
        return replace(Regex("[^a-zA-Z0-9\\s]"), "").trim().lowercase()
    }
}