package com.example.animeapp.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class NetworkStatus(
    val icon: ImageVector,
    val label: String,
    val iconColor: Color
)

val networkStatusPlaceholder = NetworkStatus(
    icon = Icons.Filled.SignalCellularOff,
    label = "No signal",
    iconColor = Color.Gray
)