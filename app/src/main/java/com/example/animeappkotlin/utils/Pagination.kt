package com.example.animeappkotlin.utils

import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.example.animeappkotlin.R
import com.example.animeappkotlin.models.CompletePagination

object Pagination {

    fun setPaginationButtons(
        container: LinearLayout,
        pagination: CompletePagination?,
        onPaginationClick: (Int) -> Unit
    ) {
        container.removeAllViews()

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

        if (lastPage <= 4) {
            for (i in 1..lastPage) {
                addButton(inflater, container, "$i", onPaginationClick, i, currentPage == i)
            }
        } else {
            addButton(inflater, container, "1", onPaginationClick, 1, currentPage == 1)
            addButton(inflater, container, "2", onPaginationClick, 2, currentPage == 2)

            if (currentPage > 3) {
                addDots(inflater, container)
            }

            if (currentPage > 2 && currentPage < lastPage - 1) {
                addButton(inflater, container, "$currentPage", onPaginationClick, currentPage, true)
            }

            if (currentPage < lastPage - 2) {
                addDots(inflater, container)
            }

            addButton(inflater, container, "${lastPage - 1}", onPaginationClick, lastPage - 1, currentPage == lastPage - 1)
            addButton(inflater, container, "$lastPage", onPaginationClick, lastPage, currentPage == lastPage)
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
        button.setPadding(30, 20, 30, 20)
        if (isCurrentPage) {
            button.isEnabled = false
            button.setBackgroundResource(R.drawable.pagination_button_current_bg)
            button.setTextColor(container.context.getColor(R.color.nightTextColorPrimary))
        } else {
            button.setBackgroundResource(R.drawable.pagination_button_bg)
            button.setTextColor(container.context.getColor(R.color.textColorPrimary))
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