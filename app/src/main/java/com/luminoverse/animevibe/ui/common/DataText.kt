package com.luminoverse.animevibe.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun DataTextWithIcon(modifier: Modifier = Modifier, label: String? = null, value: String?, icon: ImageVector) {
    if (!value.isNullOrBlank() && value.lowercase() != "null") {
        Row(
            modifier = modifier.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label ?: "Icon",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 4.dp)
            )

            val fullText = AnnotatedString.Builder().apply {
                if (!label.isNullOrBlank()) {
                    append("$label: ")
                }
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append(value) }
            }.toAnnotatedString()

            Text(
                text = fullText,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview
@Composable
fun DataTextWithIconSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonBox(
            modifier = Modifier.size(20.dp),
            width = 20.dp,
            height = 20.dp
        )
        Spacer(modifier = Modifier.padding(end = 4.dp))
        SkeletonBox(
            modifier = Modifier.weight(0.3f),
            height = 16.dp
        )
    }
}

data class ClickableItem(val text: String, val onClick: () -> Unit)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClickableDataTextWithIcon(
    label: String,
    items: List<ClickableItem>?,
    icon: ImageVector
) {
    if (!items.isNullOrEmpty()) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LabelRow(label, icon)
            ClickableItems(items)
        }
    }
}

@Composable
private fun LabelRow(label: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(20.dp)
                .padding(end = 4.dp)
        )
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ClickableItems(items: List<ClickableItem>) {
    items.forEachIndexed { index, item ->
        val annotatedString = createAnnotatedString(item.text, index < items.size - 1)
        Text(
            style = MaterialTheme.typography.bodyMedium,
            text = annotatedString,
            modifier = Modifier.clickable { item.onClick() },
        )
    }
}

@Composable
private fun createAnnotatedString(text: String, appendComma: Boolean): AnnotatedString {
    return if (appendComma) {
        AnnotatedString.Builder().apply {
            pushStyle(clickableTextStyle())
            append(text)
            pop()
            append(", ")
        }.toAnnotatedString()
    } else {
        AnnotatedString.Builder().apply {
            pushStyle(clickableTextStyle())
            append(text)
        }.toAnnotatedString()
    }
}

@Composable
private fun clickableTextStyle(): SpanStyle {
    return SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        textDecoration = TextDecoration.Underline,
    )
}