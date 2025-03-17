package com.example.animeapp.ui.animeSearch.ui

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
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeSearchScreen(navController: NavController) {
    val viewModel: AnimeSearchViewModel = hiltViewModel()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val state = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.title_search)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
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
}