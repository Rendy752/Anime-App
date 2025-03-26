package com.example.animeapp.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable

enum class Screen(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
) {
    Recommendations(
        "recommendations",
        "Recommendations",
        { Icon(Icons.Filled.Home, contentDescription = null) }),
    Search("search", "Search", { Icon(Icons.Filled.Search, contentDescription = null) }),
    Settings("settings", "Settings", { Icon(Icons.Filled.Settings, contentDescription = null) });
}