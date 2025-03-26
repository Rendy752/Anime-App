package com.example.animeapp.ui.animeSearch

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.Genre
import com.example.animeapp.models.Producer
import com.example.animeapp.ui.animeSearch.bottomSheet.GenresBottomSheet
import com.example.animeapp.ui.animeSearch.bottomSheet.ProducersBottomSheet
import com.example.animeapp.ui.animeSearch.limitAndPagination.LimitAndPaginationSection
import com.example.animeapp.ui.animeSearch.results.ResultsSection
import com.example.animeapp.ui.animeSearch.searchField.SearchFieldSection
import com.example.animeapp.ui.animeSearch.genreProducerFilterField.GenreProducerFilterFieldSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeSearchScreen(
    navController: NavController,
    genre: Genre? = null,
    producer: Producer? = null,
) {
    val viewModel: AnimeSearchViewModel = hiltViewModel()

    val queryState by viewModel.queryState.collectAsState()
    val animeSearchResults = viewModel.animeSearchResults.collectAsState().value
    val selectedGenres = viewModel.selectedGenres.collectAsState().value
    val genres = viewModel.genres.collectAsState().value
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val state = rememberPullToRefreshState()
    var isFilterBottomSheetShow by remember { mutableStateOf(false) }
    var isGenresBottomSheetShow by remember { mutableStateOf(false) }
    var isProducersBottomSheetShow by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(genre, producer) {
        if (genre != null) {
            viewModel.setSelectedGenre(genre)
            viewModel.applyGenreFilters()
        } else if (producer != null) {
            viewModel.setSelectedProducer(producer)
            viewModel.applyProducerFilters()
        } else {
            viewModel.searchAnime()
        }
    }

    Scaffold(
        topBar = {
            if (!isLandscape) {
                Column {
                    TopAppBar(
                        title = { Text(text = stringResource(id = R.string.title_search)) },
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
            onRefresh = { viewModel.applyFilters(viewModel.queryState.value) },
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
                                .fillMaxHeight()
                                .clip(MaterialTheme.shapes.extraLarge),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            SearchFieldSection(
                                queryState.query,
                                viewModel::applyFilters,
                                true,
                                isFilterBottomSheetShow
                            ) {
                                isFilterBottomSheetShow = true
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            GenreProducerFilterFieldSection(
                                viewModel,
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
                            queryState.query,
                            viewModel::applyFilters
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GenreProducerFilterFieldSection(
                            viewModel,
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
                            viewModel = viewModel,
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
                            viewModel,
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
                            viewModel,
                            onDismiss = { isProducersBottomSheetShow = false }
                        )
                    }
                }
            }
        }
    }
}