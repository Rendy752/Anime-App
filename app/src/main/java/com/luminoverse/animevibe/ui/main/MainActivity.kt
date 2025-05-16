package com.luminoverse.animevibe.ui.main

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
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
import com.luminoverse.animevibe.AnimeApplication
import com.luminoverse.animevibe.ui.common.ConfirmationAlert
import com.luminoverse.animevibe.ui.theme.AppTheme
import com.luminoverse.animevibe.utils.HlsPlayerUtils
import com.luminoverse.animevibe.utils.PipUtil.buildPipActions
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
                setAutoEnterEnabled(false)
            }
            setActions(buildPipActions(this@MainActivity, false))
        }

        intent?.let { intentChannel.trySend(it) }

        setContent {
            navController = rememberNavController()
            val mainViewModel: MainViewModel = hiltViewModel()
            val state by mainViewModel.state.collectAsStateWithLifecycle()

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val resetIdleTimer = { lastInteractionTime = System.currentTimeMillis() }

            LaunchedEffect(Unit) {
                startIdleDetection { mainViewModel.onAction(MainAction.SetIsShowIdleDialog(it)) }
            }

            BackHandler {
                mainViewModel.onAction(MainAction.SetShowQuitDialog(true))
            }

            AppTheme(
                isDarkMode = state.isDarkMode,
                contrastMode = state.contrastMode,
                colorStyle = state.colorStyle
            ) {
                if (state.showQuitDialog) {
                    ConfirmationAlert(
                        title = "Quit AnimeVibe?",
                        message = "Are you sure you want to quit the app?",
                        onConfirm = { finish() },
                        onCancel = { mainViewModel.onAction(MainAction.SetShowQuitDialog(false)) }
                    )
                }

                if (state.isShowIdleDialog) {
                    ConfirmationAlert(
                        title = "Are you still there?",
                        message = "It seems you haven't interacted with the app for a while. Would you like to quit the app?",
                        onConfirm = { finish() },
                        onCancel = {
                            mainViewModel.onAction(MainAction.SetIsShowIdleDialog(false))
                            resetIdleTimer()
                        }
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { resetIdleTimer() },
                                onDoubleTap = { resetIdleTimer() },
                                onLongPress = { resetIdleTimer() }
                            )
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                    resetIdleTimer()
                                }
                            }
                        },
                    color = MaterialTheme.colorScheme.surface
                ) {
                    MainScreen(
                        navController = navController,
                        intentChannel = intentChannel,
                        onResetIdleTimer = resetIdleTimer,
                        mainState = state.copy(isLandscape = isLandscape),
                        mainAction = mainViewModel::onAction
                    )
                    setSystemBarAppearance(MaterialTheme.colorScheme.surface)
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
        lastInteractionTime = System.currentTimeMillis()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        lastInteractionTime = System.currentTimeMillis()
        return super.dispatchTouchEvent(ev)
    }

    private fun setSystemBarAppearance(color: Color) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = color.luminance() > 0.5f
        windowInsetsController.isAppearanceLightNavigationBars = color.luminance() > 0.5f
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        configuration: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, configuration)
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
        val isPlaying = HlsPlayerUtils.state.value.isPlaying
        if (currentRoute?.startsWith("animeWatch/") == true && isPlaying) {
            pipParamsBuilder.setActions(buildPipActions(this, true))
            enterPictureInPictureMode(pipParamsBuilder.build())
        }
    }

    override fun onPause() {
        super.onPause()
        (applicationContext as AnimeApplication).cleanupService()
    }

    override fun onDestroy() {
        super.onDestroy()
        (applicationContext as AnimeApplication).cleanupService()
    }

    fun exitPipModeIfActive() {
        if (isInPictureInPictureMode) {
            moveTaskToBack(true)
        }
    }

    private fun startIdleDetection(action: (Boolean) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(500)
                    val currentTime = System.currentTimeMillis()
                    val isIdle = currentTime - lastInteractionTime > idleTimeoutMillis
                    val currentRoute = navController.currentDestination?.route
                    if (isIdle && currentRoute?.startsWith("animeWatch/") == false) {
                        action(true)
                    }
                }
            }
        }
    }
}