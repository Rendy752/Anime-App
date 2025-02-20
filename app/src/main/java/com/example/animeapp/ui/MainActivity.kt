package com.example.animeapp.ui

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.chuckerteam.chucker.api.Chucker
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.example.animeapp.R
import com.example.animeapp.data.remote.api.RetrofitInstance
import com.example.animeapp.databinding.ActivityMainBinding
import com.example.animeapp.utils.Navigation
import com.example.animeapp.utils.ShakeDetector
import com.example.animeapp.utils.Theme
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSplashScreen()
        setupTheme()
        setupSensor()
        setupViewBinding()
        setupNavigation()
        setupChucker()
    }

    private fun setupSplashScreen() {
        installSplashScreen()
    }

    private fun setupTheme() {
        val sharedPrefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("is_dark_mode", false)
        Theme.setTheme(isDarkMode)
    }

    private fun setupSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector { startActivity(Chucker.getLaunchIntent(this)) }
    }

    private fun setupViewBinding() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun setupNavigation() {
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.animeRecommendationsFragment,
                R.id.animeSearchFragment,
                R.id.settingsFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    private fun setupChucker() {
        val chuckerCollector = ChuckerCollector(
            this,
            true,
            RetentionManager.Period.ONE_HOUR
        )

        val chuckerInterceptor = ChuckerInterceptor.Builder(this)
            .collector(chuckerCollector)
            .maxContentLength(250_000L)
            .redactHeaders("Auth-Token", "Bearer")
            .alwaysReadResponseBody(true)
            .createShortcut(true)
            .build()

        RetrofitInstance.addInterceptor(chuckerInterceptor)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == Intent.ACTION_VIEW &&
            intent.scheme == "animeapp" &&
            intent.data != null
        ) {
            handleAnimeUrl(intent.data)
        }
    }

    private fun handleAnimeUrl(uri: Uri?) {
        uri?.pathSegments?.let { segments ->
            if (segments.size >= 2 && segments[0] == "detail") {
                val animeId = segments[1].toIntOrNull()
                if (animeId != null) {
                    val navHostFragment =
                        supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
                    val currentFragment = navHostFragment?.childFragmentManager?.fragments?.get(0)
                    if (currentFragment != null) {
                        Navigation.navigateToAnimeDetail(
                            currentFragment,
                            animeId,
                            R.id.action_global_animeDetailFragment
                        )
                    }
                }
            } else {
                Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            shakeDetector,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(shakeDetector)
    }
}