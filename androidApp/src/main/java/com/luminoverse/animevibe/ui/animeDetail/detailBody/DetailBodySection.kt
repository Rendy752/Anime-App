package com.luminoverse.animevibe.ui.animeDetail.detailBody

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.ui.common.ClickableDataTextWithIcon
import com.luminoverse.animevibe.ui.common.ClickableItem
import com.luminoverse.animevibe.ui.common.DataTextWithIcon
import com.luminoverse.animevibe.ui.common.DataTextWithIconSkeleton
import com.luminoverse.animevibe.ui.main.navigation.NavRoute
import com.luminoverse.animevibe.ui.main.navigation.navigateTo
import com.luminoverse.animevibe.utils.basicContainer

@Composable
fun DetailBodySection(animeDetail: AnimeDetail, navController: NavController) {
    Column(
        modifier = Modifier
            .basicContainer(outerPadding = PaddingValues(0.dp))
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                DataTextWithIcon(
                    label = "Status",
                    value = animeDetail.status,
                    icon = Icons.Default.Info
                )
                DataTextWithIcon(
                    label = "Type",
                    value = animeDetail.type ?: "Unknown",
                    icon = Icons.Default.PlayCircle
                )
                DataTextWithIcon(
                    label = "Source",
                    value = animeDetail.source,
                    icon = Icons.AutoMirrored.Filled.MenuBook
                )
                DataTextWithIcon(
                    label = "Season",
                    value = animeDetail.season ?: "-",
                    icon = Icons.Default.CalendarMonth
                )
                DataTextWithIcon(
                    label = "Released",
                    value = animeDetail.year?.toString() ?: "-",
                    icon = Icons.Default.Event
                )
                DataTextWithIcon(
                    label = "Aired",
                    value = animeDetail.aired.string,
                    icon = Icons.Default.DateRange
                )
                DataTextWithIcon(
                    label = "Rating",
                    value = animeDetail.rating ?: "Unknown",
                    icon = Icons.Default.Star
                )
                DataTextWithIcon(
                    label =
                        "Episodes", value = animeDetail.episodes.toString(),
                    icon = Icons.AutoMirrored.Filled.List
                )

                val genreItems = animeDetail.genres?.map { genre ->
                    ClickableItem(genre.name) {
                        navController.navigateTo(
                            NavRoute.SearchWithFilter.fromFilter(
                                genreId = genre.mal_id,
                                producerId = null
                            )
                        )
                    }
                }
                ClickableDataTextWithIcon(
                    label = "Genres",
                    items = genreItems,
                    icon = Icons.AutoMirrored.Filled.Label
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val studioItems = animeDetail.studios?.map { studio ->
                    ClickableItem(studio.name) {
                        navController.navigateTo(
                            NavRoute.SearchWithFilter.fromFilter(
                                genreId = null,
                                producerId = studio.mal_id
                            )
                        )
                    }
                }
                ClickableDataTextWithIcon(
                    label = "Studios",
                    items = studioItems,
                    icon = Icons.Default.Apartment
                )

                val producerItems = animeDetail.producers?.map { producer ->
                    ClickableItem(producer.name) {
                        navController.navigateTo(
                            NavRoute.SearchWithFilter.fromFilter(
                                genreId = null,
                                producerId = producer.mal_id
                            )
                        )
                    }
                }
                ClickableDataTextWithIcon(
                    label = "Producers",
                    items = producerItems,
                    icon = Icons.Default.Apartment
                )

                val licensorItems = animeDetail.licensors?.map { licensor ->
                    ClickableItem(licensor.name) {
                        navController.navigateTo(
                            NavRoute.SearchWithFilter.fromFilter(
                                genreId = null,
                                producerId = licensor.mal_id
                            )
                        )
                    }
                }
                ClickableDataTextWithIcon(
                    label = "Licensors",
                    items = licensorItems,
                    icon = Icons.Default.Apartment
                )

                DataTextWithIcon(
                    label = "Broadcast", value = animeDetail.broadcast.string ?: "-",
                    icon = Icons.Default.NotificationsActive
                )
                DataTextWithIcon(
                    label = "Duration",
                    value = animeDetail.duration,
                    icon = Icons.Default.AccessTime
                )
            }
        }
    }
}

@Preview
@Composable
fun DetailBodySectionSkeleton() {
    Column(
        modifier = Modifier
            .basicContainer(outerPadding = PaddingValues(0.dp))
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                repeat(9) {
                    DataTextWithIconSkeleton()
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                repeat(5) {
                    DataTextWithIconSkeleton()
                }
            }
        }
    }
}