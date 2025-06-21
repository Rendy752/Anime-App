package com.luminoverse.animevibe.main

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.luminoverse.animevibe.models.NetworkStatus
import com.luminoverse.animevibe.ui.main.MainAction
import com.luminoverse.animevibe.ui.main.MainViewModel
import com.luminoverse.animevibe.ui.theme.ColorStyle
import com.luminoverse.animevibe.ui.theme.ContrastMode
import com.luminoverse.animevibe.ui.theme.ThemeMode
import com.luminoverse.animevibe.utils.NetworkStateMonitor
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: MainViewModel
    private lateinit var application: Application
    private lateinit var themePrefs: SharedPreferences
    private lateinit var settingsPrefs: SharedPreferences
    private lateinit var networkStateMonitor: NetworkStateMonitor
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var mockResources: Resources
    private lateinit var mockConfiguration: Configuration

    private lateinit var isConnectedLiveData: MutableLiveData<Boolean>
    private lateinit var networkStatusLiveData: MutableLiveData<NetworkStatus>

    private val mockNetworkStatus = NetworkStatus(
        icon = Icons.Filled.Wifi,
        label = "10000 Kbps",
        iconColor = Color.Green
    )

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        application = mockk(relaxed = true)
        themePrefs = mockk(relaxed = true)
        settingsPrefs = mockk(relaxed = true)
        mockResources = mockk(relaxed = true)
        mockConfiguration = mockk(relaxed = true)

        networkStateMonitor = mockk(relaxed = true)
        isConnectedLiveData = MutableLiveData(true)
        networkStatusLiveData = MutableLiveData(mockNetworkStatus)

        every {
            application.getSharedPreferences(
                "theme_prefs",
                Context.MODE_PRIVATE
            )
        } returns themePrefs
        every {
            application.getSharedPreferences(
                "settings_prefs",
                Context.MODE_PRIVATE
            )
        } returns settingsPrefs

        every { themePrefs.getString("theme_mode", ThemeMode.System.name) } returns ThemeMode.System.name
        every { themePrefs.getBoolean("is_dark_mode", false) } returns false
        every {
            themePrefs.getString(
                "contrast_mode",
                ContrastMode.Normal.name
            )
        } returns ContrastMode.Normal.name
        every {
            themePrefs.getString(
                "color_style",
                ColorStyle.Default.name
            )
        } returns ColorStyle.Default.name
        every { settingsPrefs.getBoolean("notifications_enabled", false) } returns false
        every { settingsPrefs.getBoolean("auto_play_video", true) } returns true
        every { settingsPrefs.getBoolean("rtl", false) } returns false

        val isConnectedObserver = slot<Observer<Boolean>>()
        val networkStatusObserver = slot<Observer<NetworkStatus>>()
        every { networkStateMonitor.isConnected.observeForever(capture(isConnectedObserver)) } answers {
            isConnectedObserver.captured.onChanged(true)
        }
        every { networkStateMonitor.networkStatus.observeForever(capture(networkStatusObserver)) } answers {
            networkStatusObserver.captured.onChanged(mockNetworkStatus)
        }

        viewModel = MainViewModel(application, networkStateMonitor)
        runTest { advanceUntilIdle() }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Test
    fun `init should load preferences, set system theme, and start network monitoring`() = runTest {
        val state = viewModel.state.value

        assertEquals(ThemeMode.System, state.themeMode)
        assertFalse("isDarkMode should be false when system is in light mode", state.isDarkMode)
        assertTrue(state.isConnected)
        assertEquals(mockNetworkStatus, state.networkStatus)

        verify { networkStateMonitor.startMonitoring() }
        verify { application.registerReceiver(any(), any<IntentFilter>()) }
    }

    @Test
    fun `SetThemeMode should update themeMode, isDarkMode, and persist to SharedPreferences`() = runTest {
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { themePrefs.edit() } returns editor

        viewModel.onAction(MainAction.SetThemeMode(ThemeMode.Dark))
        advanceUntilIdle()

        var state = viewModel.state.value
        assertEquals(ThemeMode.Dark, state.themeMode)
        assertTrue(state.isDarkMode)
        verify { editor.putString("theme_mode", ThemeMode.Dark.name) }
        verify { editor.apply() }

        viewModel.onAction(MainAction.SetThemeMode(ThemeMode.Light))
        advanceUntilIdle()

        state = viewModel.state.value
        assertEquals(ThemeMode.Light, state.themeMode)
        assertFalse(state.isDarkMode)
        verify { editor.putString("theme_mode", ThemeMode.Light.name) }
        verify { editor.apply() }
    }

    @Test
    fun `onCleared should stop network monitoring and unregister receiver`() = runTest {
        viewModel.onCleared()
        advanceUntilIdle()

        verify { networkStateMonitor.stopMonitoring() }
        verify { application.unregisterReceiver(any<BroadcastReceiver>()) }
    }

    @Test
    fun `SetContrastMode should update contrastMode and persist to SharedPreferences`() = runTest {
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { themePrefs.edit() } returns editor

        viewModel.onAction(MainAction.SetContrastMode(ContrastMode.High))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ContrastMode.High, state.contrastMode)
        verify { editor.putString("contrast_mode", ContrastMode.High.name) }
        verify { editor.apply() }
    }
}