package com.luminoverse.animevibe.utils.watch

import android.util.Log
import org.apache.commons.text.similarity.LevenshteinDistance
import java.util.regex.Pattern
import kotlin.math.min

object AnimeTitleFinder {

    private val levenshteinDistance = LevenshteinDistance.getDefaultInstance()
    private const val MIN_SIMILARITY_THRESHOLD = 0.25
    private val titleRegex = Pattern.compile(
        "^(.*?)(?:\\s+(?:(?:Season|Movie)\\s*(\\d+)|(\\d+)(?:st|nd|rd|th)?\\s*(?:Season)?|(\\d+)))?\\s*(?:\\(?(Part\\s*\\d+)\\)?)?\\s*$",
        Pattern.CASE_INSENSITIVE
    )
    private val numberRegex = Pattern.compile("\\d+")

    fun <T> findClosestMatches(
        targetTitles: List<String>,
        data: List<T>,
        maxResults: Int,
        titleExtractor: (T) -> String
    ): List<T> {
        if (data.isEmpty() || targetTitles.isEmpty()) {
            return emptyList()
        }

        val normalizedTargetTitles = targetTitles.map { it.normalizeTitle() }

        val scoredItems = data.map { item ->
            val itemName = titleExtractor(item)
            val normalizedItemName = itemName.normalizeTitle()
            Log.d("AnimeTitleFinder", "Comparing '$itemName' with '$targetTitles'")

            val bestScore = normalizedTargetTitles.maxOf { targetTitle ->
                calculateEnhancedSimilarity(normalizedItemName, targetTitle)
            }

            Log.d("AnimeTitleFinder", "Best score for '$itemName': $bestScore")
            ScoredItem(item, bestScore)
        }

        val sortedAndFiltered = scoredItems.sortedByDescending { it.score }
            .filter { it.score >= MIN_SIMILARITY_THRESHOLD }

        val topResults = sortedAndFiltered.take(min(maxResults, sortedAndFiltered.size))
        val firstTwoFromData = data.take(2)

        return (topResults.map { it.item } + firstTwoFromData)
            .distinct()
            .take(maxResults + 2)
    }

    private fun extractCoreTitleAndNumber(title: String): Pair<String, Pair<Int?, String?>> {
        val modifiedTitle = title.trim()
        val matcher = titleRegex.matcher(modifiedTitle)
        var seasonNumber: Int? = null
        var coreTitle = modifiedTitle
        var part: String? = null

        if (matcher.find()) {
            coreTitle = matcher.group(1)?.trim() ?: ""
            seasonNumber = (2..4).firstNotNullOfOrNull {
                matcher.group(it)?.toIntOrNull()
            }
            part = matcher.group(5)?.trim()
        } else {
            val numberMatcher = numberRegex.matcher(modifiedTitle)
            if (numberMatcher.find()) {
                seasonNumber = numberMatcher.group().toIntOrNull()
                coreTitle = modifiedTitle.replace(numberMatcher.group(), "").trim()
            }
        }

        if (seasonNumber == null) {
            val numbers =
                numberRegex.toRegex().findAll(modifiedTitle).map { it.value.toInt() }.toList()
            seasonNumber = if (numbers.isNotEmpty() && numbers.last() in 1900..2100) {
                null
            } else {
                numbers.lastOrNull()?.also {
                    coreTitle = modifiedTitle.replace(it.toString(), "").trim()
                }
            }
            if (seasonNumber == null && coreTitle.lowercase().contains("movie")) {
                seasonNumber = 1
            }
        }

        return coreTitle to (seasonNumber to part)
    }

