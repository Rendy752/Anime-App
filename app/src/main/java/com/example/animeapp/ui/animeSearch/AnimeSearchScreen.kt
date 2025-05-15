package com.example.animeapp.ui.animeSearch

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.animeapp.ui.animeSearch.bottomSheet.FilterBottomSheet
import com.example.animeapp.ui.animeSearch.bottomSheet.GenresBottomSheet
import com.example.animeapp.ui.animeSearch.bottomSheet.ProducersBottomSheet
import com.example.animeapp.ui.animeSearch.searchField.GenreProducerFilterFieldSection
import com.example.animeapp.ui.animeSearch.results.ResultsSection
import com.example.animeapp.ui.animeSearch.searchField.SearchFieldSection
import com.example.animeapp.ui.common_ui.LimitAndPaginationQueryState
import com.example.animeapp.ui.common_ui.LimitAndPaginationSection
import com.example.animeapp.ui.main.MainState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.animeapp.utils.Resource

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun AnimeSearchScreen(
    navController: NavHostController = rememberNavController(),
    mainState: MainState = MainState(),
    genreId: Int? = null,
    producerId: Int? = null,
    searchState: SearchState = SearchState(),
    filterSelectionState: FilterSelectionState = FilterSelectionState(),
    onAction: (SearchAction) -> Unit = {}
) {
    val state = rememberPullToRefreshState()
    val leftFilterScrollState = rememberScrollState()
    val resultsSectionScrollState = rememberLazyListState()
    var isFilterBottomSheetShow by remember { mutableStateOf(false) }
    var isGenresBottomSheetShow by remember { mutableStateOf(false) }
    var isProducersBottomSheetShow by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(mainState.isConnected) {
        if (!mainState.isConnected) return@LaunchedEffect

        if (searchState.genres is Resource.Error) onAction(SearchAction.FetchGenres)
        if (searchState.producers is Resource.Error) onAction(SearchAction.FetchProducers)
        if (searchState.animeSearchResults is Resource.Error) {
            onAction(SearchAction.HandleInitialFetch(genreId = genreId, producerId = producerId))
        }
    }

    LaunchedEffect(Unit) {
        onAction(SearchAction.HandleInitialFetch(genreId, producerId))
    }

    Scaffold(
        topBar = {
            if (!mainState.isLandscape && (genreId != null || producerId != null)) {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Search",
                                modifier = Modifier.padding(end = 8.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        thickness = 2.dp
                    )
                }
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = searchState.isRefreshing,
            onRefresh = { onAction(SearchAction.ApplyFilters(searchState.queryState)) },
            modifier = Modifier.padding(paddingValues),
            state = state,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    isRefreshing = searchState.isRefreshing,
                    containerColor = MaterialTheme.colorScheme.primary,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = state
                )
            }
        ) {
            if (mainState.isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxHeight()
                            .verticalScroll(leftFilterScrollState),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        SearchFieldSection(
                            queryState = searchState.queryState,
                            onQueryChanged = { updatedQueryState ->
                                onAction(SearchAction.ApplyFilters(updatedQueryState))
                            },
                            isFilterBottomSheetShow = isFilterBottomSheetShow,
                            resetBottomSheetFilters = { onAction(SearchAction.ResetBottomSheetFilters) },
                            onFilterClick = { isFilterBottomSheetShow = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        GenreProducerFilterFieldSection(
                            selectedGenres = filterSelectionState.selectedGenres,
                            setSelectedGenre = { genre ->
                                onAction(SearchAction.SetSelectedGenre(genre))
                            },
                            applyGenreFilters = { onAction(SearchAction.ApplyGenreFilters) },
                            selectedProducers = filterSelectionState.selectedProducers,
                            setSelectedProducer = { producer ->
                                onAction(SearchAction.SetSelectedProducer(producer))
                            },
                            applyProducerFilters = { onAction(SearchAction.ApplyProducerFilters) },
                            isGenresBottomSheetShow = isGenresBottomSheetShow,
                            isProducersBottomSheetShow = isProducersBottomSheetShow,
                            setGenresBottomSheet = { isGenresBottomSheetShow = it },
                            setProducersBottomSheet = { isProducersBottomSheetShow = it }
                        )
                        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                        LimitAndPaginationSection(
                            isVisible = searchState.animeSearchResults is Resource.Success,
                            pagination = searchState.animeSearchResults.data?.pagination,
                            query = LimitAndPaginationQueryState(
                                searchState.queryState.page,
                                searchState.queryState.limit
                            ),
                            onQueryChanged = { updatedQuery ->
                                onAction(
                                    SearchAction.ApplyFilters(
                                        searchState.queryState.copy(
                                            page = updatedQuery.page,
                                            limit = updatedQuery.limit
                                        )
                                    )
                                )
                            },
                            useHorizontalPager = false
                        )
                    }

                    VerticalDivider()

                    ResultsSection(
                        modifier = Modifier.weight(0.5f),
                        resultsSectionScrollState = resultsSectionScrollState,
                        navController = navController,
                        query = searchState.queryState.query,
                        animeSearchResults = searchState.animeSearchResults,
                        selectedGenres = filterSelectionState.selectedGenres,
                        genres = searchState.genres,
                        onGenreClick = { genre ->
                            onAction(SearchAction.SetSelectedGenre(genre))
                            onAction(SearchAction.ApplyGenreFilters)
                        }
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    SearchFieldSection(
                        queryState = searchState.queryState,
                        onQueryChanged = { updatedQueryState ->
                            onAction(SearchAction.ApplyFilters(updatedQueryState))
                        },
                        isFilterBottomSheetShow = isFilterBottomSheetShow,
                        resetBottomSheetFilters = { onAction(SearchAction.ResetBottomSheetFilters) },
                        onFilterClick = { isFilterBottomSheetShow = true }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    GenreProducerFilterFieldSection(
                        selectedGenres = filterSelectionState.selectedGenres,
                        setSelectedGenre = { genre ->
                            onAction(SearchAction.SetSelectedGenre(genre))
                        },
                        applyGenreFilters = { onAction(SearchAction.ApplyGenreFilters) },
                        selectedProducers = filterSelectionState.selectedProducers,
                        setSelectedProducer = { producer ->
                            onAction(SearchAction.SetSelectedProducer(producer))
                        },
                        applyProducerFilters = { onAction(SearchAction.ApplyProducerFilters) },
                        isGenresBottomSheetShow = isGenresBottomSheetShow,
                        isProducersBottomSheetShow = isProducersBottomSheetShow,
                        setGenresBottomSheet = { isGenresBottomSheetShow = it },
                        setProducersBottomSheet = { isProducersBottomSheetShow = it }
                    )

                    ResultsSection(
                        modifier = Modifier.weight(1f),
                        resultsSectionScrollState = resultsSectionScrollState,
                        navController = navController,
                        query = searchState.queryState.query,
                        animeSearchResults = searchState.animeSearchResults,
                        selectedGenres = filterSelectionState.selectedGenres,
                        genres = searchState.genres,
                        onGenreClick = { genre ->
                            onAction(SearchAction.SetSelectedGenre(genre))
                            onAction(SearchAction.ApplyGenreFilters)
                        }
                    )

                    LimitAndPaginationSection(
                        isVisible = searchState.animeSearchResults is Resource.Success,
                        pagination = searchState.animeSearchResults.data?.pagination,
                        query = LimitAndPaginationQueryState(
                            searchState.queryState.page,
                            searchState.queryState.limit
                        ),
                        onQueryChanged = { updatedQuery ->
                            onAction(
                                SearchAction.ApplyFilters(
                                    searchState.queryState.copy(
                                        page = updatedQuery.page,
                                        limit = updatedQuery.limit
                                    )
                                )
                            )
                        }
                    )
                }
            }

            val configuration = LocalConfiguration.current
            val bottomSheetWidthFraction = if (mainState.isLandscape) 0.7f else 0.95f
            val bottomSheetHeightFraction = if (mainState.isLandscape) 0.9f else 0.6f
            val containerColor = MaterialTheme.colorScheme.surfaceContainer
            val bottomPadding = 48.dp
            val shape = MaterialTheme.shapes.extraLarge

            if (isFilterBottomSheetShow) {
                ModalBottomSheet(
                    modifier = Modifier
                        .height((configuration.screenHeightDp * bottomSheetHeightFraction).dp)
                        .width((configuration.screenWidthDp * bottomSheetWidthFraction).dp)
                        .padding(bottom = if (mainState.isLandscape) 0.dp else bottomPadding)
                        .align(Alignment.BottomCenter),
                    sheetGesturesEnabled = false,
                    containerColor = containerColor,
                    sheetState = sheetState,
                    onDismissRequest = { isFilterBottomSheetShow = false },
                    shape = shape,
                    contentWindowInsets = { WindowInsets(0.dp) }
                ) {
                    FilterBottomSheet(
                        queryState = searchState.queryState,
                        applyFilters = { updatedQueryState ->
                            onAction(SearchAction.ApplyFilters(updatedQueryState))
                        },
                        resetBottomSheetFilters = { onAction(SearchAction.ResetBottomSheetFilters) },
                        onDismiss = { isFilterBottomSheetShow = false }
                    )
                }
            }

            if (isGenresBottomSheetShow) {
                ModalBottomSheet(
                    modifier = Modifier
                        .height((configuration.screenHeightDp * bottomSheetHeightFraction).dp)
                        .width((configuration.screenWidthDp * bottomSheetWidthFraction).dp)
                        .padding(bottom = if (mainState.isLandscape) 0.dp else bottomPadding)
                        .align(Alignment.BottomCenter),
                    sheetGesturesEnabled = false,
                    containerColor = containerColor,
                    sheetState = sheetState,
                    onDismissRequest = { isGenresBottomSheetShow = false },
                    shape = shape,
                    contentWindowInsets = { WindowInsets(0.dp) }
                ) {
                    Column(modifier = Modifier.clip(shape)) {
                        GenresBottomSheet(
                            queryState = searchState.queryState,
                            fetchGenres = { onAction(SearchAction.FetchGenres) },
                            genres = searchState.genres,
                            selectedGenres = filterSelectionState.selectedGenres,
                            setSelectedGenre = { genre ->
                                onAction(SearchAction.SetSelectedGenre(genre))
                            },
                            resetGenreSelection = { onAction(SearchAction.ResetGenreSelection) },
                            applyGenreFilters = { onAction(SearchAction.ApplyGenreFilters) },
                            onDismiss = { isGenresBottomSheetShow = false }
                        )
                    }
                }
            }

            if (isProducersBottomSheetShow) {
                ModalBottomSheet(
                    modifier = Modifier
                        .height((configuration.screenHeightDp * bottomSheetHeightFraction).dp)
                        .width((configuration.screenWidthDp * bottomSheetWidthFraction).dp)
                        .padding(bottom = if (mainState.isLandscape) 0.dp else bottomPadding)
                        .align(Alignment.BottomCenter),
                    sheetGesturesEnabled = false,
                    containerColor = containerColor,
                    sheetState = sheetState,
                    onDismissRequest = { isProducersBottomSheetShow = false },
                    shape = shape,
                    contentWindowInsets = { WindowInsets(0.dp) }
                ) {
                    Column(modifier = Modifier.clip(shape)) {
                        ProducersBottomSheet(
                            queryState = searchState.queryState,
                            producers = searchState.producers,
                            fetchProducers = { onAction(SearchAction.FetchProducers) },
                            selectedProducers = filterSelectionState.selectedProducers,
                            producersQueryState = searchState.producersQueryState,
                            applyProducerQueryStateFilters = { updatedQueryState ->
                                onAction(
                                    SearchAction.ApplyProducerQueryStateFilters(updatedQueryState)
                                )
                            },
                            setSelectedProducer = { producer ->
                                onAction(SearchAction.SetSelectedProducer(producer))
                            },
                            applyProducerFilters = { onAction(SearchAction.ApplyProducerFilters) },
                            resetProducerSelection = { onAction(SearchAction.ResetProducerSelection) },
                            onDismiss = { isProducersBottomSheetShow = false }
                        )
                    }
                }
            }
        }
    }
}