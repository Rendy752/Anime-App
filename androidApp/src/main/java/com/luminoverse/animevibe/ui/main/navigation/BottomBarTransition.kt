package com.luminoverse.animevibe.ui.main.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val navigationMutex = Mutex()
private const val NAVIGATION_DEBOUNCE_MS = 300L

suspend fun navigateToAdjacentRoute(
    isNextLogical: Boolean,
    currentRoute: String?,
    navController: NavHostController
) {
    navigationMutex.withLock {
        if (currentRoute == null || currentRoute !in NavRoute.bottomRoutes.map { it.route }) {
            return
        }

        val currentIndex = NavRoute.bottomRoutes.indexOfFirst { it.route == currentRoute }
        if (currentIndex == -1) {
            return
        }

        val newIndex = if (isNextLogical) {
            (currentIndex + 1).coerceAtMost(NavRoute.bottomRoutes.size - 1)
        } else {
            (currentIndex - 1).coerceAtLeast(0)
        }

        if (newIndex != currentIndex) {
            val targetRoute = NavRoute.bottomRoutes[newIndex]
            navController.navigateTo(targetRoute)
            delay(NAVIGATION_DEBOUNCE_MS)
        }
    }
}

fun getBottomBarEnterTransition(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry,
    isRtl: Boolean
): EnterTransition {
    val initialRoute = initialState.destination.route
    val targetRoute = targetState.destination.route
    return if (initialRoute in NavRoute.bottomRoutes.map { it.route } && targetRoute in NavRoute.bottomRoutes.map { it.route }) {
        val initialIndex = NavRoute.bottomRoutes.indexOfFirst { it.route == initialRoute }
        val targetIndex = NavRoute.bottomRoutes.indexOfFirst { it.route == targetRoute }

        val slideOffset: (Int) -> Int = when {
            targetIndex > initialIndex -> { fullWidth -> if (isRtl) -fullWidth else fullWidth }
            targetIndex < initialIndex -> { fullWidth -> if (isRtl) fullWidth else -fullWidth }
            else -> { _ -> 0 }
        }
        slideInHorizontally(animationSpec = tween(700), initialOffsetX = slideOffset)
    } else {
        scaleIn(animationSpec = tween(700))
    }
}

fun getBottomBarExitTransition(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry,
    isRtl: Boolean
): ExitTransition {
    val initialRoute = initialState.destination.route
    val targetRoute = targetState.destination.route
    return if (initialRoute in NavRoute.bottomRoutes.map { it.route } && targetRoute in NavRoute.bottomRoutes.map { it.route }) {
        val initialIndex = NavRoute.bottomRoutes.indexOfFirst { it.route == initialRoute }
        val targetIndex = NavRoute.bottomRoutes.indexOfFirst { it.route == targetRoute }

        val slideOffset: (Int) -> Int = when {
            targetIndex > initialIndex -> { fullWidth -> if (isRtl) fullWidth else -fullWidth }
            targetIndex < initialIndex -> { fullWidth -> if (isRtl) -fullWidth else fullWidth }
            else -> { _ -> 0 }
        }
        slideOutHorizontally(animationSpec = tween(700), targetOffsetX = slideOffset)
    } else {
        scaleOut(animationSpec = tween(700))
    }
}
