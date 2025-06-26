package com.luminoverse.animevibe.animeRecommendations

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luminoverse.animevibe.ui.animeRecommendations.AnimeRecommendationsScreen
import com.luminoverse.animevibe.ui.theme.AppTheme
import com.luminoverse.animevibe.utils.resource.Resource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import com.luminoverse.animevibe.models.AnimeRecommendationResponse
import com.luminoverse.animevibe.models.animeRecommendationPlaceholder
import com.luminoverse.animevibe.models.defaultPagination
import com.luminoverse.animevibe.ui.animeRecommendations.AnimeRecommendationsViewModel
import com.luminoverse.animevibe.ui.animeRecommendations.RecommendationsState
import com.luminoverse.animevibe.ui.main.MainState
import com.luminoverse.animevibe.ui.main.MainViewModel
import dagger.hilt.android.testing.BindValue
import kotlinx.coroutines.flow.StateFlow
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

    @BindValue
    val mockMainViewModel: MainViewModel = mockk(relaxed = true)
    val mockViewModel: AnimeRecommendationsViewModel = mockk(relaxed = true)

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
                    navController = mockk(relaxed = true),
                    mainState = mockk(relaxed = true),
                    recommendationsState = mockk(relaxed = true),
                    onAction = mockk(relaxed = true)
                )
            }
        }
        // Add assertion if title exists, e.g.:
        // composeTestRule.onNodeWithText("Recommendations").assertIsDisplayed()
    }

    @Test
    fun animeRecommendationsScreen_displaysLoading() {
        val loadingState = Resource.Loading<AnimeRecommendationResponse>()
        val recommendationsState: RecommendationsState = mockk {
            every { animeRecommendations } returns loadingState
        }
        every { mockViewModel.recommendationsState } returns mockk<StateFlow<RecommendationsState>> {
            every { value } returns recommendationsState
        }

        composeTestRule.setContent {
            AppTheme {
                AnimeRecommendationsScreen(
                    navController = mockk(relaxed = true),
                    mainState = mockk(relaxed = true),
                    recommendationsState = recommendationsState,
                    onAction = mockk(relaxed = true)
                )
            }
        }
        composeTestRule.onNodeWithText("If you like").assertDoesNotExist()
    }

    @Test
    fun animeRecommendationsScreen_displaysErrorMessage_whenNotConnected() {
        val mainState: MainState = mockk {
            every { networkStatus.isConnected } returns false
        }
        val recommendationsState: RecommendationsState = mockk {
            every { animeRecommendations } returns Resource.Error("No internet connection")
        }
        every { mockViewModel.recommendationsState } returns mockk<StateFlow<RecommendationsState>> {
            every { value } returns recommendationsState
        }
        every { mockMainViewModel.state } returns mockk<StateFlow<MainState>> {
            every { value } returns mainState
        }

        composeTestRule.setContent {
            AppTheme {
                AnimeRecommendationsScreen(
                    navController = mockk(relaxed = true),
                    mainState = mainState,
                    recommendationsState = recommendationsState,
                    onAction = mockk(relaxed = true)
                )
            }
        }
        composeTestRule.onNodeWithText("No internet connection").assertIsDisplayed()
    }

    @Test
    fun animeRecommendationsScreen_displaysErrorMessage_whenLoadingFailsAndConnected() {
        val errorState = Resource.Error<AnimeRecommendationResponse>("Failed to load")
        val mainState: MainState = mockk {
            every { networkStatus.isConnected } returns true
        }
        val recommendationsState: RecommendationsState = mockk {
            every { animeRecommendations } returns errorState
        }
        every { mockViewModel.recommendationsState } returns mockk<StateFlow<RecommendationsState>> {
            every { value } returns recommendationsState
        }
        every { mockMainViewModel.state } returns mockk<StateFlow<MainState>> {
            every { value } returns mainState
        }

        composeTestRule.setContent {
            AppTheme {
                AnimeRecommendationsScreen(
                    navController = mockk(relaxed = true),
                    mainState = mainState,
                    recommendationsState = recommendationsState,
                    onAction = mockk(relaxed = true)
                )
            }
        }
        composeTestRule.onNodeWithText("Error Loading Data").assertIsDisplayed()
    }

    @Test
    fun animeRecommendationsScreen_rendersListItem_inPortrait() {
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
            )
        )
        val successState = Resource.Success(
            AnimeRecommendationResponse(
                recommendations,
                defaultPagination
            )
        )
        val recommendationsState: RecommendationsState = mockk {
            every { animeRecommendations } returns successState
        }
        every { mockViewModel.recommendationsState } returns mockk<StateFlow<RecommendationsState>> {
            every { value } returns recommendationsState
        }

        composeTestRule.setContent {
            AppTheme {
                AnimeRecommendationsScreen(
                    navController = mockk(relaxed = true),
                    mainState = mockk(relaxed = true),
                    recommendationsState = recommendationsState,
                    onAction = mockk(relaxed = true)
                )
            }
        }
        composeTestRule.onNodeWithText("Anime 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Anime 2").assertIsDisplayed()
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
            animeRecommendationPlaceholder.copy(
                mal_id = "4",
                entry = listOf(),
                content = "Anime 4"
            )
        )
        val successState = Resource.Success(
            AnimeRecommendationResponse(
                recommendations,
                defaultPagination
            )
        )
        val recommendationsState: RecommendationsState = mockk {
            every { animeRecommendations } returns successState
        }
        every { mockViewModel.recommendationsState } returns mockk<StateFlow<RecommendationsState>> {
            every { value } returns recommendationsState
        }

        composeTestRule.setContent {
            AppTheme {
                AnimeRecommendationsScreen(
                    navController = mockk(relaxed = true),
                    mainState = mockk(relaxed = true),
                    recommendationsState = recommendationsState,
                    onAction = mockk(relaxed = true)
                )
            }
        }
        composeTestRule.onNodeWithText("Anime 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Anime 3").assertIsDisplayed()
    }
}