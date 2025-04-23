package com.example.animeapp.ui.main

import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Icon
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.animeapp.AnimeApplication
import com.example.animeapp.ui.common_ui.ConfirmationAlert
import com.example.animeapp.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.session.MediaButtonReceiver
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val onPictureInPictureModeChangedListeners = mutableListOf<(Boolean) -> Unit>()
    private lateinit var navController: NavHostController
    private var lastInteractionTime = System.currentTimeMillis()
    private val idleTimeoutMillis = TimeUnit.MINUTES.toMillis(1)
    private val intentChannel = Channel<Intent>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        intent?.let { intentChannel.trySend(it) }

        setContent {
            navController = rememberNavController()

            val mainViewModel: MainViewModel = hiltViewModel()
            val state by mainViewModel.state.collectAsStateWithLifecycle()

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val resetIdleTimer = remember { { lastInteractionTime = System.currentTimeMillis() } }

            val isPipMode by remember { mutableStateOf(isInPictureInPictureMode) }
            val mediaService = (application as AnimeApplication).getMediaPlaybackService()
            val isPlaying by mediaService?.isPlayingState?.collectAsState(initial = false)
                ?: remember { mutableStateOf(false) }

            LaunchedEffect(isPipMode, isPlaying) {
                if (isPipMode) {
                    updatePipActions(isPlaying)
                }
            }

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
                    onCancel = {
                        mainViewModel.dispatch(MainAction.SetIsShowIdleDialog(false))
                        resetIdleTimer()
                    }
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
                            intentChannel = intentChannel,
                            onResetIdleTimer = resetIdleTimer,
                            mainState = state.copy(isLandscape = isLandscape),
                            mainAction = mainViewModel::dispatch
                        )
                        setStatusBarColor(MaterialTheme.colorScheme.surface)
                    }
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
        if (isInPictureInPictureMode) {
            val isPlaying = (application as AnimeApplication).getMediaPlaybackService()?.getExoPlayer()?.isPlaying == true
            updatePipActions(isPlaying)
        }
    }

    fun addOnPictureInPictureModeChangedListener(listener: (Boolean) -> Unit) {
        onPictureInPictureModeChangedListeners.add(listener)
    }

    fun removeOnPictureInPictureModeChangedListener(listener: (Boolean) -> Unit) {
        onPictureInPictureModeChangedListeners.remove(listener)
    }

    private fun updatePipActions(isPlaying: Boolean?): PictureInPictureParams {
        val service = (application as AnimeApplication).getMediaPlaybackService()
        val currentEpisodeNo = service?.getCurrentEpisodeNo() ?: -1
        val episodes = service?.getEpisodes() ?: emptyList()

        val actions = mutableListOf<RemoteAction>()
        val hasPreviousEpisode = currentEpisodeNo > 1 && episodes.any { it.episodeNo == currentEpisodeNo - 1 }
        val hasNextEpisode = episodes.any { it.episodeNo == currentEpisodeNo + 1 }

        if (hasPreviousEpisode) {
            actions.add(
                RemoteAction(
                    Icon.createWithResource(this, androidx.media3.session.R.drawable.media3_icon_previous),
                    "Previous",
                    "Skip to previous episode",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    )
                )
            )
        }

        actions.add(
            RemoteAction(
                Icon.createWithResource(
                    this,
                    if (isPlaying == true) androidx.media3.session.R.drawable.media3_icon_pause else androidx.media3.session.R.drawable.media3_icon_play
                ),
                if (isPlaying == true) "Pause" else "Play",
                if (isPlaying == true) "Pause playback" else "Resume playback",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
            )
        )

        if (hasNextEpisode) {
            actions.add(
                RemoteAction(
                    Icon.createWithResource(this, androidx.media3.session.R.drawable.media3_icon_next),
                    "Next",
                    "Skip to next episode",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    )
                )
            )
        }

        return PictureInPictureParams.Builder()
            .setActions(actions)
            .build()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val currentRoute = navController.currentDestination?.route
        if (currentRoute?.startsWith("animeWatch/") == true) {
            val isPlaying = (application as AnimeApplication).getMediaPlaybackService()?.getExoPlayer()?.isPlaying == true
            enterPictureInPictureMode(updatePipActions(isPlaying))
        }
    }
}