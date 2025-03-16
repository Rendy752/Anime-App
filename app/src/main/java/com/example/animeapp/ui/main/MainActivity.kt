package com.example.animeapp.ui.main

import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.example.animeapp.ui.theme.AppTheme
import com.example.animeapp.utils.ShakeDetector
import com.example.animeapp.BuildConfig.DEBUG
import com.chuckerteam.chucker.api.Chucker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        WindowCompat.setDecorFitsSystemWindows(window, false)
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
        if (DEBUG) setupSensor()
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
        if (DEBUG) sensorManager.registerListener(
            shakeDetector,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onPause() {
        super.onPause()
        if (DEBUG) sensorManager.unregisterListener(shakeDetector)
    }

    private fun setupSensor() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector { startActivity(Chucker.getLaunchIntent(this)) }
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