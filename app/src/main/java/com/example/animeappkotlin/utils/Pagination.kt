package com.example.animeappkotlin.utils

import com.example.animeappkotlin.models.CompletePagination

object Pagination {

    fun getPaginationText(pagination: CompletePagination?): String {
        if (pagination == null) {
            return ""
        }

        val currentPage = pagination.current_page
        val lastPage = pagination.last_visible_page
        val hasNextPage = pagination.has_next_page

        if (lastPage == 1) {
            return "1"
        }

        return buildString {
            if (currentPage > 1) {
                append("< ")
            }

            if (lastPage == 2) {
                append("1 2 ")
            } else {
                for (i in 1..minOf(2, lastPage)) {
                    if (i == currentPage) {
                        append("[$i] ")
                    } else {
                        append("$i ")
                    }
                }
                if (lastPage > 3 && currentPage > 3) {
                    append("... ")
                }
                if (currentPage > 2 && currentPage < lastPage - 1) {
                    append("[$currentPage] ")
                }
                if (lastPage > 4 && currentPage < lastPage - 2) {
                    append("... ")
                }
                for (i in maxOf(1, lastPage - 1)..lastPage) {
                    if (i == currentPage) {
                        append("[$i] ")
                    } else {
                        append("$i ")
                    }
                }
            }

            if (hasNextPage) {
                append(">")
            }
        }
    }
}