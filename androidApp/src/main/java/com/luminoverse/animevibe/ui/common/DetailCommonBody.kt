package com.luminoverse.animevibe.ui.common


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import com.luminoverse.animevibe.utils.basicContainer

@Composable
fun DetailCommonBody(modifier: Modifier = Modifier, title: String, body: String?) {
    if (body != null && body.isNotBlank()) {
        Column(
            modifier = modifier
                .basicContainer(outerPadding = PaddingValues(0.dp))
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Justify
            )
        }
    }
}

@Preview
@Composable
fun DetailCommonBodySkeleton(
    modifier: Modifier = Modifier,
    title: String? = "Title"
) {
    Column(
        modifier = modifier
            .basicContainer(outerPadding = PaddingValues(0.dp))
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title ?: "Title",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            repeat(4) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    height = 16.dp
                )
            }
        }
    }
}