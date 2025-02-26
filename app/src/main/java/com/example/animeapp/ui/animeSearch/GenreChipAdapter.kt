package com.example.animeapp.ui.animeSearch

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.R
import com.example.animeapp.models.Genre
import com.google.android.material.chip.Chip

class GenreChipAdapter(
    private val onChipClick: (Genre) -> Unit,
) : RecyclerView.Adapter<GenreChipAdapter.ChipViewHolder>() {

    var items: List<Genre> = emptyList()
    var selectedIds: List<Int> = emptyList()

    class ChipViewHolder(val chip: Chip) : RecyclerView.ViewHolder(chip)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val chip = LayoutInflater.from(parent.context)
            .inflate(R.layout.chip_layout, parent, false) as Chip
        return ChipViewHolder(chip)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val genre = items[position]
        "${genre.name} (${genre.count})".also { holder.chip.text = it }
        holder.chip.isChecked = selectedIds.contains(genre.mal_id)
        holder.chip.setOnLongClickListener {
            val context = holder.chip.context
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(genre.url)))
            true
        }
        holder.chip.setOnClickListener { onChipClick(genre) }
    }

    override fun getItemCount(): Int = items.size
}