package com.example.animeapp.models

data class ProducersSearchQueryState(
    val query: String = "",
    val page: Int = 1,
    val limit: Int? = 25,

    val orderBy: String? = null,
    val sort: String? = null,
    val letter: String? = null,
) {
    private fun defaultLimitAndPage(): ProducersSearchQueryState {
        return copy(page = 1, limit = 25)
    }

    fun resetProducers(): ProducersSearchQueryState {
        return defaultLimitAndPage().copy(
            query = "",
            orderBy = null,
            sort = null,
        )
    }
}