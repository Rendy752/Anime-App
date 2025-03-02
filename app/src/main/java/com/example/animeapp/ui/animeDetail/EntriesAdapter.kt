package com.example.animeapp.ui.animeDetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.databinding.AnimeSearchItemBinding
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.Relation
import com.example.animeapp.utils.AnimeHeaderUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class EntriesAdapter(
    private val animeAPI: AnimeAPI,
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

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onBindViewHolder(holder: RelationItemViewHolder, position: Int) {
        val relationItem = relationItems.entry.getOrNull(position)
        relationItem?.let {
            holder.binding.apply {
                shimmerViewContainer.startShimmer()
            }

            coroutineScope.launch {
                val animeDetail = getAnimeDetail(it.mal_id)
                if (animeDetail != null) {
                    AnimeHeaderUtils.bindAnimeData(holder.binding, animeDetail)
                    holder.itemView.setOnClickListener {
                        onItemClickListener(animeDetail.mal_id)
                    }
                } else {
                    AnimeHeaderUtils.handleNullData(holder.binding, it.name)
                }
                holder.binding.apply {
                    shimmerViewContainer.stopShimmer()
                    shimmerViewContainer.hideShimmer()
                }
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        coroutineScope.coroutineContext.cancelChildren()
    }

    private suspend fun getAnimeDetail(animeId: Int): AnimeDetail? {
        val response = animeAPI.getAnimeDetail(animeId)
        return if (response.isSuccessful) {
            response.body()?.data
        } else {
            null
        }
    }

    override fun getItemCount(): Int {
        return relationItems.entry.size
    }
}