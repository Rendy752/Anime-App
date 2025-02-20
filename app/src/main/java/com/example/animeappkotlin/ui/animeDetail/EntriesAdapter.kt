package com.example.animeappkotlin.ui.animeDetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.animeappkotlin.data.remote.api.RetrofitInstance
import com.example.animeappkotlin.databinding.AnimeSearchItemBinding
import com.example.animeappkotlin.models.AnimeDetail
import com.example.animeappkotlin.models.Relation
import com.example.animeappkotlin.utils.AnimeHeaderUtils
import kotlinx.coroutines.runBlocking

class EntriesAdapter(
    private val relationItems: Relation,
    private val onItemClickListener: (Int) -> Unit
) :
    RecyclerView.Adapter<EntriesAdapter.RelationItemViewHolder>() {

    class RelationItemViewHolder(val binding: AnimeSearchItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelationItemViewHolder {
        val binding =
            AnimeSearchItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RelationItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RelationItemViewHolder, position: Int) {
        val relationItem = relationItems.entry.getOrNull(position)
        relationItem?.let {
            val animeDetail = getAnimeDetail(relationItem.mal_id)
            if (animeDetail != null) {
                AnimeHeaderUtils.bindAnimeData(holder.binding, animeDetail)
                holder.itemView.setOnClickListener {
                    onItemClickListener(animeDetail.mal_id)
                }
            } else {
                AnimeHeaderUtils.handleNullData(holder.binding, relationItem.name)
            }
            holder.binding.apply {
                shimmerViewContainer.stopShimmer()
                shimmerViewContainer.hideShimmer()
            }
        }
    }

    override fun getItemCount(): Int {
        return relationItems.entry.size
    }

    private fun getAnimeDetail(animeId: Int): AnimeDetail? {
        var detail: AnimeDetail? = null
        val response = runBlocking { RetrofitInstance.api.getAnimeDetail(animeId) }
        if (response.isSuccessful) {
            detail = response.body()?.data
        }
        return detail
    }
}