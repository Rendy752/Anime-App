package com.example.animeapp.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable

enum class BottomScreen(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
) {
    Home("home", "Home", { Icon(Icons.Filled.Home, contentDescription = "Home") }),
    Recommendations(
        "recommendations",
        "Recommendations",
        { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Recommendations") }),
    Search("search", "Search", { Icon(Icons.Filled.Search, contentDescription = "Search") }),
    Settings("settings", "Settings", { Icon(Icons.Filled.Settings, contentDescription = "Settings") });

    companion object {
        val orderedList = listOf(Home, Recommendations, Search, Settings)
    }
}