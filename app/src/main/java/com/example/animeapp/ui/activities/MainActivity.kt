package com.example.animeapp.ui.activities

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.animeapp.R
import com.example.animeapp.databinding.ActivityMainBinding
import com.example.animeapp.data.local.database.AnimeDetailDatabase
import com.example.animeapp.data.local.database.AnimeRecommendationsDatabase
import com.example.animeapp.repository.AnimeDetailRepository
import com.example.animeapp.repository.AnimeRecommendationsRepository
import com.example.animeapp.ui.viewmodels.AnimeRecommendationsViewModel
import com.example.animeapp.ui.providerfactories.AnimeRecommendationsViewModelProviderFactory
import com.example.animeapp.ui.providerfactories.AnimeDetailViewModelProviderFactory
import com.example.animeapp.ui.viewmodels.AnimeDetailViewModel
import com.example.animeapp.utils.LogUtils
import com.example.animeapp.utils.ShakeDetector

class MainActivity : AppCompatActivity() {
    private val viewModel: AnimeRecommendationsViewModel by lazy {
        val animeRecommendationsRepository = AnimeRecommendationsRepository(
            AnimeRecommendationsDatabase.getDatabase(this)
        )
        val viewModelProviderFactory =
            AnimeRecommendationsViewModelProviderFactory(animeRecommendationsRepository)
        ViewModelProvider(
            this,
            viewModelProviderFactory
        ).get(AnimeRecommendationsViewModel::class.java)
    }

    private lateinit var binding: ActivityMainBinding
    val animeRecommendationsViewModel: AnimeRecommendationsViewModel
        get() = viewModel

    val animeDetailViewModel: AnimeDetailViewModel by lazy {
        val animeDetailRepository = AnimeDetailRepository(AnimeDetailDatabase.getDatabase(this))
        val animeDetailViewModelProviderFactory =
            AnimeDetailViewModelProviderFactory(animeDetailRepository)
        ViewModelProvider(
            this,
            animeDetailViewModelProviderFactory
        )[AnimeDetailViewModel::class.java]
    }

    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector {
            LogUtils.showLogs(this)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.animeRecommendationsFragment, R.id.aboutFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
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