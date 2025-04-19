package com.example.animeapp.models

import com.example.animeapp.utils.TimeUtils.getCurrentDayOfWeek

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
}