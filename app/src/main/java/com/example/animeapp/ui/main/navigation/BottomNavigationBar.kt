package com.example.animeapp.ui.main.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        NavRoute.bottomRoutes.forEach { screen ->
            NavigationBarItem(
                modifier = Modifier.padding(horizontal = 4.dp),
                icon = when (screen) {
                    NavRoute.Home -> {
                        { Icon(Icons.Filled.Home, contentDescription = "Home") }
                    }

                    NavRoute.Recommendations -> {
                        {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = "Recommendations"
                            )
                        }
                    }

                    NavRoute.Search -> {
                        { Icon(Icons.Filled.Search, contentDescription = "Search") }
                    }

                    NavRoute.History -> {
                        { Icon(Icons.Filled.History, contentDescription = "History") }
                    }

                    NavRoute.Settings -> {
                        { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
                    }

                    else -> {
                        {}
                    }
                },
                label = {
                    Text(
                        text = screen.route.replaceFirstChar { it.uppercase() },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                selected = currentRoute == screen.route,
                onClick = { navController.navigateTo(screen) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}