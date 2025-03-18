package com.example.animeapp.ui.animeSearch.ui

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
import com.example.animeapp.ui.animeSearch.components.FilterBottomSheet
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.animeapp.ui.animeSearch.components.GenresBottomSheet
import com.example.animeapp.ui.animeSearch.components.ProducersBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeSearchScreen(navController: NavController) {
    val viewModel: AnimeSearchViewModel = hiltViewModel()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val state = rememberPullToRefreshState()
    var isFilterBottomSheetShow by remember { mutableStateOf(false) }
    var isGenresBottomSheetShow by remember { mutableStateOf(false) }
    var isProducersBottomSheetShow by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = {
            if (!isLandscape) {
                TopAppBar(
                    title = { Text(text = stringResource(id = R.string.title_search)) },
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
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
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
                                .clip(MaterialTheme.shapes.extraLarge)
                        ) {
                            SearchFieldSection(viewModel)
                            FilterBottomSheet(
                                viewModel = viewModel,
                                onDismiss = {}
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(0.5f)
                                .fillMaxHeight()
                        ) {
                            FilterFieldSection(
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
                                ResultsSection(navController, viewModel)
                            }
                            LimitAndPaginationSection(viewModel)
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        SearchFieldSection(viewModel)
                        FilterFieldSection(
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
                            ResultsSection(navController, viewModel)
                        }
                        LimitAndPaginationSection(viewModel)
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
                        .fillMaxHeight()
                        .width(if (isLandscape) screenWidth * 0.4f else screenWidth * 0.95f)
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