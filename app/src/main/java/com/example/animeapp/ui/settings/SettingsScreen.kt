package com.example.animeapp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.animeapp.ui.main.components.BottomScreen
import com.example.animeapp.ui.main.MainAction
import com.example.animeapp.ui.main.MainState
import com.example.animeapp.ui.settings.components.ColorStyleCard
import com.example.animeapp.ui.settings.components.ContrastModeChips
import com.example.animeapp.ui.settings.components.DarkModeToggle
import com.example.animeapp.ui.theme.ColorStyle

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun SettingsScreen(
    mainState: MainState = MainState(),
    mainAction: (MainAction) -> Unit = {}
) {
    Scaffold(
        topBar = {
            if (!mainState.isLandscape) Column {
                TopAppBar(
                    title = {
                        Text(
                            text = BottomScreen.Settings.label,
                            modifier = Modifier.padding(end = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    thickness = 2.dp
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DarkModeToggle(
                isDarkMode = mainState.isDarkMode,
                onDarkModeChanged = { mainAction(MainAction.SetDarkMode(it)) }
            )
            ContrastModeChips(
                selectedContrastMode = mainState.contrastMode,
                onContrastModeChanged = { mainAction(MainAction.SetContrastMode(it)) }
            )
            Text(
                text = "Color Style",
                style = MaterialTheme.typography.titleMedium
            )
            ColorStyle.entries.forEach { style ->
                ColorStyleCard(
                    colorStyle = style,
                    isSelected = style == mainState.colorStyle,
                    isDarkMode = mainState.isDarkMode,
                    contrastMode = mainState.contrastMode,
                    onColorStyleSelected = { mainAction(MainAction.SetColorStyle(style)) }
                )
            }
        }
    }
}