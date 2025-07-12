package com.luminoverse.animevibe.ui.main.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry

fun getBottomBarEnterTransition(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry
): EnterTransition {
    val initialRoute = initialState.destination.route
    val targetRoute = targetState.destination.route
    return if (initialRoute in NavRoute.bottomRoutes.map { it.route } && targetRoute in NavRoute.bottomRoutes.map { it.route }) {
        val initialIndex = NavRoute.bottomRoutes.indexOfFirst { it.route == initialRoute }
        val targetIndex = NavRoute.bottomRoutes.indexOfFirst { it.route == targetRoute }

        val slideOffset: (Int) -> Int = when {
            targetIndex > initialIndex -> { fullWidth -> fullWidth }
            targetIndex < initialIndex -> { fullWidth -> -fullWidth }
            else -> { _ -> 0 }
        }
        slideInHorizontally(animationSpec = tween(700), initialOffsetX = slideOffset)
    } else {
        slideInVertically { fullHeight -> fullHeight } + fadeIn()
    }
}

fun getBottomBarExitTransition(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry
): ExitTransition {
    val initialRoute = initialState.destination.route
    val targetRoute = targetState.destination.route
    return if (initialRoute in NavRoute.bottomRoutes.map { it.route } && targetRoute in NavRoute.bottomRoutes.map { it.route }) {
        val initialIndex = NavRoute.bottomRoutes.indexOfFirst { it.route == initialRoute }
        val targetIndex = NavRoute.bottomRoutes.indexOfFirst { it.route == targetRoute }

        val slideOffset: (Int) -> Int = when {
            targetIndex > initialIndex -> { fullWidth -> -fullWidth }
            targetIndex < initialIndex -> { fullWidth -> fullWidth }
            else -> { _ -> 0 }
        }
        slideOutHorizontally(animationSpec = tween(700), targetOffsetX = slideOffset)
    } else {
        slideOutVertically { fullHeight -> fullHeight } + fadeOut()
    }
}
