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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeSearchScreen(navController: NavController) {
    val viewModel: AnimeSearchViewModel = hiltViewModel()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val state = rememberPullToRefreshState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = {
            if (!isLandscape) {
                TopAppBar(
                    title = { Text(text = stringResource(id = R.string.title_search)) },
                    actions = {
                        IconButton(onClick = { showBottomSheet = true }) {
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
        Box(modifier = Modifier.fillMaxSize()) {
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
                if (isLandscape) {
                    Row {
                        Column(
                            modifier = Modifier
                                .weight(0.5f)
                                .clip(MaterialTheme.shapes.extraLarge)
                        ) {
                            FilterSection(viewModel, true)
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
                            Column(modifier = Modifier.weight(1f)) {
                                ResultsSection(navController, viewModel)
                            }
                            LimitAndPaginationSection(viewModel)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        FilterSection(viewModel)
                        HorizontalDivider()
                        Column(modifier = Modifier.weight(1f)) {
                            ResultsSection(navController, viewModel)
                        }
                        LimitAndPaginationSection(viewModel)
                    }
                }
            }
            if (showBottomSheet && !isLandscape) {
                val configuration = LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp.dp
                val screenHeight = configuration.screenHeightDp.dp
                val bottomSheetWidth = screenWidth * 0.9f
                val bottomSheetHeight = screenHeight * 0.6f
                val bottomSheetPadding: Dp = 48.dp
                val shape = MaterialTheme.shapes.extraLarge

                ModalBottomSheet(
                    modifier = Modifier
                        .height(bottomSheetHeight)
                        .width(bottomSheetWidth)
                        .padding(bottom = bottomSheetPadding)
                        .align(Alignment.BottomCenter),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    sheetState = sheetState,
                    onDismissRequest = { showBottomSheet = false },
                    shape = shape
                ) {
                    Column(modifier = Modifier.clip(shape)) {
                        FilterBottomSheet(
                            viewModel = viewModel,
                            onDismiss = { showBottomSheet = false })
                    }
                }
            }
        }
    }
}