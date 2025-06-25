package com.luminoverse.animevibe.utils

import org.apache.commons.text.StringEscapeUtils
import java.text.NumberFormat
import java.util.Locale

object TextUtils {
    fun String.formatSynopsis(): String {
        return StringEscapeUtils.unescapeJava(this)
            .replace(Regex("[\r\n]+"), ". ")
            .replace("[^a-zA-Z0-9\\s.,?']".toRegex(), "")
    }

    fun Int.formatNumber(): String {
        val numberFormat = NumberFormat.getInstance(Locale.getDefault())
        return numberFormat.format(this).replace(",", ".")
    }

    fun String.toTitleCase(): String {
        return this.replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }
}