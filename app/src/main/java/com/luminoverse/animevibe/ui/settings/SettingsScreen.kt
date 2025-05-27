package com.luminoverse.animevibe.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.luminoverse.animevibe.ui.common.ToggleWithLabel
import com.luminoverse.animevibe.ui.main.MainAction
import com.luminoverse.animevibe.ui.main.MainState
import com.luminoverse.animevibe.ui.settings.components.ColorStyleCard
import com.luminoverse.animevibe.ui.settings.components.ContrastModeChips
import com.luminoverse.animevibe.ui.theme.ColorStyle
import com.luminoverse.animevibe.utils.resource.Resource
import androidx.navigation.NavBackStackEntry

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun SettingsScreen(
    mainState: MainState = MainState(),
    mainAction: (MainAction) -> Unit = {},
    state: SettingsState = SettingsState(),
    action: (SettingsAction) -> Unit = {},
    navBackStackEntry: NavBackStackEntry? = null
) {
    val colorStyleCardScrollState = rememberScrollState()
    val context = LocalContext.current

    val density = LocalDensity.current
    val statusBarPadding = with(density) {
        WindowInsets.systemBars.getTop(density).toDp()
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> mainAction(MainAction.CheckNotificationPermission) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        mainAction(MainAction.SetNotificationEnabled(isGranted))
        if (!isGranted) {
            settingsLauncher.launch(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            )
        }
        mainAction(MainAction.CheckNotificationPermission)
    }

    LaunchedEffect(mainState.isConnected) {
        if (!mainState.isConnected) return@LaunchedEffect
        if (state.animeDetailSample is Resource.Error) action(SettingsAction.GetRandomAnime)
    }

    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(paddingValues)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ToggleWithLabel(
                    isActive = mainState.isDarkMode,
                    label = "Dark Mode",
                    description = "Enable dark mode",
                    onToggle = { mainAction(MainAction.SetDarkMode(it)) },
                    modifier = Modifier.padding(top = statusBarPadding)
                )
            }
            item {
                ToggleWithLabel(
                    isActive = mainState.isAutoPlayVideo,
                    label = "Auto Play Video",
                    description = "Enable auto play video",
                    onToggle = { mainAction(MainAction.SetAutoPlayVideo(it)) }
                )
            }
            item {
                ToggleWithLabel(
                    isActive = mainState.isRtl,
                    label = "Right-to-Left Layout",
                    description = "Enable right-to-left layout for text and UI",
                    onToggle = { mainAction(MainAction.SetRtl(it)) }
                )
            }
            item {
                ToggleWithLabel(
                    isActive = mainState.isNotificationEnabled,
                    label = "Notifications",
                    description = "Enable notifications",
                    onToggle = { enable ->
                        if (enable) {
                            if (SDK_INT >= 33) {
                                val permissionStatus = ContextCompat.checkSelfPermission(
                                    context,
                                    "android.permission.POST_NOTIFICATIONS"
                                )
                                val isGranted =
                                    permissionStatus == PackageManager.PERMISSION_GRANTED
                                if (isGranted) {
                                    mainAction(MainAction.SetNotificationEnabled(true))
                                } else {
                                    val shouldShowRequest =
                                        (context as? ComponentActivity)?.shouldShowRequestPermissionRationale(
                                            "android.permission.POST_NOTIFICATIONS"
                                        )
                                    if (shouldShowRequest == true) {
                                        permissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
                                    } else {
                                        settingsLauncher.launch(
                                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                putExtra(
                                                    Settings.EXTRA_APP_PACKAGE,
                                                    context.packageName
                                                )
                                            }
                                        )
                                    }
                                }
                            } else {
                                mainAction(MainAction.SetNotificationEnabled(true))
                            }
                        } else {
                            settingsLauncher.launch(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                            )
                        }
                    }
                )
            }
            item {
                ContrastModeChips(
                    selectedContrastMode = mainState.contrastMode,
                    onContrastModeChanged = { mainAction(MainAction.SetContrastMode(it)) }
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Color Style",
                        style = MaterialTheme.typography.titleMedium
                    )
                    ColorStyle.entries.forEach { style ->
                        ColorStyleCard(
                            animeDetailSample = state.animeDetailSample,
                            state = colorStyleCardScrollState,
                            colorStyle = style,
                            isSelected = style == mainState.colorStyle,
                            isDarkMode = mainState.isDarkMode,
                            contrastMode = mainState.contrastMode,
                            onColorStyleSelected = { mainAction(MainAction.SetColorStyle(style)) },
                            navBackStackEntry = navBackStackEntry
                        )
                    }
                }
            }
        }
    }
}