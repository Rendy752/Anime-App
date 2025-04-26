package com.example.animeapp.ui.main

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

val bottomRoutes = BottomScreen.entries.map { it.route }

fun getBottomBarEnterTransition(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry
): EnterTransition {
    val initialRoute = initialState.destination.route
    val targetRoute = targetState.destination.route
    return if (initialRoute in bottomRoutes && targetRoute in bottomRoutes) {
        val initialIndex = BottomScreen.orderedList.indexOfFirst { it.route == initialRoute }
        val targetIndex = BottomScreen.orderedList.indexOfFirst { it.route == targetRoute }
        val slideOffset: (Int) -> Int = when {
            targetIndex > initialIndex -> { fullWidth -> fullWidth }
            targetIndex < initialIndex -> { fullWidth -> -fullWidth }
            else -> { _ -> 0 }
        }
        slideInHorizontally(animationSpec = tween(700), initialOffsetX = slideOffset)
    } else {
        scaleIn(animationSpec = tween(700))
    }
}

fun getBottomBarExitTransition(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry
): ExitTransition {
    val initialRoute = initialState.destination.route
    val targetRoute = targetState.destination.route
    return if (initialRoute in bottomRoutes && targetRoute in bottomRoutes) {
        val initialIndex = BottomScreen.orderedList.indexOfFirst { it.route == initialRoute }
        val targetIndex = BottomScreen.orderedList.indexOfFirst { it.route == targetRoute }
        val slideOffset: (Int) -> Int = when {
            targetIndex > initialIndex -> { fullWidth -> -fullWidth }
            targetIndex < initialIndex -> { fullWidth -> fullWidth }
            else -> { _ -> 0 }
        }
        slideOutHorizontally(animationSpec = tween(700), targetOffsetX = slideOffset)
    } else {
        scaleOut(animationSpec = tween(700))
    }
}