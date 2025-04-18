package com.example.animeapp.ui.main

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.animeapp.ui.common_ui.ConfirmationAlert
import com.example.animeapp.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val onPictureInPictureModeChangedListeners = mutableListOf<(Boolean) -> Unit>()
    private lateinit var navController: NavHostController
    private var lastInteractionTime = System.currentTimeMillis()
    private val idleTimeoutMillis = TimeUnit.MINUTES.toMillis(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            navController = rememberNavController()
            val mainViewModel: MainViewModel = hiltViewModel()

            val state by mainViewModel.state.collectAsStateWithLifecycle()

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            val resetIdleTimer = remember { { lastInteractionTime = System.currentTimeMillis() } }

            LaunchedEffect(Unit) {
                while (true) {
                    delay(500)
                    val currentTime = System.currentTimeMillis()
                    val isIdle = currentTime - lastInteractionTime > idleTimeoutMillis
                    val currentRoute = navController.currentDestination?.route

                    if (isIdle && currentRoute?.startsWith("animeWatch/") == false) {
                        mainViewModel.dispatch(MainAction.SetIsShowIdleDialog(true))
                    }
                }
            }

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
                ConfirmationAlert(
                    title = "Quit AnimeApp?",
                    message = "Are you sure you want to quit the app?",
                    onConfirm = { finish() },
                    onCancel = { mainViewModel.dispatch(MainAction.SetShowQuitDialog(false)) }
                )
            }

            if (state.isShowIdleDialog) {
                ConfirmationAlert(
                    title = "Are you still there?",
                    message = "It seems you haven't interacted with the app for a while. Are you want to quit the app?",
                    onConfirm = { finish() },
                    onCancel = { mainViewModel.dispatch(MainAction.SetIsShowIdleDialog(false)) }
                )
            }

            if (!state.themeApplied) {
                mainViewModel.dispatch(MainAction.SetThemeApplied(true))
            }

            if (state.themeApplied) {
                AppTheme(context = this) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = { resetIdleTimer() },
                                    onDoubleTap = { resetIdleTimer() },
                                    onLongPress = { resetIdleTimer() }
                                )
                            },
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

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        lastInteractionTime = System.currentTimeMillis()
        return super.dispatchTouchEvent(ev)
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