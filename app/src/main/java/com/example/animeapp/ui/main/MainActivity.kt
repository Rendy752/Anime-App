package com.example.animeapp.ui.main

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.animeapp.AnimeApplication
import com.example.animeapp.ui.animeWatch.AnimeWatchViewModel
import com.example.animeapp.ui.common_ui.ConfirmationAlert
import com.example.animeapp.ui.theme.AppTheme
import com.example.animeapp.utils.HlsPlayerUtil
import com.example.animeapp.utils.PipUtil.buildPipActions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val onPictureInPictureModeChangedListeners = mutableListOf<(Boolean) -> Unit>()
    private lateinit var navController: NavHostController
    private var lastInteractionTime = System.currentTimeMillis()
    private val idleTimeoutMillis = TimeUnit.MINUTES.toMillis(1)
    private val intentChannel = Channel<Intent>(Channel.CONFLATED)
    private lateinit var pipParamsBuilder: PictureInPictureParams.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        pipParamsBuilder = PictureInPictureParams.Builder().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setAutoEnterEnabled(true)
            }
            setActions(buildPipActions(this@MainActivity, false))
        }

        intent?.let { intentChannel.trySend(it) }

        setContent {
            navController = rememberNavController()

            val mainViewModel: MainViewModel = hiltViewModel()
            val animeWatchViewModel: AnimeWatchViewModel = hiltViewModel()
            val state by mainViewModel.state.collectAsStateWithLifecycle()
            val pipSourceRect by animeWatchViewModel.pipSourceRect.collectAsStateWithLifecycle()

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val resetIdleTimer = remember { { lastInteractionTime = System.currentTimeMillis() } }

            val currentRoute = navController.currentDestination?.route
            val isOnWatchScreen = currentRoute?.startsWith("animeWatch/") == true

            LaunchedEffect(isOnWatchScreen, pipSourceRect) {
                if (isOnWatchScreen && pipSourceRect != null) {
                    Log.d("MainActivity", "PiP SourceRectHint: $pipSourceRect")
                    pipParamsBuilder.setSourceRectHint(pipSourceRect)
                    setPictureInPictureParams(pipParamsBuilder.build())
                }
            }

            LaunchedEffect(Unit) {
                while (true) {
                    delay(500)
                    val currentTime = System.currentTimeMillis()
                    val isIdle = currentTime - lastInteractionTime > idleTimeoutMillis
                    val route = navController.currentDestination?.route

                    if (isIdle && route?.startsWith("animeWatch/") == false) {
                        mainViewModel.dispatch(MainAction.SetIsShowIdleDialog(true))
                    }
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
                    onCancel = {
                        mainViewModel.dispatch(MainAction.SetIsShowIdleDialog(false))
                        resetIdleTimer()
                    }
                )
            }

            AppTheme(
                isDarkMode = state.isDarkMode,
                contrastMode = state.contrastMode,
                colorStyle = state.colorStyle
            ) {
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
                        intentChannel = intentChannel,
                        onResetIdleTimer = resetIdleTimer,
                        mainState = state.copy(isLandscape = isLandscape),
                        mainAction = mainViewModel::dispatch
                    )
                    setStatusBarAppearance(MaterialTheme.colorScheme.surface)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                intent?.let { intentChannel.send(it) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.let { intentChannel.trySend(it) }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        lastInteractionTime = System.currentTimeMillis()
        return super.dispatchTouchEvent(ev)
    }

    private fun setStatusBarAppearance(color: Color) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = color.luminance() > 0.5f
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        configuration: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, configuration)
        Log.d(
            "MainActivity",
            "onPictureInPictureModeChanged: isInPictureInPictureMode=$isInPictureInPictureMode, configuration=$configuration"
        )
        onPictureInPictureModeChangedListeners.forEach {
            if (!isInPictureInPictureMode) {
                (application as AnimeApplication).getMediaPlaybackService()?.pausePlayer()
            }
            it(isInPictureInPictureMode)
        }
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
            val isPlaying = HlsPlayerUtil.state.value.isPlaying
            Log.d("MainActivity", "onUserLeaveHint: Entering PiP, isPlaying=$isPlaying")
            pipParamsBuilder.setActions(buildPipActions(this, isPlaying))
            enterPictureInPictureMode(pipParamsBuilder.build())
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause")
        (applicationContext as AnimeApplication).cleanupService()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy")
        (applicationContext as AnimeApplication).cleanupService()
    }
}