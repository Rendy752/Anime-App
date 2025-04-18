package com.example.animeapp.models

import java.time.DayOfWeek
import java.time.LocalDate

data class AnimeSchedulesSearchQueryState(
    val filter: String? = getCurrentDayOfWeek(),
    val kids: Boolean? = false,
    val sfw: Boolean? = true,
    val unapproved: Boolean? = null,

    val page: Int = 1,
    val limit: Int? = 25,
) {
    private fun defaultLimitAndPage(): AnimeSchedulesSearchQueryState {
        return copy(page = 1, limit = 25)
    }

    fun resetQueryState(): AnimeSchedulesSearchQueryState {
        return defaultLimitAndPage().copy(
            filter = getCurrentDayOfWeek(),
            sfw = true,
            kids = false,
            unapproved = null,
        )
    }

    companion object {
        fun getCurrentDayOfWeek(): String {
            val currentDay = LocalDate.now().dayOfWeek
            return when (currentDay) {
                DayOfWeek.MONDAY -> "monday"
                DayOfWeek.TUESDAY -> "tuesday"
                DayOfWeek.WEDNESDAY -> "wednesday"
                DayOfWeek.THURSDAY -> "thursday"
                DayOfWeek.FRIDAY -> "friday"
                DayOfWeek.SATURDAY -> "saturday"
                DayOfWeek.SUNDAY -> "sunday"
                else -> "unknown"
            }
        }
    }
}