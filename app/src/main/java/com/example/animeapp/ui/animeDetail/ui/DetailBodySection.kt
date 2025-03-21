package com.example.animeapp.ui.animeDetail.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.ui.common_ui.ClickableDataTextWithIcon
import com.example.animeapp.ui.common_ui.ClickableItem
import com.example.animeapp.ui.common_ui.DataTextWithIcon
import com.example.animeapp.utils.basicContainer
import com.example.animeapp.utils.Navigation.navigateWithFilter

@Composable
fun DetailBodySection(animeDetail: AnimeDetail, navController: NavController) {
    Column(
        modifier = Modifier
            .basicContainer()
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                DataTextWithIcon("Status", animeDetail.status, Icons.Default.Info)
                DataTextWithIcon("Type", animeDetail.type ?: "Unknown", Icons.Default.Category)
                DataTextWithIcon("Source", animeDetail.source, Icons.AutoMirrored.Filled.MenuBook)
                DataTextWithIcon("Season", animeDetail.season ?: "-", Icons.Default.CalendarMonth)
                DataTextWithIcon(
                    "Released",
                    animeDetail.year?.toString() ?: "-",
                    Icons.Default.Event
                )
                DataTextWithIcon("Aired", animeDetail.aired.string, Icons.Default.DateRange)
                DataTextWithIcon("Rating", animeDetail.rating ?: "Unknown", Icons.Default.Star)
                DataTextWithIcon(
                    "Episodes", animeDetail.episodes.toString(),
                    Icons.AutoMirrored.Filled.List
                )

                val genreItems = animeDetail.genres?.map { genre ->
                    ClickableItem(genre.name) {
                        navigateWithFilter(navController, genre, genreFilter = true)
                    }
                }
                ClickableDataTextWithIcon("Genres", genreItems, Icons.AutoMirrored.Filled.Label)
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                val studioItems = animeDetail.studios?.map { studio ->
                    ClickableItem(studio.name) {
                        navigateWithFilter(navController, studio)
                    }
                }
                ClickableDataTextWithIcon("Studios", studioItems, Icons.Default.Apartment)

                val producerItems = animeDetail.producers?.map { producer ->
                    ClickableItem(producer.name) {
                        navigateWithFilter(navController, producer)
                    }
                }
                ClickableDataTextWithIcon("Producers", producerItems, Icons.Default.Apartment)

                val licensorItems = animeDetail.licensors?.map { licensor ->
                    ClickableItem(licensor.name) {
                        navigateWithFilter(navController, licensor)
                    }
                }
                ClickableDataTextWithIcon("Licensors", licensorItems, Icons.Default.Apartment)

                DataTextWithIcon(
                    "Broadcast",
                    animeDetail.broadcast.string ?: "-",
                    Icons.Default.NotificationsActive
                )
                DataTextWithIcon("Duration", animeDetail.duration, Icons.Default.AccessTime)
            }
        }
    }
}