package com.example.animeapp.utils

object Limit {

    val limitOptions = arrayOf(5, 10, 15, 20, 25)

    fun getLimitValue(position: Int): Int {
        return if (position in limitOptions.indices) {
            limitOptions[position]
        } else {
            DEFAULT_LIMIT
        }
    }

    const val DEFAULT_LIMIT = 10
}