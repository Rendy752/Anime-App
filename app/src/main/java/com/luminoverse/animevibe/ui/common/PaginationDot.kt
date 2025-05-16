package com.luminoverse.animevibe.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PaginationDot() {
    Text(
        text = "...",
        modifier = Modifier.padding(horizontal = 4.dp),
        fontSize = 18.sp
    )
}