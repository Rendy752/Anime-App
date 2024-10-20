package com.example.animeapp.utils

import org.apache.commons.text.StringEscapeUtils

fun formatSynopsis(synopsis: String): String {
    return StringEscapeUtils.unescapeJava(synopsis)
        .replace(Regex("[\r\n]+"), ". ")
        .replace("[^a-zA-Z0-9\\s.,?']".toRegex(), "")
}