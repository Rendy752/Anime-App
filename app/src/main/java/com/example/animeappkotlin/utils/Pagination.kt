package com.example.animeappkotlin.utils

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import com.example.animeappkotlin.R
import com.example.animeappkotlin.models.CompletePagination

object Pagination {

    fun setPaginationButtons(
        container: LinearLayout,
        pagination: CompletePagination?,
        onPaginationClick: (Int) -> Unit
    ) {
        container.removeAllViews() // Clear previous buttons

        if (pagination == null) {
            return
        }

        val currentPage = pagination.current_page
        val lastPage = pagination.last_visible_page
        val hasNextPage = pagination.has_next_page

        val inflater = LayoutInflater.from(container.context)

        if (currentPage > 1) {
            addButton(inflater, container, "<", onPaginationClick, currentPage - 1)
        }

        if (lastPage == 2) {
            addButton(inflater, container, "1", onPaginationClick, 1, currentPage == 1)
            addButton(inflater, container, "2", onPaginationClick, 2, currentPage == 2)
        } else {
            for (i in 1..minOf(2, lastPage)) {
                addButton(inflater, container, "$i", onPaginationClick, i, currentPage == i)
            }
            if (lastPage > 3 && currentPage > 3) {
                addDots(inflater, container)
            }
            if (currentPage > 2 && currentPage < lastPage - 1) {
                addButton(inflater, container, "$currentPage", onPaginationClick, currentPage, true)
            }
            if (lastPage > 4 && currentPage < lastPage - 2) {
                addDots(inflater, container)
            }
            for (i in maxOf(1, lastPage - 1)..lastPage) {
                addButton(inflater, container, "$i", onPaginationClick, i, currentPage == i)
            }
        }

        if (hasNextPage) {
            addButton(inflater, container, ">", onPaginationClick, currentPage + 1)
        }
    }

    private fun addButton(
        inflater: LayoutInflater,
        container: LinearLayout,
        text: String,
        onPaginationClick: (Int) -> Unit,
        pageNumber: Int,
        isCurrentPage: Boolean = false
    ) {
        val button = inflater.inflate(R.layout.pagination_button, container, false) as TextView
        button.text = text
        if (isCurrentPage) {
            button.setBackgroundResource(R.drawable.pagination_button_current_bg) // Example background for current page
        }
        button.setOnClickListener {
            onPaginationClick(pageNumber)
        }
        container.addView(button)
    }

    private fun addDots(inflater: LayoutInflater, container: LinearLayout) {
        val dots = inflater.inflate(R.layout.pagination_dots, container, false)
        container.addView(dots)
    }
}