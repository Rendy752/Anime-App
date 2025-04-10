package com.example.animeapp.ui.main

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.animeapp.ui.common_ui.QuitConfirmationAlert
import com.example.animeapp.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val onPictureInPictureModeChangedListeners = mutableListOf<(Boolean) -> Unit>()
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            navController = rememberNavController()
            val mainViewModel: MainViewModel = hiltViewModel()

            val state by mainViewModel.state.collectAsStateWithLifecycle()

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            LaunchedEffect(state.isDarkMode) {
                if (state.isDarkMode) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }

            BackHandler {
                mainViewModel.dispatch(MainAction.SetShowQuitDialog(true))
            }

            if (state.showQuitDialog) {
                QuitConfirmationAlert(
                    onDismissRequest = { mainViewModel.dispatch(MainAction.SetShowQuitDialog(false)) },
                    onQuitConfirmed = { finish() }
                )
            }

            if (!state.themeApplied) {
                mainViewModel.dispatch(MainAction.SetThemeApplied(true))
            }

            if (state.themeApplied) {
                AppTheme(context = this) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainScreen(
                            navController = navController,
                            mainState = state.copy(isLandscape = isLandscape),
                            mainAction = mainViewModel::dispatch,
                        )
                        setStatusBarColor(MaterialTheme.colorScheme.surface)
                    }
                }
            }
        }
    }

    private fun setStatusBarColor(color: Color) {
        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = color.toArgb()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        val useDarkIcons = color.luminance() > 0.5f
        windowInsetsController.isAppearanceLightStatusBars = useDarkIcons
    }

    @Deprecated("Deprecated in android.app.Activity")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        onPictureInPictureModeChangedListeners.forEach { it(isInPictureInPictureMode) }
    }

    fun addOnPictureInPictureModeChangedListener(listener: (Boolean) -> Unit) {
        onPictureInPictureModeChangedListeners.add(listener)
    }

    fun removeOnPictureInPictureModeChangedListener(listener: (Boolean) -> Unit) {
        onPictureInPictureModeChangedListeners.remove(listener)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val currentRoute = navController.currentDestination?.route
        if (currentRoute?.startsWith("animeWatch/") == true) {
            enterPictureInPictureMode(PictureInPictureParams.Builder().build())
        }
    }
}