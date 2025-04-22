package com.example.animeapp.animeRecommendations

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.animeapp.R
import com.example.animeapp.ui.animeRecommendations.AnimeRecommendationsScreen
import com.example.animeapp.ui.main.BottomScreen
import com.example.animeapp.ui.theme.AppTheme
import com.example.animeapp.utils.Resource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import com.example.animeapp.models.AnimeRecommendationResponse
import com.example.animeapp.models.animeRecommendationPlaceholder
import com.example.animeapp.models.defaultPagination
import com.example.animeapp.ui.animeRecommendations.AnimeRecommendationsViewModel
import dagger.hilt.android.testing.BindValue
import kotlinx.coroutines.flow.MutableStateFlow

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class AnimeRecommendationsScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @BindValue
    val mockViewModel: AnimeRecommendationsViewModel = mock()

    private lateinit var navController: NavController

    @Before
    fun setup() {
        hiltRule.inject()
        composeTestRule.setContent {
            navController = rememberNavController()
        }
    }

    @Test
    fun animeRecommendationsScreen_displaysTitle() {
        composeTestRule.setContent {
            AppTheme {
                AnimeRecommendationsScreen(
                    navController = navController,
                    isConnected = true,
                    isLandscape = false
                )
                composeTestRule.onNodeWithText(BottomScreen.Recommendations.label)
                    .assertIsDisplayed()
            }
        }
    }

    @Test
    fun animeRecommendationsScreen_displaysLoading() {
        val loadingState =
            MutableStateFlow<Resource<AnimeRecommendationResponse>>(Resource.Loading())
        whenever(mockViewModel.animeRecommendations).thenReturn(loadingState)

        composeTestRule.setContent {
            AppTheme {
                AnimeRecommendationsScreen(
                    navController = navController,
                    isConnected = true,
                    isLandscape = false
                )
                composeTestRule.onNodeWithText("If you like").assertDoesNotExist()
            }
        }
    }

    @Test
    fun animeRecommendationsScreen_displaysErrorMessage_whenNotConnected() {
        composeTestRule.setContent {
            val currentContext = LocalContext.current
            AppTheme {
                AnimeRecommendationsScreen(
                    navController = navController,
                    isConnected = false,
                    isLandscape = false
                )
                composeTestRule.onNodeWithText(currentContext.getString(R.string.no_internet_connection))
                    .assertIsDisplayed()
            }
        }
    }

    @Test
    fun animeRecommendationsScreen_displaysErrorMessage_whenLoadingFailsAndConnected() {
        val errorState =
            MutableStateFlow<Resource<AnimeRecommendationResponse>>(Resource.Error("Failed to load"))
        whenever(mockViewModel.animeRecommendations).thenReturn(errorState)

        composeTestRule.setContent {
            val currentContext = LocalContext.current
            AppTheme {
                AnimeRecommendationsScreen(
                    navController = navController,
                    isConnected = true,
                    isLandscape = false
                )
                composeTestRule.onNodeWithText(currentContext.getString(R.string.error_loading_data))
                    .assertIsDisplayed()
            }
        }
    }

    @Test
    fun animeRecommendationsScreen_rendersListItem_inPortrait() {
        val recommendations = listOf(
            animeRecommendationPlaceholder.copy(
                mal_id = "1",
                entry = listOf(),
                content = "Anime 1"
            ),
            animeRecommendationPlaceholder.copy(mal_id = "2", entry = listOf(), content = "Anime 2")
        )
        val successState =
            MutableStateFlow(
                Resource.Success(
                    AnimeRecommendationResponse(
                        recommendations,
                        defaultPagination
                    )
                )
            )
        whenever(mockViewModel.animeRecommendations).thenReturn(successState)

        composeTestRule.setContent {
            AppTheme {
                AnimeRecommendationsScreen(
                    navController = navController,
                    isConnected = true,
                    isLandscape = false
                )
                composeTestRule.onNodeWithText("Anime 1").assertIsDisplayed()
                composeTestRule.onNodeWithText("Anime 2").assertIsDisplayed()
            }
        }
    }

    @Test
    fun animeRecommendationsScreen_rendersTwoColumns_inLandscape() {
        val recommendations = listOf(
            animeRecommendationPlaceholder.copy(
                mal_id = "1",
                entry = listOf(),
                content = "Anime 1"
            ),
            animeRecommendationPlaceholder.copy(
                mal_id = "2",
                entry = listOf(),
                content = "Anime 2"
            ),
            animeRecommendationPlaceholder.copy(
                mal_id = "3",
                entry = listOf(),
                content = "Anime 3"
            ),
            animeRecommendationPlaceholder.copy(mal_id = "4", entry = listOf(), content = "Anime 4")
        )
        val successState =
            MutableStateFlow(
                Resource.Success(
                    AnimeRecommendationResponse(
                        recommendations,
                        defaultPagination
                    )
                )
            )
        whenever(mockViewModel.animeRecommendations).thenReturn(successState)

        composeTestRule.setContent {
            AppTheme {
                AnimeRecommendationsScreen(
                    navController = navController,
                    isConnected = true,
                    isLandscape = true
                )
            }
        }

        composeTestRule.onNodeWithText("Anime 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Anime 3").assertIsDisplayed()
    }
}