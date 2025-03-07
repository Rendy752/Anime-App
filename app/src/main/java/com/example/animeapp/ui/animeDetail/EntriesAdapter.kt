package com.example.animeapp.ui.animeDetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.databinding.AnimeSearchItemBinding
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.Relation
import com.example.animeapp.utils.BindAnimeUtils
import com.example.animeapp.utils.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EntriesAdapter(
    private val relationItems: Relation,
    private val getAnimeDetail: suspend (Int) -> Resource<AnimeDetailResponse>,
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
                withContext(Dispatchers.Main) {
                    holder.binding.apply {
                        when (animeDetail) {
                            is Resource.Success -> {
                                shimmerViewContainer.stopShimmer()
                                shimmerViewContainer.hideShimmer()
                                animeDetail.data?.data?.let { animeDetailData ->
                                    BindAnimeUtils.bindAnimeData(holder.binding, animeDetailData)
                                    holder.itemView.setOnClickListener {
                                        onItemClickListener(animeDetailData.mal_id)
                                    }
                                } ?: run {
                                    BindAnimeUtils.handleNullData(holder.binding, it.name)
                                }
                            }

                            is Resource.Error -> {
                                shimmerViewContainer.stopShimmer()
                                shimmerViewContainer.hideShimmer()
                                BindAnimeUtils.handleNullData(holder.binding, it.name)
                            }

                            is Resource.Loading -> {
                                shimmerViewContainer.startShimmer()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        coroutineScope.coroutineContext.cancelChildren()
    }

    override fun getItemCount(): Int {
        return relationItems.entry.size
    }
}