package com.example.animeapp.ui.animeDetail.numericDetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animeapp.ui.common_ui.SkeletonBox
import com.example.animeapp.utils.basicContainer

@Composable
fun NumericDetailItem(
    title: String,
    value: String,
    subValue: String? = null,
    icon: ImageVector
) {
    Column(
        modifier = Modifier
            .basicContainer(outerPadding = PaddingValues(0.dp))
            .height(120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        subValue?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview
@Composable
fun NumericDetailItemSkeleton() {
    Column(
        modifier = Modifier
            .basicContainer(outerPadding = PaddingValues(0.dp))
            .width(90.dp)
            .height(120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SkeletonBox(width = 24.dp, height = 24.dp)
        SkeletonBox(
            modifier = Modifier.fillMaxWidth(0.8f),
            height = 20.dp
        )
        Spacer(modifier = Modifier.height(4.dp))
        SkeletonBox(
            modifier = Modifier.fillMaxWidth(0.6f),
            height = 30.dp
        )
        Spacer(modifier = Modifier.height(4.dp))
        SkeletonBox(
            modifier = Modifier.fillMaxWidth(0.7f),
            height = 16.dp
        )
    }
}