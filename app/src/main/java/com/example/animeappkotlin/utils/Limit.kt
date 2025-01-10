package com.example.animeappkotlin.utils

object Limit {

    val limitOptions = arrayOf("5", "10", "15", "20", "25")

    fun getLimitValue(position: Int): Int? {
        return when (position) {
            0 -> null
            else -> limitOptions[position].toInt()
        }
    }

    const val DEFAULT_LIMIT = 10
}