    private fun calculateEnhancedSimilarity(s1: String, s2: String): Double {
        val (coreTitle1, numberPair1) = extractCoreTitleAndNumber(s1)
        val (coreTitle2, numberPair2) = extractCoreTitleAndNumber(s2)
        val (number1, part1) = numberPair1
        val (number2, part2) = numberPair2

        Log.d(
            "AnimeTitleFinder",
            "Core titles: '$coreTitle1' <=> '$coreTitle2', Numbers: $number1 <=> $number2, Parts: $part1 <=> $part2"
        )

        val coreSimilarity = calculateCombinedSimilarity(coreTitle1, coreTitle2)

        var score = coreSimilarity * 0.6
        var numberScore = 0.0
        var partScore = 0.0

        if (number1 != null && number2 != null && number1 == number2) {
            numberScore = 0.2
            if (part1 != null && part2 != null && part1 == part2) {
                partScore = 0.2
            } else if (part1 != null || part2 != null) {
                score *= 0.95
            }
            if (coreSimilarity < MIN_SIMILARITY_THRESHOLD) {
                numberScore *= 0.5
                partScore *= 0.5
            }
        } else if (number1 != null || number2 != null) {
            score *= 0.9
        }

        return (score + numberScore + partScore).coerceAtMost(1.0)
    }

    private fun calculateCombinedSimilarity(s1: String, s2: String): Double =
        (calculateLevenshteinSimilarity(s1, s2) * 0.7 + calculateJaccardSimilarity(s1, s2) * 0.3)
            .coerceAtMost(1.0)

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

    private data class ScoredItem<T>(val item: T, val score: Double)

    fun String.normalizeTitle(): String =
        replace(Regex("[^a-zA-Z0-9\\s]"), "").trim().lowercase()

    /**
     * Filters a list of items based on a search query, using extractors to focus on specific attributes.
     * Supports case-insensitive matching, fuzzy search for typing mistakes, and core title matching.
     * Falls back to simple contains check if no fuzzy matches are found.
     *
     * @param searchQuery The user's search input.
     * @param items The list of items to search through.
     * @param extractors List of functions to extract string attributes from items.
     * @return Filtered list of items matching the query.
     */
    fun <T> searchTitle(
        searchQuery: String,
        items: List<T>,
        extractors: List<(T) -> String>
    ): List<T> {
        if (searchQuery.isBlank()) {
            Log.d("AnimeTitleFinder", "Search query is empty, returning full list")
            return items
        }

        if (extractors.isEmpty()) {
            Log.w("AnimeTitleFinder", "No extractors provided, returning empty list")
            return emptyList()
        }

        val normalizedQuery = searchQuery.trim().lowercase()
        val effectiveThreshold = when (normalizedQuery.length) {
            in 0..4 -> 1
            in 5..7 -> 2
            else -> 3
        }

        val fuzzyMatches = items.filter { item ->
            extractors.any { extractor ->
                val attribute = extractor(item).lowercase()
                if (attribute.isEmpty()) {
                    Log.w("AnimeTitleFinder", "Empty attribute for item: $item")
                    return@any false
                }

                val (coreTitle, _) = extractCoreTitleAndNumber(attribute)
                val normalizedCoreTitle = coreTitle.normalizeTitle()

                normalizedCoreTitle.contains(normalizedQuery) ||
                        levenshteinDistance.apply(normalizedCoreTitle, normalizedQuery) <= effectiveThreshold ||
                        (normalizedQuery.length <= 7 && levenshteinDistance.apply(attribute, normalizedQuery) <= effectiveThreshold) ||
                        normalizedCoreTitle.split(" ").any { word ->
                            levenshteinDistance.apply(word, normalizedQuery) <= effectiveThreshold
                        }
            }
        }

        return if (fuzzyMatches.isEmpty()) {
            Log.d("AnimeTitleFinder", "No fuzzy matches for '$searchQuery', applying fallback contains check")
            items.filter { item ->
                extractors.any { extractor ->
                    val attribute = extractor(item).lowercase()
                    if (attribute.isEmpty()) {
                        Log.w("AnimeTitleFinder", "Empty attribute for item: $item")
                        return@any false
                    }
                    if (attribute.contains(normalizedQuery)) {
                        Log.d("AnimeTitleFinder", "Fallback match for '$normalizedQuery' in '$attribute'")
                        true
                    } else {
                        false
                    }
                }
            }
        } else {
            fuzzyMatches
        }.also {
            Log.d(
                "AnimeTitleFinder",
                "Filtered ${it.size} items for query: $searchQuery, threshold: $effectiveThreshold, fallback: ${fuzzyMatches.isEmpty()}"
            )
        }
    }
}