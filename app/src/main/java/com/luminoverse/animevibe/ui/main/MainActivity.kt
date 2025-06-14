package com.luminoverse.animevibe.ui.main

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
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
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.HlsPlayerUtils
import com.luminoverse.animevibe.utils.media.PipUtil.buildPipActions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import android.content.pm.PackageManager
import com.luminoverse.animevibe.utils.media.MediaPlaybackAction
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var hlsPlayerUtils: HlsPlayerUtils

    private val onPictureInPictureModeChangedListeners = mutableListOf<(Boolean) -> Unit>()
    private lateinit var navController: NavHostController
    private var lastInteractionTime = System.currentTimeMillis()
    private val idleTimeoutMillis = TimeUnit.MINUTES.toMillis(1)
    private val intentChannel = Channel<Intent>(Channel.CONFLATED)
    private lateinit var pipParamsBuilder: PictureInPictureParams.Builder
    private var lastBackPressTime = 0L
    private val backPressTimeoutMillis = 2000L

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            println("MainActivity: POST_NOTIFICATIONS permission granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        pipParamsBuilder = PictureInPictureParams.Builder().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setAutoEnterEnabled(false)
            }
            setActions(buildPipActions(this@MainActivity, false))
        }

        intent?.let { intentChannel.trySend(it) }

        requestNotificationPermission()

        setContent {
            navController = rememberNavController()
            val mainViewModel: MainViewModel = hiltViewModel()
            val state by mainViewModel.state.collectAsStateWithLifecycle()

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val resetIdleTimer = { lastInteractionTime = System.currentTimeMillis() }

            LaunchedEffect(Unit) {
                startIdleDetection(
                    isShowIdleDialog = mainViewModel.state.value.isShowIdleDialog,
                    action = { mainViewModel.onAction(MainAction.SetIsShowIdleDialog(it)) }
                )
            }

            BackHandler {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < backPressTimeoutMillis) {
                    finish()
                } else {
                    lastBackPressTime = currentTime
                    Toast.makeText(
                        this@MainActivity,
                        "Press back again to exit AnimeVibe",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            AppTheme(
                isDarkMode = state.isDarkMode,
                contrastMode = state.contrastMode,
                colorStyle = state.colorStyle,
                isRtl = state.isRtl
            ) {
                if (state.isShowIdleDialog) {
                    ConfirmationAlert(
                        title = "Are you still there ?",
                        message = "It seems you haven't interacted with the app for a while. Would you like to quit the app ?",
                        onConfirm = { finish() },
                        onCancel = {
                            mainViewModel.onAction(MainAction.SetIsShowIdleDialog(false))
                            resetIdleTimer()
                        }
                    )
                }

                MainScreen(
                    navController = navController,
                    intentChannel = intentChannel,
                    resetIdleTimer = resetIdleTimer,
                    mainState = state.copy(isLandscape = isLandscape),
                    mainAction = mainViewModel::onAction,
                )
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                intent?.let { intentChannel.send(it) }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            when {
                checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED -> {
                    println("MainActivity: POST_NOTIFICATIONS permission already granted")
                }

                shouldShowRequestPermissionRationale(permission) -> {
                    println("MainActivity: Showing rationale for POST_NOTIFICATIONS permission")
                    requestPermissionLauncher.launch(permission)
                }

                else -> {
                    println("MainActivity: Requesting POST_NOTIFICATIONS permission")
                    requestPermissionLauncher.launch(permission)
                }
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

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        configuration: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, configuration)
        onPictureInPictureModeChangedListeners.forEach {
            if (!isInPictureInPictureMode && !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                hlsPlayerUtils.dispatch(HlsPlayerAction.Pause)
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
        if (::navController.isInitialized) {
            val currentRoute = navController.currentDestination?.route
            val isPlaying = hlsPlayerUtils.getPlayer()?.isPlaying == true
            if (currentRoute?.startsWith("animeWatch/") == true && isPlaying) {
                pipParamsBuilder.setActions(buildPipActions(this@MainActivity, true))
                enterPictureInPictureMode(pipParamsBuilder.build())
            } else {
                (applicationContext as AnimeApplication).getMediaPlaybackService()
                    ?.dispatch(MediaPlaybackAction.StopService)
            }
        }
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

    private fun startIdleDetection(isShowIdleDialog: Boolean, action: (Boolean) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(1000)
                    val currentTime = System.currentTimeMillis()
                    val isIdle = currentTime - lastInteractionTime > idleTimeoutMillis
                    val currentRoute = navController.currentDestination?.route

                    if (isIdle && currentRoute?.startsWith("animeWatch/") != true) {
                        if (!isShowIdleDialog) {
                            action(true)
                        }
                    }
                }
            }
        }
    }
}