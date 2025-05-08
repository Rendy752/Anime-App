package com.example.animeapp.ui.settings.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.animeDetailPlaceholder
import com.example.animeapp.ui.common_ui.AnimeHeader
import com.example.animeapp.ui.common_ui.AnimeHeaderSkeleton
import com.example.animeapp.ui.common_ui.AnimeScheduleItem
import com.example.animeapp.ui.common_ui.AnimeScheduleItemSkeleton
import com.example.animeapp.ui.common_ui.AnimeSearchItem
import com.example.animeapp.ui.common_ui.ContinueWatchingAnime
import com.example.animeapp.ui.theme.ColorStyle
import com.example.animeapp.ui.theme.ContrastMode
import com.example.animeapp.utils.ColorUtils
import com.example.animeapp.utils.Resource

@Composable
fun ColorStyleCard(
    animeDetailSample: Resource<AnimeDetail>,
    state: ScrollState,
    colorStyle: ColorStyle,
    isSelected: Boolean,
    isDarkMode: Boolean,
    contrastMode: ContrastMode,
    onColorStyleSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = ColorUtils.generateColorScheme(colorStyle, isDarkMode, contrastMode)

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color.Blue else Color.Gray,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onColorStyleSelected() }
            .semantics { contentDescription = "Color style ${colorStyle.name} card" }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Column {
                Text(
                    text = "${colorStyle.name} Style",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Preview of ${colorStyle.name} color scheme",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            MaterialTheme(colorScheme = colorScheme) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .horizontalScroll(state),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (animeDetailSample is Resource.Loading) AnimeScheduleItemSkeleton(
                        modifier = Modifier.widthIn(max = 160.dp)
                    )
                    else AnimeScheduleItem(
                        animeDetail = animeDetailSample.data ?: animeDetailPlaceholder,
                        modifier = Modifier.widthIn(max = 160.dp)
                    )
                    Column(
                        modifier = Modifier.widthIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (animeDetailSample is Resource.Loading) AnimeScheduleItemSkeleton()
                        else AnimeSearchItem(
                            animeDetail = animeDetailSample.data ?: animeDetailPlaceholder
                        )
                        ContinueWatchingAnime()
                    }
                    if (animeDetailSample is Resource.Loading) AnimeHeaderSkeleton(
                        modifier = Modifier.widthIn(max = 400.dp)
                    )
                    else AnimeHeader(
                        animeDetail = animeDetailSample.data ?: animeDetailPlaceholder,
                        modifier = Modifier.widthIn(max = 400.dp)
                    )
                }
            }
        }
    }
}