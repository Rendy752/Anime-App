package com.example.animeapp.ui.settings.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animeapp.R
import androidx.core.content.edit

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val themePrefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    val isDarkMode = remember { mutableStateOf(themePrefs.getBoolean("is_dark_mode", false)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(id = R.string.dark_mode))
                Text(text = stringResource(id = R.string.dark_mode_description), fontSize = 12.sp)
            }
            Switch(
                checked = isDarkMode.value,
                onCheckedChange = { checked ->
                    isDarkMode.value = checked
                    themePrefs.edit {
                        putBoolean("is_dark_mode", checked)
                    }
                    if (checked) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                }
            )
        }
    }
}