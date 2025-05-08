package com.example.animeapp.ui.animeWatch.infoContent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.ui.common_ui.AnimeHeader
import com.example.animeapp.ui.common_ui.AnimeHeaderSkeleton
import com.example.animeapp.ui.common_ui.DetailCommonBody
import com.example.animeapp.ui.common_ui.DetailCommonBodySkeleton
import com.example.animeapp.ui.common_ui.YoutubePreview
import com.example.animeapp.ui.common_ui.YoutubePreviewSkeleton

@Composable
fun InfoContentSection(
    animeDetail: AnimeDetail?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (animeDetail != null) {
            AnimeHeader(animeDetail = animeDetail)
            YoutubePreview(embedUrl = animeDetail.trailer.embed_url)
            DetailCommonBody(title = "Synopsis", body = animeDetail.synopsis)
        } else {
            AnimeHeaderSkeleton()
            YoutubePreviewSkeleton()
            DetailCommonBodySkeleton()
        }
    }
}