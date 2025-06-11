package com.luminoverse.animevibe.ui.animeSearch

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.luminoverse.animevibe.ui.animeSearch.bottomSheet.FilterBottomSheet
import com.luminoverse.animevibe.ui.animeSearch.bottomSheet.GenresBottomSheet
import com.luminoverse.animevibe.ui.animeSearch.bottomSheet.ProducersBottomSheet
import com.luminoverse.animevibe.ui.animeSearch.searchField.GenreProducerFilterFieldSection
import com.luminoverse.animevibe.ui.animeSearch.results.ResultsSection
import com.luminoverse.animevibe.ui.animeSearch.searchField.SearchFieldSection
import com.luminoverse.animevibe.ui.common.LimitAndPaginationQueryState
import com.luminoverse.animevibe.ui.common.LimitAndPaginationSection
import com.luminoverse.animevibe.ui.main.MainState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.luminoverse.animevibe.ui.common.CustomModalBottomSheet
import com.luminoverse.animevibe.utils.resource.Resource

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

            CustomModalBottomSheet(
                modifier = Modifier.align(Alignment.BottomCenter),
                isVisible = isFilterBottomSheetShow,
                isLandscape = mainState.isLandscape,
                onDismiss = { isFilterBottomSheetShow = false },
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

            CustomModalBottomSheet(
                modifier = Modifier.align(Alignment.BottomCenter),
                isVisible = isGenresBottomSheetShow,
                isLandscape = mainState.isLandscape,
                onDismiss = { isGenresBottomSheetShow = false },
            ) {
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

            CustomModalBottomSheet(
                modifier = Modifier.align(Alignment.BottomCenter),
                isVisible = isProducersBottomSheetShow,
                isLandscape = mainState.isLandscape,
                onDismiss = { isProducersBottomSheetShow = false },
            ) {
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