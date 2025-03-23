package com.example.animeapp.ui.animeWatch.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.ui.common_ui.AnimeHeader
import com.example.animeapp.ui.common_ui.DetailCommonBody
import com.example.animeapp.ui.common_ui.YoutubePreview
import com.example.animeapp.utils.basicContainer

@Composable
fun ContentSection(
    animeDetail: AnimeDetail,
    scrollState: LazyListState,
    selectedContentIndex: Int,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.padding(8.dp), state = scrollState) {
        item {
            if (selectedContentIndex == 0) {
                Column(
                    modifier = Modifier
                        .basicContainer()
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Video")
                    }
                }
            } else {
                AnimeHeader(animeDetail)
                YoutubePreview(animeDetail.trailer.embed_url)
                DetailCommonBody("Synopsis", animeDetail.synopsis)
            }
        }
    }
}