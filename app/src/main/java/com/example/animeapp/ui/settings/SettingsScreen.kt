package com.example.animeapp.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.animeapp.ui.main.components.BottomScreen
import com.example.animeapp.ui.main.MainAction
import com.example.animeapp.ui.main.MainState
import com.example.animeapp.ui.settings.components.ContrastModeSelector
import com.example.animeapp.ui.settings.components.DarkModeToggle

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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
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
                .padding(16.dp)
        ) {
            DarkModeToggle(
                isDarkMode = mainState.isDarkMode,
                onDarkModeChanged = { mainAction(MainAction.SetDarkMode(it)) }
            )
            ContrastModeSelector(
                contrastMode = mainState.contrastMode,
                onContrastModeChanged = { mainAction(MainAction.SetContrastMode(it)) }
            )
        }
    }
}