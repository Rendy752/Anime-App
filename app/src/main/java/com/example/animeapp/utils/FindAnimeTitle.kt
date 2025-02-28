package com.example.animeapp.utils

import com.example.animeapp.models.AnimeAniwatch
import com.example.animeapp.models.AnimeAniwatchSearchResponse
import com.example.animeapp.models.AnimeDetail

object FindAnimeTitle {
    fun findClosestAnime(
        animeSearchData: AnimeAniwatchSearchResponse,
        animeDetail: AnimeDetail?
    ): AnimeAniwatch? {
        val animeDetailData = animeDetail
            ?: return animeSearchData.animes.minByOrNull { it.name.normalizeForComparison() }

        val normalizedDetailTitle = animeDetailData.title.normalizeForComparison()
        val normalizedDetailEnglishTitle = animeDetailData.title_english?.normalizeForComparison()
        val normalizedDetailJapaneseTitle = animeDetailData.title_japanese?.normalizeForComparison()
        val normalizedDetailSynonyms =
            animeDetailData.title_synonyms?.map { it.normalizeForComparison() } ?: emptyList()
        val normalizedOtherTitles = animeDetailData.titles.map { it.title.normalizeForComparison() }

        return animeSearchData.animes.minByOrNull { anime ->
            val normalizedAnimeName = anime.name.normalizeForComparison()

            val detailTitleScore =
                if (normalizedAnimeName.contains(normalizedDetailTitle, ignoreCase = true)) {
                    if (normalizedAnimeName.equals(
                            normalizedDetailTitle,
                            ignoreCase = true
                        )
                    ) 0 else 1
                } else {
                    Int.MAX_VALUE
                }

            val englishTitleScore = normalizedDetailEnglishTitle?.let { et ->
                if (normalizedAnimeName.contains(et, ignoreCase = true)) {
                    if (normalizedAnimeName.equals(et, ignoreCase = true)) 0 else 2
                } else {
                    Int.MAX_VALUE
                }
            } ?: Int.MAX_VALUE

            val japaneseTitleScore = normalizedDetailJapaneseTitle?.let { jt ->
                if (normalizedAnimeName.contains(jt, ignoreCase = true)) {
                    if (normalizedAnimeName.equals(jt, ignoreCase = true)) 0 else 3
                } else {
                    Int.MAX_VALUE
                }
            } ?: Int.MAX_VALUE

            val synonymScore = normalizedDetailSynonyms.minOfOrNull { synonym ->
                if (normalizedAnimeName.contains(synonym, ignoreCase = true)) {
                    if (normalizedAnimeName.equals(synonym, ignoreCase = true)) 0 else 4
                } else {
                    Int.MAX_VALUE
                }
            } ?: Int.MAX_VALUE

            val otherTitlesScore = normalizedOtherTitles.minOfOrNull { otherTitle ->
                if (normalizedAnimeName.contains(otherTitle, ignoreCase = true)) {
                    if (normalizedAnimeName.equals(otherTitle, ignoreCase = true)) 0 else 5
                } else {
                    Int.MAX_VALUE
                }
            } ?: Int.MAX_VALUE

            minOf(
                detailTitleScore,
                englishTitleScore,
                japaneseTitleScore,
                synonymScore,
                otherTitlesScore
            )

        }?.takeIf { anime ->
            val normalizedAnimeName = anime.name.normalizeForComparison()
            normalizedAnimeName.contains(normalizedDetailTitle, ignoreCase = true) ||
                    normalizedDetailEnglishTitle?.let {
                        normalizedAnimeName.contains(
                            it,
                            ignoreCase = true
                        )
                    } == true ||
                    normalizedDetailJapaneseTitle?.let {
                        normalizedAnimeName.contains(
                            it,
                            ignoreCase = true
                        )
                    } == true ||
                    normalizedDetailSynonyms.any {
                        normalizedAnimeName.contains(
                            it,
                            ignoreCase = true
                        )
                    } ||
                    normalizedOtherTitles.any {
                        normalizedAnimeName.contains(
                            it,
                            ignoreCase = true
                        )
                    }
        }
    }

    private fun String.normalizeForComparison(): String {
        return this.replace(Regex("[^a-zA-Z0-9\\s]"), "").trim()
    }
}