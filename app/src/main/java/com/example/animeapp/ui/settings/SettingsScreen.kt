package com.example.animeapp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.animeapp.ui.main.components.BottomScreen
import com.example.animeapp.ui.main.MainAction
import com.example.animeapp.ui.main.MainState
import com.example.animeapp.ui.settings.components.ColorStyleSelector
import com.example.animeapp.ui.settings.components.ContrastModeSelector
import com.example.animeapp.ui.settings.components.DarkModeToggle
import com.example.animeapp.ui.theme.ColorStyle
import com.example.animeapp.utils.ColorUtils
import com.example.animeapp.utils.basicContainer

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun SettingsScreen(
    mainState: MainState = MainState(),
    mainAction: (MainAction) -> Unit = {}
) {
    Scaffold(
        topBar = {
            Column {
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
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DarkModeToggle(
                isDarkMode = mainState.isDarkMode,
                onDarkModeChanged = { mainAction(MainAction.SetDarkMode(it)) }
            )
            ContrastModeSelector(
                contrastMode = mainState.contrastMode,
                onContrastModeChanged = { mainAction(MainAction.SetContrastMode(it)) }
            )
            ColorStyleSelector(
                colorStyle = mainState.colorStyle,
                onColorStyleChanged = { mainAction(MainAction.SetColorStyle(it)) }
            )

            ColorStyle.entries.forEach { style ->
                val scheme = ColorUtils.generateColorScheme(
                    style,
                    mainState.isDarkMode,
                    mainState.contrastMode
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${style.name} Preview",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .basicContainer(
                                    backgroundBrush = Brush.verticalGradient(
                                        colors = listOf(
                                            scheme.primary,
                                            scheme.primaryContainer
                                        )
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .basicContainer(
                                    backgroundBrush = Brush.verticalGradient(
                                        colors = listOf(
                                            scheme.secondary,
                                            scheme.secondaryContainer
                                        )
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .basicContainer(
                                    backgroundBrush = Brush.verticalGradient(
                                        colors = listOf(
                                            scheme.tertiary,
                                            scheme.tertiaryContainer
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}