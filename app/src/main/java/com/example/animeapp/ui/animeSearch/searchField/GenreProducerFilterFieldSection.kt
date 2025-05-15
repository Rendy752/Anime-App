package com.example.animeapp.ui.animeSearch.searchField

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.Genre
import com.example.animeapp.models.Producer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GenreProducerFilterFieldSection(
    selectedGenres: List<Genre>,
    setSelectedGenre: (Genre) -> Unit,
    applyGenreFilters: () -> Unit,
    selectedProducers: List<Producer>,
    setSelectedProducer: (Producer) -> Unit,
    applyProducerFilters: () -> Unit,
    isGenresBottomSheetShow: Boolean,
    isProducersBottomSheetShow: Boolean,
    setGenresBottomSheet: (Boolean) -> Unit,
    setProducersBottomSheet: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isGenresBottomSheetShow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                .border(2.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                .clickable {
                    setGenresBottomSheet(true)
                    setProducersBottomSheet(false)
                }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedGenres.isNotEmpty()) {
                    FilterChipFlow(
                        itemList = selectedGenres,
                        onSetSelectedId = {
                            setSelectedGenre(it as Genre)
                            applyGenreFilters()
                        },
                        itemName = { (it as Genre).name },
                        getItemId = { it },
                        isHorizontal = true,
                        isChecked = true
                    )
                } else {
                    Text(
                        text = "Genres",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Icon(
                imageVector = if (isGenresBottomSheetShow) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = "Chevron Down",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isProducersBottomSheetShow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                .border(2.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                .clickable {
                    setProducersBottomSheet(true)
                    setGenresBottomSheet(false)
                }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedProducers.isNotEmpty()) {
                    FilterChipFlow(
                        itemList = selectedProducers,
                        onSetSelectedId = {
                            setSelectedProducer(it as Producer)
                            applyProducerFilters()
                        },
                        itemName = { (it as Producer).titles?.get(0)?.title ?: "Unknown" },
                        getItemId = { it },
                        isHorizontal = true,
                        isChecked = true
                    )
                } else {
                    Text(
                        text = "Producers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Icon(
                imageVector = if (isProducersBottomSheetShow) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = "Chevron Down",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}