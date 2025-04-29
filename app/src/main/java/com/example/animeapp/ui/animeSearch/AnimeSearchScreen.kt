package com.example.animeapp.ui.animeSearch

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.animeapp.ui.animeSearch.bottomSheet.FilterBottomSheet
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.animeapp.models.Genre
import com.example.animeapp.models.Producer
import com.example.animeapp.ui.animeSearch.bottomSheet.GenresBottomSheet
import com.example.animeapp.ui.animeSearch.bottomSheet.ProducersBottomSheet
import com.example.animeapp.ui.common_ui.LimitAndPaginationSection
import com.example.animeapp.ui.animeSearch.results.ResultsSection
import com.example.animeapp.ui.animeSearch.searchField.SearchFieldSection
import com.example.animeapp.ui.animeSearch.genreProducerFilterField.GenreProducerFilterFieldSection
import com.example.animeapp.ui.common_ui.LimitAndPaginationQueryState
import com.example.animeapp.ui.main.components.BottomScreen
import com.example.animeapp.ui.main.MainState
import com.example.animeapp.utils.Resource

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun AnimeSearchScreen(
    navController: NavHostController = rememberNavController(),
    mainState: MainState = MainState(),
    genre: Genre? = null,
    producer: Producer? = null,
) {
    val viewModel: AnimeSearchViewModel = hiltViewModel()

    val queryState by viewModel.queryState.collectAsStateWithLifecycle()
    val animeSearchResults by viewModel.animeSearchResults.collectAsStateWithLifecycle()

    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val selectedGenres by viewModel.selectedGenres.collectAsStateWithLifecycle()

    val producers by viewModel.producers.collectAsStateWithLifecycle()
    val selectedProducers by viewModel.selectedProducers.collectAsStateWithLifecycle()
    val producersQueryState by viewModel.producersQueryState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val state = rememberPullToRefreshState()
    var isFilterBottomSheetShow by remember { mutableStateOf(false) }
    var isGenresBottomSheetShow by remember { mutableStateOf(false) }
    var isProducersBottomSheetShow by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun handleInitialFetch() {
        if (genre != null) {
            viewModel.applyGenreFilters()
        } else if (producer != null) {
            viewModel.applyProducerFilters()
        } else if (animeSearchResults.data == null) viewModel.searchAnime()
    }

    LaunchedEffect(mainState.isConnected) {
        if (!mainState.isConnected) return@LaunchedEffect

        if (genres is Resource.Error) viewModel.fetchGenres()
        if (producers is Resource.Error) viewModel.fetchProducers()
        if (animeSearchResults is Resource.Error) {
            handleInitialFetch()
        }
    }

    LaunchedEffect(Unit) {
        if (genres !is Resource.Success) viewModel.fetchGenres()
        if (producers !is Resource.Success) viewModel.fetchProducers()
        if (genre != null) {
            viewModel.setSelectedGenre(genre)
        } else if (producer != null) {
            viewModel.setSelectedProducer(producer)
        }
        handleInitialFetch()
    }

    Scaffold(
        topBar = {
            if (!mainState.isLandscape) {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = BottomScreen.Search.label,
                                modifier = Modifier.padding(end = 8.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            if (genre != null || producer != null) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
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
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.applyFilters(queryState) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = state,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    isRefreshing = isRefreshing,
                    containerColor = MaterialTheme.colorScheme.primary,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = state
                )
            },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (mainState.isLandscape) {
                    Row {
                        Column(
                            modifier = Modifier
                                .weight(0.5f)
                                .verticalScroll(rememberScrollState())
                                .fillMaxHeight()
                                .clip(MaterialTheme.shapes.extraLarge),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            SearchFieldSection(
                                queryState = queryState,
                                onQueryChanged = viewModel::applyFilters,
                                isFilterBottomSheetShow = isFilterBottomSheetShow,
                                resetBottomSheetFilters = viewModel::resetBottomSheetFilters,
                                onFilterClick = { isFilterBottomSheetShow = true }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            GenreProducerFilterFieldSection(
                                selectedGenres = selectedGenres,
                                setSelectedGenre = viewModel::setSelectedGenre,
                                applyGenreFilters = viewModel::applyGenreFilters,
                                selectedProducers = selectedProducers,
                                setSelectedProducer = viewModel::setSelectedProducer,
                                applyProducerFilters = viewModel::applyProducerFilters,
                                isGenresBottomSheetShow = isGenresBottomSheetShow,
                                isProducersBottomSheetShow = isProducersBottomSheetShow,
                                setGenresBottomSheet = { isGenresBottomSheetShow = it },
                                setProducersBottomSheet = { isProducersBottomSheetShow = it }
                            )
                            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                            LimitAndPaginationSection(
                                isVisible = animeSearchResults is Resource.Success,
                                pagination = animeSearchResults.data?.pagination,
                                query = LimitAndPaginationQueryState(
                                    queryState.page,
                                    queryState.limit
                                ),
                                onQueryChanged = {
                                    viewModel.applyFilters(
                                        queryState.copy(
                                            page = it.page,
                                            limit = it.limit
                                        )
                                    )
                                },
                                useHorizontalPager = false
                            )
                        }
                        VerticalDivider()
                        Box(
                            modifier = Modifier
                                .weight(0.5f)
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            ResultsSection(
                                navController = navController,
                                animeSearchResults = animeSearchResults,
                                selectedGenres = selectedGenres,
                                genres = genres,
                                onGenreClick = { genre ->
                                    viewModel.setSelectedGenre(genre)
                                    viewModel.applyGenreFilters()
                                }
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        SearchFieldSection(
                            queryState = queryState,
                            onQueryChanged = viewModel::applyFilters,
                            isFilterBottomSheetShow = isFilterBottomSheetShow,
                            resetBottomSheetFilters = viewModel::resetBottomSheetFilters,
                            onFilterClick = { isFilterBottomSheetShow = true }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GenreProducerFilterFieldSection(
                            selectedGenres = selectedGenres,
                            setSelectedGenre = viewModel::setSelectedGenre,
                            applyGenreFilters = viewModel::applyGenreFilters,
                            selectedProducers = selectedProducers,
                            setSelectedProducer = viewModel::setSelectedProducer,
                            applyProducerFilters = viewModel::applyProducerFilters,
                            isGenresBottomSheetShow = isGenresBottomSheetShow,
                            isProducersBottomSheetShow = isProducersBottomSheetShow,
                            setGenresBottomSheet = { isGenresBottomSheetShow = it },
                            setProducersBottomSheet = { isProducersBottomSheetShow = it }
                        )
                        HorizontalDivider()
                        Column(modifier = Modifier.weight(1f)) {
                            ResultsSection(
                                navController = navController,
                                animeSearchResults = animeSearchResults,
                                selectedGenres = selectedGenres,
                                genres = genres,
                                onGenreClick = { genre ->
                                    viewModel.setSelectedGenre(genre)
                                    viewModel.applyGenreFilters()
                                }
                            )
                        }
                        LimitAndPaginationSection(
                            isVisible = animeSearchResults is Resource.Success,
                            pagination = animeSearchResults.data?.pagination,
                            query = LimitAndPaginationQueryState(
                                queryState.page,
                                queryState.limit
                            ),
                            onQueryChanged = {
                                viewModel.applyFilters(
                                    queryState.copy(
                                        page = it.page,
                                        limit = it.limit
                                    )
                                )
                            }
                        )
                    }
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
                        queryState = queryState,
                        applyFilters = viewModel::applyFilters,
                        resetBottomSheetFilters = viewModel::resetBottomSheetFilters,
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
                    containerColor = containerColor,
                    sheetState = sheetState,
                    onDismissRequest = { isGenresBottomSheetShow = false },
                    shape = shape,
                    contentWindowInsets = { WindowInsets(0.dp) }
                ) {
                    Column(modifier = Modifier.clip(shape)) {
                        GenresBottomSheet(
                            queryState = queryState,
                            fetchGenres = viewModel::fetchGenres,
                            genres = genres,
                            selectedGenres = selectedGenres,
                            setSelectedGenre = viewModel::setSelectedGenre,
                            resetGenreSelection = viewModel::resetGenreSelection,
                            applyGenreFilters = viewModel::applyGenreFilters,
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
                    containerColor = containerColor,
                    sheetState = sheetState,
                    onDismissRequest = { isProducersBottomSheetShow = false },
                    shape = shape,
                    contentWindowInsets = { WindowInsets(0.dp) }
                ) {
                    Column(modifier = Modifier.clip(shape)) {
                        ProducersBottomSheet(
                            queryState = queryState,
                            producers = producers,
                            fetchProducers = viewModel::fetchProducers,
                            selectedProducers = selectedProducers,
                            producersQueryState = producersQueryState,
                            applyProducerQueryStateFilters = viewModel::applyProducerQueryStateFilters,
                            setSelectedProducer = viewModel::setSelectedProducer,
                            applyProducerFilters = viewModel::applyProducerFilters,
                            resetProducerSelection = viewModel::resetProducerSelection,
                            onDismiss = { isProducersBottomSheetShow = false }
                        )
                    }
                }
            }
        }
    }
}