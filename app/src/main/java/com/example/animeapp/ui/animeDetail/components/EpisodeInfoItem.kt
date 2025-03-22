package com.example.animeapp.ui.animeDetail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.animeapp.R
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.ui.common_ui.SkeletonBox
import com.example.animeapp.utils.Navigation.navigateToAnimeWatch
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ShareUtils

@Composable
fun EpisodeInfoItem(
    text: String,
    color: Color,
    icon: ImageVector,
    isFirst: Boolean,
    isLast: Boolean,
    hasRight: Boolean
) {
    val leftShape = if (isFirst) RoundedCornerShape(percent = 50) else RoundedCornerShape(0.dp)
    val rightShape =
        if (isLast || !hasRight) RoundedCornerShape(percent = 50) else RoundedCornerShape(0.dp)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(
                topStart = leftShape.topStart,
                topEnd = rightShape.topEnd,
                bottomStart = leftShape.bottomStart,
                bottomEnd = rightShape.bottomEnd
            ),
            color = color,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun EpisodeInfoItemSkeleton(
    isFirst: Boolean,
    isLast: Boolean,
    hasRight: Boolean
) {
    val width = 60.dp
    val height = 32.dp

    val leftShape = if (isFirst) RoundedCornerShape(percent = 50) else RoundedCornerShape(0.dp)
    val rightShape =
        if (isLast || !hasRight) RoundedCornerShape(percent = 50) else RoundedCornerShape(0.dp)

    Row(verticalAlignment = Alignment.CenterVertically) {
        SkeletonBox(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = leftShape.topStart,
                        topEnd = rightShape.topEnd,
                        bottomStart = leftShape.bottomStart,
                        bottomEnd = rightShape.bottomEnd
                    )
                )
                .background(MaterialTheme.colorScheme.primary),
            width = width,
            height = height,
        )
        if (hasRight) {
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailTopBar(
    animeTitle: String,
    animeDetail: Resource<AnimeDetailResponse>?,
    animeDetailComplement: Resource<AnimeDetailComplement?>?,
    defaultEpisode: EpisodeDetailComplement?,
    navController: NavController
) {
    val context = LocalContext.current
    Column {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            title = {
                Text(
                    text = animeDetail?.data?.data?.title ?: animeTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Companion.Ellipsis
                )
            },
            actions = {
                animeDetail?.data?.data?.let { animeDetailData ->
                    if (animeDetailComplement is Resource.Success &&
                        animeDetailComplement.data?.episodes?.isNotEmpty() == true &&
                        defaultEpisode != null
                    ) {
                        animeDetailComplement.data.let { animeDetailComplementData ->
                            IconButton(onClick = {
                                navController.navigateToAnimeWatch(
                                    animeDetail = animeDetailData,
                                    episodeId = animeDetailComplementData.episodes[0].episodeId,
                                    episodes = animeDetailComplementData.episodes,
                                    defaultEpisode = defaultEpisode
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.LiveTv,
                                    tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = stringResource(id = R.string.watch)
                                )
                            }
                        }
                    }
                    IconButton(onClick = {
                        ShareUtils.shareAnimeDetail(context, animeDetailData)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = stringResource(id = R.string.filter)
                        )
                    }
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