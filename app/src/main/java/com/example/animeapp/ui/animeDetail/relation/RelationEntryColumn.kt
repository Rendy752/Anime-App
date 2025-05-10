package com.example.animeapp.ui.animeDetail.relation

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
import com.example.animeapp.models.Relation
import com.example.animeapp.ui.animeDetail.DetailAction
import com.example.animeapp.ui.animeDetail.DetailState

@Composable
fun RelationEntryColumn(
    relation: Relation,
    detailState: DetailState,
    navController: NavController,
    onAction: (DetailAction) -> Unit,
    onItemClickListener: (Int) -> Unit
) {
    Column {
        Text(
            text = "${relation.entry.size} ${relation.relation}",
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
                    detailState = detailState,
                    navController = navController,
                    onAction = onAction,
                    onItemClickListener = onItemClickListener
                )
            }
        }
    }
}