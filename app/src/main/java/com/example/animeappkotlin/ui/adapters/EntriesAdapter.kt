package com.example.animeappkotlin.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.animeappkotlin.data.remote.api.RetrofitInstance
import com.example.animeappkotlin.databinding.EntryItemBinding
import com.example.animeappkotlin.models.Relation
import kotlinx.coroutines.runBlocking

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

            Glide.with(holder.itemView.context)
                .load(getAnimeImageUrl(relationItem.mal_id))
                .into(holder.binding.ivAnimeImage)
        }
    }

    override fun getItemCount(): Int {
        return relationItems.entry.size ?: 0
    }

    private fun getAnimeImageUrl(animeId: Int): String? {
        var imageUrl: String? = null
        val response = runBlocking { RetrofitInstance.api.getAnimeDetail(animeId) }
        if (response.isSuccessful) {
            imageUrl = response.body()?.data?.images?.jpg?.image_url
        }
        return imageUrl
    }
}