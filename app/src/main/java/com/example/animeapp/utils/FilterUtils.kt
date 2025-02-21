package com.example.animeapp.utils

import com.example.animeapp.databinding.BottomSheetAnimeSearchFilterBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

object FilterUtils {

    val TYPE_OPTIONS =
        listOf("Any", "TV", "Movie", "OVA", "Special", "ONA", "Music", "CM", "PV", "TV Special")
    val STATUS_OPTIONS = listOf("Any", "Airing", "Complete", "Upcoming")
    val RATING_OPTIONS = listOf("Any", "G", "PG", "PG13", "R17", "R", "Rx")
    private val RATING_DESCRIPTIONS = mapOf(
        "G" to "All Ages",
        "PG" to "Children",
        "PG13" to "Teens 13 or older",
        "R17" to "17+ (violence & profanity)",
        "R" to "Mild Nudity",
        "Rx" to "Hentai"
    )
    val ORDER_BY_OPTIONS = listOf(
        "Any", "mal_id", "title", "start_date", "end_date", "episodes", "score",
        "scored_by", "rank", "popularity", "members", "favorites"
    )
    val SORT_OPTIONS = listOf("Any", "desc", "asc")
    val GENRE_OPTIONS = listOf("Action", "Adventure", "Comedy", "Drama", "Fantasy", "Sci-Fi")

    fun collectFilterValues(binding: BottomSheetAnimeSearchFilterBinding): Map<String, Any?> {
        binding.apply {
            val enableDateRange = binding.enableDateRangeCheckBox.isChecked

            val startDate = if (!enableDateRange) {
                null
            } else {
                DateUtils.formatDate(
                    binding.startDatePicker.year,
                    binding.startDatePicker.month,
                    binding.startDatePicker.dayOfMonth
                )
                    .takeIf { it.isNotBlank() }
            }

            val endDate = if (!enableDateRange) {
                null
            } else {
                DateUtils.formatDate(
                    binding.endDatePicker.year,
                    binding.endDatePicker.month,
                    binding.endDatePicker.dayOfMonth
                )
                    .takeIf { it.isNotBlank() }
            }

            return mapOf(
                "type" to typeSpinner.text.toString().takeIf { it != "Any" },
                "score" to scoreEditText.text.toString().toDoubleOrNull(),
                "minScore" to minScoreEditText.text.toString().toDoubleOrNull(),
                "maxScore" to maxScoreEditText.text.toString().toDoubleOrNull(),
                "status" to statusSpinner.text.toString().takeIf { it != "Any" },
                "rating" to RATING_DESCRIPTIONS.entries.firstOrNull {
                    it.value == ratingSpinner.text.toString()
                }?.key?.takeIf { it != "Any" },
                "sfw" to sfwCheckBox.isChecked,
                "unapproved" to unapprovedCheckBox.isChecked,
                "genres" to getChipGroupValues(genresChipGroup),
                "orderBy" to orderBySpinner.text.toString().takeIf { it != "Any" },
                "sort" to sortSpinner.text.toString().takeIf { it != "Any" },
                "producers" to producersEditText.text.toString().takeIf { it.isNotBlank() },
                "startDate" to startDate,
                "endDate" to endDate
            )
        }
    }

    private fun getChipGroupValues(chipGroup: ChipGroup): String? {
        val selectedChipIds = mutableListOf<Int>()
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                selectedChipIds.add(chip.id)
            }
        }
        return if (selectedChipIds.isNotEmpty()) selectedChipIds.joinToString(",") else null
    }

    fun getRatingDescription(ratingCode: String): String {
        return RATING_DESCRIPTIONS[ratingCode] ?: ratingCode
    }
}