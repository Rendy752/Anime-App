package com.example.animeapp.ui.main.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed interface NavRoute {
    val route: String
    val arguments: List<NamedNavArgument> get() = emptyList()

    object Home : NavRoute {
        override val route = "home"
    }

    object Recommendations : NavRoute {
        override val route = "recommendations"
    }

    object Search : NavRoute {
        override val route = "search"
    }

    object History : NavRoute {
        override val route = "history"
    }

    object Settings : NavRoute {
        override val route = "settings"
    }

    data class AnimeDetail(val id: Int) : NavRoute {
        override val route = "animeDetail/{id}"
        override val arguments = listOf(
            navArgument("id") { type = NavType.IntType }
        )

        companion object {
            const val ROUTE_PATTERN = "animeDetail/{id}"
            fun fromId(id: Int) = AnimeDetail(id)
        }
    }

    data class AnimeWatch(val malId: Int, val episodeId: String) : NavRoute {
        override val route = "animeWatch/{malId}/{episodeId}"
        override val arguments = listOf(
            navArgument("malId") { type = NavType.IntType },
            navArgument("episodeId") { type = NavType.StringType }
        )

        companion object {
            const val ROUTE_PATTERN = "animeWatch/{malId}/{episodeId}"
            fun fromParams(malId: Int, episodeId: String) = AnimeWatch(malId, episodeId)
        }
    }

    data class SearchWithFilter(
        val genreId: Int? = null,
        val producerId: Int? = null
    ) : NavRoute {
        override val route = "search/{genreId}/{producerId}"
        override val arguments = listOf(
            navArgument("genreId") { type = NavType.StringType; nullable = true },
            navArgument("producerId") { type = NavType.StringType; nullable = true }
        )

        companion object {
            const val ROUTE_PATTERN = "search/{genreId}/{producerId}"
            fun fromFilter(genreId: Int?, producerId: Int?) = SearchWithFilter(genreId, producerId)
        }
    }

    companion object {
        val bottomRoutes = listOf(Home, Recommendations, Search, History, Settings)
        val orderedBottomRoutes = bottomRoutes
    }
}