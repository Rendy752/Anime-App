package com.luminoverse.animevibe.ui.settings.components

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
import androidx.navigation.NavBackStackEntry
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.animeDetailPlaceholder
import com.luminoverse.animevibe.models.episodeDetailComplementPlaceholder
import com.luminoverse.animevibe.models.episodePlaceholder
import com.luminoverse.animevibe.ui.common.EpisodeDetailItem
import com.luminoverse.animevibe.ui.common.EpisodeDetailItemSkeleton
import com.luminoverse.animevibe.ui.common.AnimeHeader
import com.luminoverse.animevibe.ui.common.AnimeHeaderSkeleton
import com.luminoverse.animevibe.ui.common.AnimeScheduleItem
import com.luminoverse.animevibe.ui.common.AnimeScheduleItemSkeleton
import com.luminoverse.animevibe.ui.common.AnimeSearchItem
import com.luminoverse.animevibe.ui.common.AnimeSearchItemSkeleton
import com.luminoverse.animevibe.ui.common.ContinueWatchingAnime
import com.luminoverse.animevibe.ui.theme.ColorStyle
import com.luminoverse.animevibe.ui.theme.ContrastMode
import com.luminoverse.animevibe.utils.ColorUtils
import com.luminoverse.animevibe.utils.resource.Resource
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun ColorStyleCard(
    animeDetailSample: Resource<AnimeDetail>,
    state: ScrollState,
    colorStyle: ColorStyle,
    isSelected: Boolean,
    isDarkMode: Boolean,
    contrastMode: ContrastMode,
    onColorStyleSelected: () -> Unit,
    navBackStackEntry: NavBackStackEntry?,
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
                        .heightIn(max = 200.dp)
                        .horizontalScroll(state),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (animeDetailSample is Resource.Loading) AnimeScheduleItemSkeleton(
                        modifier = Modifier.widthIn(max = 130.dp)
                    )
                    else AnimeScheduleItem(
                        animeDetail = animeDetailSample.data ?: animeDetailPlaceholder,
                        modifier = Modifier.widthIn(max = 130.dp)
                    )

                    Column(
                        modifier = Modifier.widthIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (animeDetailSample is Resource.Loading) EpisodeDetailItemSkeleton(
                            modifier = Modifier.heightIn(max = 100.dp)
                        )
                        else EpisodeDetailItem(
                            modifier = Modifier.heightIn(max = 100.dp),
                            animeDetail = animeDetailSample.data ?: animeDetailPlaceholder,
                            lastEpisodeWatchedId = episodePlaceholder.episodeId,
                            episode = episodePlaceholder.copy(
                                name = animeDetailSample.data?.title ?: episodePlaceholder.name
                            ),
                            episodeDetailComplement = episodeDetailComplementPlaceholder.copy(
                                lastWatched = SimpleDateFormat.getDateInstance().format(Date()),
                                lastTimestamp = 260_000L,
                                duration = 300_000L
                            ),
                            query = (animeDetailSample.data?.title ?: episodePlaceholder.name).let {
                                if (it.length > 3) it.take(3) else it
                            },
                            navBackStackEntry = navBackStackEntry,
                            titleMaxLines = 2
                        )

                        ContinueWatchingAnime(
                            episodeDetailComplement = episodeDetailComplementPlaceholder.copy(
                                animeTitle = animeDetailSample.data?.title
                                    ?: episodeDetailComplementPlaceholder.animeTitle,
                                imageUrl = animeDetailSample.data?.images?.webp?.large_image_url
                            )
                        )
                    }

                    if (animeDetailSample is Resource.Loading) AnimeSearchItemSkeleton(
                        modifier = Modifier.widthIn(max = 400.dp)
                    )
                    else AnimeSearchItem(
                        modifier = Modifier.widthIn(max = 400.dp),
                        animeDetail = animeDetailSample.data ?: animeDetailPlaceholder
                    )

                    if (animeDetailSample is Resource.Loading) AnimeHeaderSkeleton(
                        modifier = Modifier.widthIn(max = 300.dp),
                        showImage = false
                    )
                    else AnimeHeader(
                        animeDetail = animeDetailSample.data ?: animeDetailPlaceholder,
                        modifier = Modifier.widthIn(max = 300.dp),
                        showImage = false
                    )
                }
            }
        }
    }
}