package com.example.animeapp.ui.animeDetail.relation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.animeapp.models.Relation
import com.example.animeapp.ui.animeDetail.DetailAction
import com.example.animeapp.ui.animeDetail.DetailState
import com.example.animeapp.utils.basicContainer

@Composable
fun RelationSection(
    navController: NavController,
    relations: List<Relation>?,
    detailState: DetailState,
    onAction: (DetailAction) -> Unit,
    onItemClickListener: (Int) -> Unit
) {
    if (relations.isNullOrEmpty()) return

    Column(
        modifier = Modifier
            .basicContainer(outerPadding = PaddingValues(0.dp))
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${relations.size} Relation${if (relations.size > 1) "s" else ""}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(relations) { relation ->
                RelationEntryColumn(
                    relation = relation,
                    detailState = detailState,
                    navController = navController,
                    onAction = onAction,
                    onItemClickListener = onItemClickListener
                )
            }
        }
    }
}