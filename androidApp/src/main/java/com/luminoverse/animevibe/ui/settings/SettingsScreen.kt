package com.luminoverse.animevibe.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.ui.common.ToggleWithLabel
import com.luminoverse.animevibe.ui.main.MainAction
import com.luminoverse.animevibe.ui.main.MainState
import com.luminoverse.animevibe.ui.settings.components.ColorStyleCard
import com.luminoverse.animevibe.ui.settings.components.ContrastModeChips
import com.luminoverse.animevibe.ui.settings.components.HeaderText
import com.luminoverse.animevibe.ui.settings.components.SettingItem
import com.luminoverse.animevibe.ui.settings.components.ThemeModeChips
import com.luminoverse.animevibe.ui.theme.ColorStyle
import com.luminoverse.animevibe.utils.onDisableNotifications
import com.luminoverse.animevibe.utils.onEnableNotifications

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    mainState: MainState,
    mainAction: (MainAction) -> Unit,
    settingsState: SettingsState,
    onSettingsAction: (SettingsAction) -> Unit,
    rememberedTopPadding: Dp
) {
    val colorStyleCardScrollState = rememberScrollState()
    val context = LocalContext.current

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> mainAction(MainAction.CheckNotificationPermission) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        mainAction(MainAction.SetPostNotificationsPermission(isGranted))
        if (!isGranted) {
            settingsLauncher.launch(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            )
        }
        mainAction(MainAction.CheckNotificationPermission)
    }

    LaunchedEffect(Unit) {
        onSettingsAction(SettingsAction.UpdateCacheSize)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = rememberedTopPadding + 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeaderText("Appearance")
        }
        item {
            ThemeModeChips(
                selectedThemeMode = mainState.themeMode,
                onThemeModeSelected = { mainAction(MainAction.SetThemeMode(it)) },
            )
            ContrastModeChips(
                selectedContrastMode = mainState.contrastMode,
                onContrastModeChanged = { mainAction(MainAction.SetContrastMode(it)) }
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Color Style",
                    style = MaterialTheme.typography.titleMedium,
                )
                ColorStyle.entries.forEach { style ->
                    ColorStyleCard(
                        state = colorStyleCardScrollState,
                        colorStyle = style,
                        isSelected = style == mainState.colorStyle,
                        themeMode = mainState.themeMode,
                        isRtl = mainState.isRtl,
                        contrastMode = mainState.contrastMode,
                        onColorStyleSelected = { mainAction(MainAction.SetColorStyle(style)) },
                    )
                }
            }
        }

        item {
            HeaderText("Behavior & Data")
        }
        item {
            SettingItem(
                title = "Clear Video Cache",
                description = "Current size: ${settingsState.cacheSize}",
                onClick = { onSettingsAction(SettingsAction.ClearCache) }
            )
        }

        item {
            HeaderText("Notifications")
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleWithLabel(
                    isActive = mainState.isPostNotificationsPermissionGranted,
                    label = "All Notifications",
                    description = "Master control for all app notifications (Requires system permission)",
                    onToggle = { enable ->
                        if (enable) {
                            onEnableNotifications(
                                context = context,
                                onPermissionGranted = {
                                    mainAction(
                                        MainAction.SetPostNotificationsPermission(true)
                                    )
                                },
                                permissionLauncher = permissionLauncher,
                                settingsLauncher = settingsLauncher
                            )
                        } else {
                            onDisableNotifications(
                                context = context,
                                settingsLauncher = settingsLauncher
                            )
                        }
                    }
                )


                AnimatedVisibility(visible = mainState.isPostNotificationsPermissionGranted) {
                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ToggleWithLabel(
                            isActive = mainState.notificationSettings.broadcastEnabled,
                            label = "Airing Reminders",
                            description = "Get notified minutes before your favorite airing anime begins",
                            onToggle = { mainAction(MainAction.SetBroadcastNotifications(it)) }
                        )
                        ToggleWithLabel(
                            isActive = mainState.notificationSettings.unfinishedEnabled,
                            label = "Continue Watching Reminders",
                            description = "Receive periodic reminders for episodes you haven't finished",
                            onToggle = { mainAction(MainAction.SetUnfinishedNotifications(it)) }
                        )
                        ToggleWithLabel(
                            isActive = mainState.notificationSettings.playbackEnabled,
                            label = "Playback Controls",
                            description = "Show media controls in the notification shade while playing a video",
                            onToggle = { mainAction(MainAction.SetPlaybackNotifications(it)) }
                        )
                    }
                }
            }
        }

        item {
            HeaderText("Others")
        }
        item {
            ToggleWithLabel(
                isActive = mainState.isAutoPlayVideo,
                label = "Auto Play Video",
                description = "Enable auto play video",
                onToggle = { mainAction(MainAction.SetAutoPlayVideo(it)) }
            )
            ToggleWithLabel(
                isActive = mainState.isRtl,
                label = "Right-to-Left Layout",
                description = "Enable right-to-left layout for text and UI",
                onToggle = { mainAction(MainAction.SetRtl(it)) }
            )
        }
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        mainState = MainState(),
        mainAction = {},
        settingsState = SettingsState(cacheSize = "123.4 MB"),
        onSettingsAction = {},
        rememberedTopPadding = 0.dp
    )
}