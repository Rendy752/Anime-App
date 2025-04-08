package com.example.animeapp.ui.animeSearch

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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.animeapp.R
import com.example.animeapp.ui.animeSearch.bottomSheet.FilterBottomSheet
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.animeapp.models.Genre
import com.example.animeapp.models.Producer
import com.example.animeapp.ui.animeSearch.bottomSheet.GenresBottomSheet
import com.example.animeapp.ui.animeSearch.bottomSheet.ProducersBottomSheet
import com.example.animeapp.ui.animeSearch.limitAndPagination.LimitAndPaginationSection
import com.example.animeapp.ui.animeSearch.results.ResultsSection
import com.example.animeapp.ui.animeSearch.searchField.SearchFieldSection
import com.example.animeapp.ui.animeSearch.genreProducerFilterField.GenreProducerFilterFieldSection
import com.example.animeapp.ui.main.BottomScreen
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.basicContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeSearchScreen(
    navController: NavController,
    isConnected: Boolean,
    isLandscape: Boolean,
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

    LaunchedEffect(isConnected) {
        if (!isConnected) return@LaunchedEffect

        if (genres is Resource.Error) viewModel.fetchGenres()
        if (producers is Resource.Error) viewModel.fetchProducers()
        if (animeSearchResults is Resource.Error) {
            handleInitialFetch()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchGenres()
        viewModel.fetchProducers()
        if (genre != null) {
            viewModel.setSelectedGenre(genre)
        } else if (producer != null) {
            viewModel.setSelectedProducer(producer)
        }
        handleInitialFetch()
    }

    Scaffold(
        topBar = {
            if (!isLandscape) {
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
                        },
                        actions = {
                            if (!queryState.isDefault()) Text(
                                modifier = Modifier
                                    .basicContainer(
                                        isPrimary = true,
                                        onItemClick = {
                                            viewModel.resetBottomSheetFilters()
                                        }),
                                text = "Filter Applied",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            IconButton(onClick = { isFilterBottomSheetShow = true }) {
                                Icon(
                                    imageVector = Icons.Filled.FilterList,
                                    tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = stringResource(id = R.string.filter)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            titleContentColor = MaterialTheme.colorScheme.primary
                        )
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
                if (isLandscape) {
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
                                queryState,
                                viewModel::applyFilters,
                                true,
                                isFilterBottomSheetShow,
                                viewModel::resetBottomSheetFilters
                            ) {
                                isFilterBottomSheetShow = true
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            GenreProducerFilterFieldSection(
                                selectedGenres,
                                viewModel::setSelectedGenre,
                                viewModel::applyGenreFilters,
                                selectedProducers,
                                viewModel::setSelectedProducer,
                                viewModel::applyProducerFilters,
                                isGenresBottomSheetShow,
                                isProducersBottomSheetShow,
                                setGenresBottomSheet = {
                                    isGenresBottomSheetShow = it
                                },
                                setProducersBottomSheet = { isProducersBottomSheetShow = it }
                            )
                            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                            LimitAndPaginationSection(
                                queryState,
                                animeSearchResults.data?.pagination,
                                viewModel::applyFilters,
                                false
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
                                navController,
                                animeSearchResults,
                                selectedGenres,
                                genres,
                            ) { genre ->
                                viewModel.setSelectedGenre(genre)
                                viewModel.applyGenreFilters()
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        SearchFieldSection(
                            queryState,
                            viewModel::applyFilters
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GenreProducerFilterFieldSection(
                            selectedGenres,
                            viewModel::setSelectedGenre,
                            viewModel::applyGenreFilters,
                            selectedProducers,
                            viewModel::setSelectedProducer,
                            viewModel::applyProducerFilters,
                            isGenresBottomSheetShow,
                            isProducersBottomSheetShow,
                            setGenresBottomSheet = {
                                isGenresBottomSheetShow = it
                            },
                            setProducersBottomSheet = { isProducersBottomSheetShow = it }
                        )
                        HorizontalDivider()
                        Column(modifier = Modifier.weight(1f)) {
                            ResultsSection(
                                navController,
                                animeSearchResults,
                                selectedGenres,
                                genres,
                            ) { genre ->
                                viewModel.setSelectedGenre(genre)
                                viewModel.applyGenreFilters()
                            }
                        }
                        LimitAndPaginationSection(
                            queryState,
                            animeSearchResults.data?.pagination,
                            viewModel::applyFilters
                        )
                    }
                }
            }

            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp
            val screenHeight = configuration.screenHeightDp.dp
            val bottomPadding = 48.dp
            val containerColor = MaterialTheme.colorScheme.surfaceContainer
            val shape = MaterialTheme.shapes.extraLarge

            if (isFilterBottomSheetShow) {
                ModalBottomSheet(
                    modifier = Modifier
                        .height(if (isLandscape) screenHeight * 0.95f else screenHeight * 0.6f)
                        .width(if (isLandscape) screenWidth * 0.7f else screenWidth * 0.95f)
                        .padding(bottom = if (isLandscape) 0.dp else bottomPadding)
                        .align(Alignment.BottomCenter),
                    containerColor = containerColor,
                    sheetState = sheetState,
                    onDismissRequest = { isFilterBottomSheetShow = false },
                    shape = shape
                ) {
                    Column(modifier = Modifier.clip(shape)) {
                        FilterBottomSheet(
                            queryState = queryState,
                            applyFilters = viewModel::applyFilters,
                            resetBottomSheetFilters = viewModel::resetBottomSheetFilters,
                            onDismiss = { isFilterBottomSheetShow = false }
                        )
                    }
                }
            }

            if (isGenresBottomSheetShow) {
                ModalBottomSheet(
                    modifier = Modifier
                        .height(if (isLandscape) screenHeight * 0.95f else screenHeight * 0.6f)
                        .width(if (isLandscape) screenWidth * 0.9f else screenWidth * 0.95f)
                        .padding(bottom = if (isLandscape) 0.dp else bottomPadding)
                        .align(Alignment.BottomCenter),
                    containerColor = containerColor,
                    sheetState = sheetState,
                    onDismissRequest = { isGenresBottomSheetShow = false },
                    shape = shape
                ) {
                    Column(modifier = Modifier.clip(shape)) {
                        GenresBottomSheet(
                            queryState,
                            viewModel::fetchGenres,
                            genres,
                            selectedGenres,
                            viewModel::setSelectedGenre,
                            viewModel::resetGenreSelection,
                            viewModel::applyGenreFilters,
                            onDismiss = { isGenresBottomSheetShow = false })
                    }
                }
            }

            if (isProducersBottomSheetShow) {
                ModalBottomSheet(
                    modifier = Modifier
                        .height(if (isLandscape) screenHeight * 0.95f else screenHeight * 0.6f)
                        .width(if (isLandscape) screenWidth * 0.9f else screenWidth * 0.95f)
                        .padding(bottom = if (isLandscape) 0.dp else bottomPadding)
                        .align(Alignment.BottomCenter),
                    containerColor = containerColor,
                    sheetState = sheetState,
                    onDismissRequest = { isProducersBottomSheetShow = false },
                    shape = shape
                ) {
                    Column(modifier = Modifier.clip(shape)) {
                        ProducersBottomSheet(
                            queryState,
                            producers,
                            viewModel::fetchProducers,
                            selectedProducers,
                            producersQueryState,
                            viewModel::applyProducerQueryStateFilters,
                            viewModel::setSelectedProducer,
                            viewModel::applyProducerFilters,
                            viewModel::resetProducerSelection,
                            onDismiss = { isProducersBottomSheetShow = false }
                        )
                    }
                }
            }
        }
    }
}