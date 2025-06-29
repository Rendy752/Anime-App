package com.luminoverse.animevibe.ui.animeDetail.relation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.Relation
import com.luminoverse.animevibe.ui.animeDetail.DetailAction
import com.luminoverse.animevibe.ui.common.SharedImageState
import com.luminoverse.animevibe.utils.resource.Resource

@Composable
fun RelationEntryColumn(
    relation: Relation,
    relationAnimeDetails: Map<Int, Resource<AnimeDetail>>,
    navController: NavController,
    onAction: (DetailAction) -> Unit,
    showImagePreview: (SharedImageState) -> Unit,
    onItemClickListener: (Int) -> Unit
) {
    Column {
        Text(
            text = "\u202A${relation.entry.size} ${relation.relation}\u202C",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .heightIn(max = 350.dp)
                .widthIn(max = 350.dp)
                .verticalScroll(rememberScrollState())
        ) {
            relation.entry.forEach { entry ->
                RelationEntryItem(
                    entryId = entry.mal_id,
                    entryName = entry.name,
                    relationDetail = relationAnimeDetails[entry.mal_id],
                    navController = navController,
                    onAction = onAction,
                    showImagePreview = showImagePreview,
                    onItemClickListener = onItemClickListener
                )
            }
        }
    }
}