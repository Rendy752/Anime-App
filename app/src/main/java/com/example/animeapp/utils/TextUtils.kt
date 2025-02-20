package com.example.animeapp.utils

import org.apache.commons.text.StringEscapeUtils

object TextUtils {
    fun formatSynopsis(synopsis: String): String {
        return StringEscapeUtils.unescapeJava(synopsis)
            .replace(Regex("[\r\n]+"), ". ")
            .replace("[^a-zA-Z0-9\\s.,?']".toRegex(), "")
    }

    fun <T> joinOrNA(list: Collection<T>?, transform: (T) -> String): String {
        return list?.let {
            if (it.isNotEmpty()) {
                it.joinToString(", ", transform = transform)
            } else {
                "-"
            }
        } ?: "-"
    }
}