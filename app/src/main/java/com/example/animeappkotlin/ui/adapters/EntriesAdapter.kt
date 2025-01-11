package com.example.animeappkotlin.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.animeappkotlin.databinding.EntryItemBinding
import com.example.animeappkotlin.models.Relation

class EntriesAdapter(private val relationItems: Relation) :
    RecyclerView.Adapter<EntriesAdapter.RelationItemViewHolder>() {

    class RelationItemViewHolder(val binding: EntryItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelationItemViewHolder {
        val binding = EntryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RelationItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RelationItemViewHolder, position: Int) {
        val relationItem = relationItems.entry.getOrNull(position)
        relationItem?.let {
            holder.binding.tvEntryType.text = relationItem.type
            holder.binding.tvEntryName.text = relationItem.name
        }
    }

    override fun getItemCount(): Int {
        return relationItems.entry.size ?: 0
    }
}