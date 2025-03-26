package com.example.animeapp.ui.animeWatch.infoContent

import androidx.compose.runtime.Composable
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.ui.common_ui.AnimeHeader
import com.example.animeapp.ui.common_ui.DetailCommonBody
import com.example.animeapp.ui.common_ui.YoutubePreview

@Composable
fun InfoContentSection(
    animeDetail: AnimeDetail
) {
    AnimeHeader(animeDetail)
    YoutubePreview(animeDetail.trailer.embed_url)
    DetailCommonBody("Synopsis", animeDetail.synopsis)
}