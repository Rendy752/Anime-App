package com.example.animeapp.ui.main

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.example.animeapp.ui.common_ui.QuitConfirmationAlert
import com.example.animeapp.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val onPictureInPictureModeChangedListeners = mutableListOf<(Boolean) -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        val themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val isDarkMode = themePrefs.getBoolean("is_dark_mode", false)

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)

        setContent {
            val themeApplied = remember { mutableStateOf(false) }
            var showQuitDialog by remember { mutableStateOf(false) }

            BackHandler {
                showQuitDialog = true
            }

            if (showQuitDialog) {
                QuitConfirmationAlert(
                    onDismissRequest = { showQuitDialog = false },
                    onQuitConfirmed = { finish() }
                )
            }

            if (!themeApplied.value) {
                themeApplied.value = true
            }

            if (themeApplied.value) {
                AppTheme(context = this) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainScreen()
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

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
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

//    override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//        if (intent.action == Intent.ACTION_VIEW &&
//            intent.scheme == "animeapp" &&
//            intent.data != null
//        ) handleAnimeUrl(intent.data, rememberNavController())
//    }
//
//    private fun handleAnimeUrl(uri: Uri?, navController: NavController) {
//        uri?.pathSegments?.let { segments ->
//            if (segments.size >= 2 && segments[0] == "detail") {
//                val animeId = segments[1].toIntOrNull()
//                if (animeId != null) {
//                    navController.navigate("animeDetail/$animeId")
//                }
//            } else Toast.makeText(this@MainActivity, "Invalid URL", Toast.LENGTH_SHORT).show()
//        }
//    }
}