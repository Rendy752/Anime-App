package com.luminoverse.animevibe.main

import android.annotation.SuppressLint
import android.app.Application
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: MainViewModel
    private lateinit var application: Application
    private lateinit var themePrefs: SharedPreferences
    private lateinit var settingsPrefs: SharedPreferences
    private lateinit var networkStateMonitor: NetworkStateMonitor

    private lateinit var networkStatusFlow: MutableStateFlow<NetworkStatus>

    private val mockNetworkStatusConnected = NetworkStatus(
        icon = Icons.Filled.Wifi,
        label = "10000 Kbps",
        iconColor = Color.Green,
        isConnected = true
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        application = mockk(relaxed = true)
        themePrefs = mockk(relaxed = true)
        settingsPrefs = mockk(relaxed = true)
        networkStateMonitor = mockk(relaxed = true)

        val notificationManager = mockk<NotificationManager>(relaxed = true)
        every { application.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        every { notificationManager.areNotificationsEnabled() } returns true

        val appOpsManager = mockk<AppOpsManager>(relaxed = true)
        every { application.getSystemService(Context.APP_OPS_SERVICE) } returns appOpsManager

        networkStatusFlow = MutableStateFlow(mockNetworkStatusConnected)

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

        every {
            themePrefs.getString(
                "theme_mode",
                ThemeMode.System.name
            )
        } returns ThemeMode.System.name
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
        every { settingsPrefs.getBoolean("auto_play_video", true) } returns true
        every { settingsPrefs.getBoolean("rtl", false) } returns false

        every { settingsPrefs.getBoolean("notifications_broadcast_enabled", true) } returns true
        every { settingsPrefs.getBoolean("notifications_unfinished_enabled", true) } returns true
        every { settingsPrefs.getBoolean("notifications_playback_enabled", true) } returns true


        every { networkStateMonitor.networkStatus } returns networkStatusFlow

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
        assertFalse("isDarkMode should be false for initial state", state.isDarkMode)
        assertTrue("isConnected should be true on init", state.networkStatus.isConnected)
        assertEquals(mockNetworkStatusConnected, state.networkStatus)

        verify { networkStateMonitor.startMonitoring() }
        verify { application.registerReceiver(any(), any<IntentFilter>()) }
    }

    @Test
    fun `SetThemeMode should update themeMode and isDarkMode correctly`() = runTest {
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { themePrefs.edit() } returns editor

        viewModel.onAction(MainAction.SetThemeMode(ThemeMode.Dark))
        advanceUntilIdle()

        var state = viewModel.state.value
        assertEquals(ThemeMode.Dark, state.themeMode)
        assertTrue("isDarkMode should be true for Dark theme", state.isDarkMode)
        verify { editor.putString("theme_mode", ThemeMode.Dark.name) }
        verify { editor.apply() }

        viewModel.onAction(MainAction.SetThemeMode(ThemeMode.Light))
        advanceUntilIdle()

        state = viewModel.state.value
        assertEquals(ThemeMode.Light, state.themeMode)
        assertFalse("isDarkMode should be false for Light theme", state.isDarkMode)
        verify { editor.putString("theme_mode", ThemeMode.Light.name) }
        verify { editor.apply() }
    }

    @Test
    fun `network status update should reflect in viewModel state`() = runTest {
        val disconnectedStatus = NetworkStatus(
            icon = Icons.Filled.WifiOff,
            label = "No Internet",
            iconColor = Color.Red,
            isConnected = false
        )

        networkStatusFlow.value = disconnectedStatus
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(
            "NetworkStatus object should be updated",
            disconnectedStatus,
            state.networkStatus
        )
        assertFalse("isConnected boolean should be false", state.networkStatus.isConnected)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Test
    fun `onCleared should stop network monitoring and unregister receiver`() = runTest {
        val receiverSlot = slot<BroadcastReceiver>()
        every { application.registerReceiver(capture(receiverSlot), any()) } returns mockk()

        viewModel = MainViewModel(application, networkStateMonitor)

        viewModel.onCleared()
        advanceUntilIdle()

        verify { networkStateMonitor.stopMonitoring() }
        verify { application.unregisterReceiver(receiverSlot.captured) }
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