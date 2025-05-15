package com.example.animeapp.ui.main.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import java.net.URLEncoder

fun NavController.navigateTo(route: NavRoute) {
    when (route) {
        is NavRoute.Home, NavRoute.Recommendations, NavRoute.Search, NavRoute.History, NavRoute.Settings -> {
            navigate(route.route) {
                popUpTo(graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        is NavRoute.AnimeDetail -> {
            navigate("animeDetail/${route.id}")
        }

        is NavRoute.AnimeWatch -> {
            val malIdEncoded = URLEncoder.encode(route.malId.toString(), "UTF-8")
            val episodeIdEncoded = URLEncoder.encode(route.episodeId, "UTF-8")
            navigate("animeWatch/$malIdEncoded/$episodeIdEncoded")
        }

        is NavRoute.SearchWithFilter -> {
            val genreId = route.genreId?.toString() ?: "null"
            val producerId = route.producerId?.toString() ?: "null"
            navigate("search/$genreId/$producerId")
        }
    }
}