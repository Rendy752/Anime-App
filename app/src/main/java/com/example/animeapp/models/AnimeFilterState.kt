package com.example.animeapp.models

data class AnimeFilterState(
    val type: String? = null,
    val score: Double? = null,
    val minScore: Double? = null,
    val maxScore: Double? = null,
    val status: String? = null,
    val rating: String? = null,
    val sfw: Boolean? = null,
    val unapproved: Boolean? = null,
    val genres: String? = null,
    val genresExclude: String? = null,
    val orderBy: String? = null,
    val sort: String? = null,
    val letter: String? = null,
    val producers: String? = null,
    val startDate: String? = null,
    val endDate: String? = null
)