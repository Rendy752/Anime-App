package com.example.animeapp.animeRecommendations

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.animeapp.R
import com.example.animeapp.ui.animeRecommendations.AnimeRecommendationsScreen
import com.example.animeapp.ui.theme.AppTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class AnimeRecommendationsScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun animeRecommendationsScreen_displaysTitle() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AppTheme {
                AnimeRecommendationsScreen(navController = navController)
                val context = LocalContext.current
                composeTestRule.onNodeWithText(context.getString(R.string.title_recommendation)).assertIsDisplayed()
            }
        }
    }

    @Test
    fun animeRecommendationsScreen_displaysLoading() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AppTheme {
                AnimeRecommendationsScreen(navController = navController)
            }
        }
        composeTestRule.onNodeWithText("If you like").assertDoesNotExist()
        composeTestRule.onNodeWithText("recommended by").assertDoesNotExist()
        composeTestRule.onNodeWithText("Then you might like").assertDoesNotExist()
        composeTestRule.onNodeWithText("If you like").assertDoesNotExist()
    }

    @Test
    fun animeRecommendationsScreen_displaysErrorMessage() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AppTheme {
                AnimeRecommendationsScreen(navController = navController)
                val context = LocalContext.current
                composeTestRule.onNodeWithText(context.getString(R.string.error_loading_data)).assertDoesNotExist()
            }
        }
    }
}