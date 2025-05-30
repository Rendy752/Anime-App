package com.luminoverse.animevibe.ui.common

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomModalBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    isLandscape: Boolean,
    onDismiss: () -> Unit,
    sheetGestureEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val bottomSheetWidthFraction = if (isLandscape) 0.7f else 0.95f
    val bottomSheetHeightFraction = if (isLandscape) 0.9f else 0.6f
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val bottomPadding = 48.dp
    val shape = MaterialTheme.shapes.extraLarge

    ModalBottomSheet(
        modifier = modifier
            .height((configuration.screenHeightDp * bottomSheetHeightFraction).dp)
            .width((configuration.screenWidthDp * bottomSheetWidthFraction).dp)
            .padding(bottom = if (isLandscape) 0.dp else bottomPadding),
        sheetGesturesEnabled = sheetGestureEnabled,
        containerColor = containerColor,
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        shape = shape,
        contentWindowInsets = { WindowInsets(0.dp) }
    ) {
        content()
    }
}