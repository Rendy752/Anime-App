package com.example.animeapp.main

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Observer
import com.example.animeapp.models.NetworkStatus
import com.example.animeapp.ui.main.MainAction
import com.example.animeapp.ui.main.MainViewModel
import com.example.animeapp.ui.theme.ColorStyle
import com.example.animeapp.ui.theme.ContrastMode
import com.example.animeapp.utils.NetworkStateMonitor
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

    private val mockNetworkStatus = NetworkStatus(
        icon = Icons.Filled.Wifi,
        label = "10000 Kbps",
        color = Color.Green
    )

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        application = mockk(relaxed = true)
        themePrefs = mockk(relaxed = true)
        settingsPrefs = mockk(relaxed = true)
        networkStateMonitor = mockk(relaxed = true)

        every { application.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE) } returns themePrefs
        every { application.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE) } returns settingsPrefs

        every { themePrefs.getBoolean("is_dark_mode", false) } returns false
        every { themePrefs.getString("contrast_mode", ContrastMode.Normal.name) } returns ContrastMode.Normal.name
        every { themePrefs.getString("color_style", ColorStyle.Default.name) } returns ColorStyle.Default.name
        every { settingsPrefs.getBoolean("notifications_enabled", false) } returns false

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

    @Test
    fun `init should load preferences and start network monitoring`() = runTest {
        val state = viewModel.state.value
        assertEquals(false, state.isDarkMode)
        assertEquals(ContrastMode.Normal, state.contrastMode)
        assertEquals(ColorStyle.Default, state.colorStyle)
        assertEquals(true, state.isConnected)
        assertEquals(mockNetworkStatus, state.networkStatus)
        verify { networkStateMonitor.startMonitoring(application) }
    }

    @Test
    fun `SetDarkMode should update isDarkMode and persist to SharedPreferences`() = runTest {
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { themePrefs.edit() } returns editor

        viewModel.dispatch(MainAction.SetDarkMode(true))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isDarkMode)
        verify { editor.putBoolean("is_dark_mode", true) }
        verify { editor.apply() }
    }

    @Test
    fun `SetContrastMode should update contrastMode and persist to SharedPreferences`() = runTest {
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { themePrefs.edit() } returns editor

        viewModel.dispatch(MainAction.SetContrastMode(ContrastMode.High))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ContrastMode.High, state.contrastMode)
        verify { editor.putString("contrast_mode", ContrastMode.High.name) }
        verify { editor.apply() }
    }

    @Test
    fun `SetColorStyle should update colorStyle and persist to SharedPreferences`() = runTest {
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { themePrefs.edit() } returns editor

        viewModel.dispatch(MainAction.SetColorStyle(ColorStyle.Monochrome))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ColorStyle.Monochrome, state.colorStyle)
        verify { editor.putString("color_style", ColorStyle.Monochrome.name) }
        verify { editor.apply() }
    }

    @Test
    fun `SetNotificationEnabled should update notificationEnabled and persist to SharedPreferences`() = runTest {
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { settingsPrefs.edit() } returns editor

        viewModel.dispatch(MainAction.SetNotificationEnabled(true))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.notificationEnabled)
        verify { editor.putBoolean("notifications_enabled", true) }
        verify { editor.apply() }
    }

    @Test
    fun `SetShowQuitDialog should update showQuitDialog`() = runTest {
        viewModel.dispatch(MainAction.SetShowQuitDialog(true))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.showQuitDialog)
    }

    @Test
    fun `SetIsConnected should update isConnected`() = runTest {
        viewModel.dispatch(MainAction.SetIsConnected(false))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(false, state.isConnected)
    }

    @Test
    fun `SetNetworkStatus should update networkStatus`() = runTest {
        val newStatus = NetworkStatus(
            icon = Icons.Filled.WifiOff,
            label = "No Internet",
            color = Color.Red
        )

        viewModel.dispatch(MainAction.SetNetworkStatus(newStatus))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(newStatus, state.networkStatus)
    }

    @Test
    fun `SetIsShowIdleDialog should update isShowIdleDialog`() = runTest {
        viewModel.dispatch(MainAction.SetIsShowIdleDialog(true))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isShowIdleDialog)
    }

    @Test
    fun `onCleared should stop network monitoring`() = runTest {
        viewModel.onCleared()
        advanceUntilIdle()

        verify { networkStateMonitor.stopMonitoring() }
    }

    @Test
    fun `NetworkStateMonitor should update state when network becomes unavailable`() = runTest {
        val isConnectedObserver = slot<Observer<Boolean>>()
        val networkStatusObserver = slot<Observer<NetworkStatus>>()
        every { networkStateMonitor.isConnected.observeForever(capture(isConnectedObserver)) } answers {
            isConnectedObserver.captured.onChanged(false)
        }
        every { networkStateMonitor.networkStatus.observeForever(capture(networkStatusObserver)) } answers {
            networkStatusObserver.captured.onChanged(
                NetworkStatus(
                    icon = Icons.Filled.WifiOff,
                    label = "No Internet",
                    color = Color.Red
                )
            )
        }

        viewModel = MainViewModel(application, networkStateMonitor)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(false, state.isConnected)
        assertEquals(
            NetworkStatus(
                icon = Icons.Filled.WifiOff,
                label = "No Internet",
                color = Color.Red
            ),
            state.networkStatus
        )
        verify { networkStateMonitor.startMonitoring(application) }
    }
}