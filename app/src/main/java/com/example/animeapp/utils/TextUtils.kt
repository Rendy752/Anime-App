package com.example.animeapp.utils

import org.apache.commons.text.StringEscapeUtils
import java.text.NumberFormat
import java.util.Locale

object TextUtils {
    fun formatSynopsis(synopsis: String): String {
        return StringEscapeUtils.unescapeJava(synopsis)
            .replace(Regex("[\r\n]+"), ". ")
            .replace("[^a-zA-Z0-9\\s.,?']".toRegex(), "")
    }

    fun formatNumber(number: Int): String {
        val numberFormat = NumberFormat.getInstance(Locale.getDefault())
        return numberFormat.format(number).replace(",", ".")
    }
}