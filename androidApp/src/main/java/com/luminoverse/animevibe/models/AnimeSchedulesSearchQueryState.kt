package com.luminoverse.animevibe.models

import com.luminoverse.animevibe.utils.TimeUtils.getCurrentDayOfWeek

data class AnimeSchedulesSearchQueryState(
    val filter: String? = getCurrentDayOfWeek(),
    val kids: Boolean? = false,
    val sfw: Boolean? = true,
    val unapproved: Boolean? = null,

    val page: Int = 1,
    val limit: Int? = 25,
)