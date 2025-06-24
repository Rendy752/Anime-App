package com.luminoverse.animevibe.ui.main

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.luminoverse.animevibe.AnimeApplication
import com.luminoverse.animevibe.ui.common.ConfirmationAlert
import com.luminoverse.animevibe.ui.theme.AppTheme
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.HlsPlayerUtils
import com.luminoverse.animevibe.utils.media.MediaPlaybackAction
import com.luminoverse.animevibe.utils.media.PipUtil.buildPipActions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.runtime.getValue

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var hlsPlayerUtils: HlsPlayerUtils

    private val mainViewModel: MainViewModel by viewModels()

    private val onPictureInPictureModeChangedListeners = mutableListOf<(Boolean) -> Unit>()
    private lateinit var navController: NavHostController
    private var lastInteractionTime = System.currentTimeMillis()
    val resetIdleTimer = { lastInteractionTime = System.currentTimeMillis() }
    private val idleTimeoutMillis = TimeUnit.MINUTES.toMillis(1)
    private val intentChannel = Channel<Intent>(Channel.CONFLATED)
    private lateinit var pipParamsBuilder: PictureInPictureParams.Builder
    private var lastBackPressTime = 0L
    private val backPressTimeoutMillis = 2000L

    private lateinit var appUpdateManager: AppUpdateManager
    private val updateType = AppUpdateType.FLEXIBLE
    private var installStateUpdatedListener: InstallStateUpdatedListener? = null

    private val updateActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                println("Update flow failed! Result code: " + result.resultCode)
            }
        }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            println("MainActivity: POST_NOTIFICATIONS permission granted")
            mainViewModel.onAction(MainAction.CheckNotificationPermission)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        appUpdateManager = AppUpdateManagerFactory.create(applicationContext)

        installStateUpdatedListener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackbarForCompleteUpdate()
            }
        }
        appUpdateManager.registerListener(installStateUpdatedListener!!)

        checkForAppUpdate()

        pipParamsBuilder = PictureInPictureParams.Builder().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setAutoEnterEnabled(false)
            }
            setActions(buildPipActions(this@MainActivity, false))
        }

        intent?.let { intentChannel.trySend(it) }

        requestNotificationPermission()

        setContent {
            val state by mainViewModel.state.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            navController = rememberNavController()

            LaunchedEffect(state.snackbarMessage) {
                state.snackbarMessage?.let { snackbarMessage ->
                    val messageText = when (snackbarMessage.type) {
                        SnackbarMessageType.SUCCESS -> "✅ ${snackbarMessage.message}"
                        SnackbarMessageType.ERROR -> "❌ ${snackbarMessage.message}"
                        SnackbarMessageType.INFO -> "ℹ️ ${snackbarMessage.message}"
                    }
                    val result = snackbarHostState.showSnackbar(
                        message = messageText,
                        actionLabel = snackbarMessage.actionLabel,
                        duration = if (snackbarMessage.actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        snackbarMessage.onAction?.invoke()
                    }
                    mainViewModel.onAction(MainAction.DismissSnackbar)
                }
            }


            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            LaunchedEffect(Unit) {
                startIdleDetection(
                    isShowIdleDialog = state.isShowIdleDialog,
                    action = { mainViewModel.onAction(MainAction.SetIsShowIdleDialog(it)) }
                )
            }

            BackHandler {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < backPressTimeoutMillis) {
                    finish()
                } else {
                    lastBackPressTime = currentTime
                    mainViewModel.onAction(
                        MainAction.ShowSnackbar(
                            SnackbarMessage(
                                message = "Press back again to exit AnimeVibe",
                                type = SnackbarMessageType.INFO
                            )
                        )
                    )
                }
            }

            AppTheme(
                themeMode = state.themeMode,
                contrastMode = state.contrastMode,
                colorStyle = state.colorStyle,
                isRtl = state.isRtl
            ) {
                Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { paddingValues ->
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
                        modifier = Modifier.consumeWindowInsets(paddingValues),
                        navController = navController,
                        intentChannel = intentChannel,
                        resetIdleTimer = resetIdleTimer,
                        mainState = state.copy(isLandscape = isLandscape),
                        mainAction = mainViewModel::onAction,
                    )
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                intent?.let { intentChannel.send(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resetIdleTimer()
        mainViewModel.onAction(MainAction.CheckNotificationPermission)
        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    popupSnackbarForCompleteUpdate()
                }
                if (appUpdateInfo.updateAvailability()
                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateActivityResultLauncher,
                        AppUpdateOptions.newBuilder(updateType).build()
                    )
                }
            }
    }

    private fun checkForAppUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            val isUpdateAvailable =
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            if (isUpdateAvailable && appUpdateInfo.isUpdateTypeAllowed(updateType)) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateActivityResultLauncher,
                    AppUpdateOptions.newBuilder(updateType).build()
                )
            }
        }.addOnFailureListener { e ->
            println("Failed to check for update: ${e.message}")
        }
    }

    private fun popupSnackbarForCompleteUpdate() {
        mainViewModel.onAction(
            MainAction.ShowSnackbar(
                SnackbarMessage(
                    message = "An update has just been downloaded.",
                    type = SnackbarMessageType.SUCCESS,
                    actionLabel = "RESTART",
                    onAction = { appUpdateManager.completeUpdate() }
                )
            )
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            when {
                checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED -> {
                    println("MainActivity: POST_NOTIFICATIONS permission already granted")
                }

                shouldShowRequestPermissionRationale(permission) -> {
                    requestPermissionLauncher.launch(permission)
                }

                else -> {
                    requestPermissionLauncher.launch(permission)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.let { intentChannel.trySend(it) }
        resetIdleTimer()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        resetIdleTimer()
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
        resetIdleTimer()
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
        installStateUpdatedListener?.let {
            appUpdateManager.unregisterListener(it)
        }
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