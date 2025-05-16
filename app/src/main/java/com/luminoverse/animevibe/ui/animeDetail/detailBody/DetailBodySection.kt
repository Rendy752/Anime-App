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
                DataTextWithIcon("Status", animeDetail.status, Icons.Default.Info)
                DataTextWithIcon("Type", animeDetail.type ?: "Unknown", Icons.Default.PlayCircle)
                DataTextWithIcon("Source", animeDetail.source, Icons.AutoMirrored.Filled.MenuBook)
                DataTextWithIcon("Season", animeDetail.season ?: "-", Icons.Default.CalendarMonth)
                DataTextWithIcon(
                    "Released", animeDetail.year?.toString() ?: "-", Icons.Default.Event
                )
                DataTextWithIcon("Aired", animeDetail.aired.string, Icons.Default.DateRange)
                DataTextWithIcon("Rating", animeDetail.rating ?: "Unknown", Icons.Default.Star)
                DataTextWithIcon(
                    "Episodes", animeDetail.episodes.toString(),
                    Icons.AutoMirrored.Filled.List
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
                ClickableDataTextWithIcon("Genres", genreItems, Icons.AutoMirrored.Filled.Label)
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
                ClickableDataTextWithIcon("Studios", studioItems, Icons.Default.Apartment)

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
                ClickableDataTextWithIcon("Producers", producerItems, Icons.Default.Apartment)

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
                ClickableDataTextWithIcon("Licensors", licensorItems, Icons.Default.Apartment)

                DataTextWithIcon(
                    "Broadcast", animeDetail.broadcast.string ?: "-",
                    Icons.Default.NotificationsActive
                )
                DataTextWithIcon("Duration", animeDetail.duration, Icons.Default.AccessTime)
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
            Column(
                modifier = Modifier.weight(1f)
            ) {
                repeat(9) {
                    DataTextWithIconSkeleton()
                }
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                repeat(5) {
                    DataTextWithIconSkeleton()
                }
            }
        }
    }
}