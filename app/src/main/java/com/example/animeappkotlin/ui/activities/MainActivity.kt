package com.example.animeappkotlin.ui.activities

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.chuckerteam.chucker.api.BodyDecoder
import com.chuckerteam.chucker.api.Chucker
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.example.animeappkotlin.R
import com.example.animeappkotlin.databinding.ActivityMainBinding
import com.example.animeappkotlin.data.local.database.AnimeDetailDatabase
import com.example.animeappkotlin.data.local.database.AnimeRecommendationsDatabase
import com.example.animeappkotlin.data.remote.api.RetrofitInstance
import com.example.animeappkotlin.repository.AnimeDetailRepository
import com.example.animeappkotlin.repository.AnimeRecommendationsRepository
import com.example.animeappkotlin.ui.viewmodels.AnimeRecommendationsViewModel
import com.example.animeappkotlin.ui.providerfactories.AnimeRecommendationsViewModelProviderFactory
import com.example.animeappkotlin.ui.providerfactories.AnimeDetailViewModelProviderFactory
import com.example.animeappkotlin.ui.viewmodels.AnimeDetailViewModel
import com.example.animeappkotlin.utils.NetworkUtils
import com.example.animeappkotlin.utils.Resource
import com.example.animeappkotlin.utils.ShakeDetector
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSource
import okio.ByteString
import kotlin.text.toIntOrNull

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector

    private val viewModel: AnimeRecommendationsViewModel by lazy {
        val repository =
            AnimeRecommendationsRepository(RetrofitInstance.api, AnimeRecommendationsDatabase.getDatabase(this))
        val factory = AnimeRecommendationsViewModelProviderFactory(repository)
        ViewModelProvider(this, factory)[AnimeRecommendationsViewModel::class.java]
    }

    val animeRecommendationsViewModel: AnimeRecommendationsViewModel get() = viewModel

    val animeDetailViewModel: AnimeDetailViewModel by lazy {
        val repository =
            AnimeDetailRepository(AnimeDetailDatabase.getDatabase(this).getAnimeDetailDao())
        val factory = AnimeDetailViewModelProviderFactory(repository)
        ViewModelProvider(this, factory)[AnimeDetailViewModel::class.java]
    }

    private var isDataLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSplashScreen()
        setupSensor()
        setupViewBinding()
        setupNavigation()
        setupChucker()
    }

    private fun setupSplashScreen() {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isDataLoaded }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (NetworkUtils.isNetworkAvailable(this@MainActivity)) {
                    viewModel.animeRecommendations.collect { resource ->
                        if (resource is Resource.Success) {
                            isDataLoaded = true
                        } else if (resource is Resource.Error && !isDataLoaded) {
                            binding.root.showSnackbar("Error loading data. Please check your connection.")
                        }
                    }
                } else {
                    binding.root.showSnackbar(
                        "No internet connection. Please check your network settings.",
                        Snackbar.LENGTH_INDEFINITE
                    )
                }
            }
        }
    }

    private fun View.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(this, message, duration).show()
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
            setOf(R.id.animeRecommendationsFragment, R.id.aboutFragment)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == Intent.ACTION_VIEW && intent.scheme == "animeappkotlin" && intent.data != null) {
            handleAnimeUrl(intent.data)
        }
    }

    private fun setupChucker() {
        val decoder = @Suppress("NOTHING_TO_OVERRIDE") object : BodyDecoder {
            override fun decode(source: BufferedSource, contentType: MediaType?): String {
                return source.readString(Charsets.UTF_8)
            }

            override fun decodeRequest(request: Request, body: ByteString): String {
                return body.utf8()
            }

            override fun decodeResponse(response: Response, body: ByteString): String {
                return body.utf8()
            }
        }

        val chuckerCollector = ChuckerCollector(
            context = this,
            showNotification = true,
            retentionPeriod = RetentionManager.Period.ONE_HOUR
        )

        val chuckerInterceptor = ChuckerInterceptor.Builder(this)
            .collector(chuckerCollector)
            .maxContentLength(250_000L)
            .redactHeaders("Auth-Token", "Bearer")
            .alwaysReadResponseBody(true)
            .addBodyDecoder(decoder)
            .createShortcut(true)
            .build()

        RetrofitInstance.addInterceptor(chuckerInterceptor)
    }

    private fun handleAnimeUrl(uri: Uri?) {
        uri?.pathSegments?.let { segments ->
            if (segments.size >= 2 && segments[0] == "detail") {
                val id = segments[1].toIntOrNull()
                if (id != null) {
                    val navController =
                        supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
                            ?.findNavController()

                    navController?.let {
                        val bundle = Bundle().apply { putInt("id", id) }
                        val navOptions = NavOptions.Builder()
                            .setEnterAnim(R.anim.slide_in_right)
                            .setExitAnim(R.anim.slide_out_left)
                            .setPopEnterAnim(R.anim.slide_in_left)
                            .setPopExitAnim(R.anim.slide_out_right)
                            .build()

                        it.navigate(
                            R.id.action_global_animeDetailFragment,
                            bundle,
                            navOptions
                        )
                    }
                }
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