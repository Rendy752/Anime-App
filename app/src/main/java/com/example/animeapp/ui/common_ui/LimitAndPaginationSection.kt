package com.example.animeapp.ui.common_ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.CompletePagination
import com.example.animeapp.ui.animeSearch.components.PaginationButtons
import com.example.animeapp.utils.Limit

data class LimitAndPaginationQueryState(
    val page: Int = 1,
    val limit: Int? = 25
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LimitAndPaginationSection(
    isVisible: Boolean,
    pagination: CompletePagination?,
    query: LimitAndPaginationQueryState,
    onQueryChanged: (LimitAndPaginationQueryState) -> Unit,
    useHorizontalPager: Boolean = true
) {
    var selectedLimit by remember { mutableIntStateOf(query.limit ?: 10) }

    if (useHorizontalPager) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 1000, easing = EaseInOut)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 1000, easing = EaseInOut)
            )
        ) {
            HorizontalPager(
                state = rememberPagerState(pageCount = { 2 }),
                Modifier.padding(8.dp)
            ) { page ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (page == 0) {
                        pagination?.let {
                            PaginationButtons(it) { onQueryChanged(query.copy(page = it)) }
                        }
                    } else if (page == 1) {
                        DropdownInputField(
                            label = "Limit",
                            options = Limit.limitOptions.map { it.toString() },
                            selectedValue = selectedLimit.toString(),
                            onValueChange = {
                                selectedLimit = it.toInt()
                                onQueryChanged(query.copy(limit = it.toInt(), page = 1))
                            },
                            modifier = Modifier.wrapContentSize()
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DropdownInputField(
                    label = "Limit",
                    options = Limit.limitOptions.map { it.toString() },
                    selectedValue = selectedLimit.toString(),
                    onValueChange = {
                        selectedLimit = it.toInt()
                        onQueryChanged(query.copy(limit = it.toInt(), page = 1))
                    },
                    modifier = Modifier.wrapContentSize()
                )
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 1000, easing = EaseInOut)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 1000, easing = EaseInOut)
                )
            ) {
                pagination?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PaginationButtons(it) { onQueryChanged(query.copy(page = it)) }
                    }
                }
            }
        }
    }
}
