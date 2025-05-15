package com.luminoverse.animevibe.ui.common_ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun highlightText(text: String, query: String): AnnotatedString {
    val annotatedString = AnnotatedString.Builder(text)
    if (query.isNotBlank()) {
        val startIndex = text.indexOf(query, ignoreCase = true)
        if (startIndex != -1) {
            annotatedString.addStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                ),
                start = startIndex,
                end = startIndex + query.length
            )
        }
    }
    return annotatedString.toAnnotatedString()
}