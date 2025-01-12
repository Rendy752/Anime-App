package com.example.animeappkotlin.ui.Common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.animeappkotlin.databinding.AnimeSearchItemBinding
import com.example.animeappkotlin.models.AnimeDetail
import com.example.animeappkotlin.utils.AnimeHeaderUtils

class AnimeHeaderAdapter : RecyclerView.Adapter<AnimeHeaderAdapter.AnimeSearchViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1
    }

    inner class AnimeSearchViewHolder(private val binding: AnimeSearchItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(anime: AnimeDetail?, isLoading: Boolean) {
            binding.apply {
                if (isLoading) {
                    shimmerViewContainer.startShimmer()
                } else {
                    shimmerViewContainer.stopShimmer()
                    shimmerViewContainer.hideShimmer()

                    anime?.let { data ->
                        AnimeHeaderUtils.bindAnimeData(binding, data)
                        setupClickListeners(data)
                    }
                }
            }
        }
    }

    private fun AnimeSearchItemBinding.setupClickListeners(data: AnimeDetail) {
        root.setOnClickListener {
            onItemClickListener?.invoke(data.mal_id)
        }
    }

    private var isLoading = false

    fun setLoading(isLoading: Boolean) {
        this.isLoading = isLoading
        if (!isLoading) {
            if (differ.currentList.isNotEmpty()) {
                differ.submitList(emptyList())
            }
        }
        notifyDataSetChanged()
    }

    private val differCallback = object : DiffUtil.ItemCallback<AnimeDetail>() {
        override fun areItemsTheSame(oldItem: AnimeDetail, newItem: AnimeDetail): Boolean {
            return oldItem.mal_id == newItem.mal_id
        }

        override fun areContentsTheSame(oldItem: AnimeDetail, newItem: AnimeDetail): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimeSearchViewHolder {
        val binding =
            AnimeSearchItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnimeSearchViewHolder(binding)
    }

    override fun getItemViewType(position: Int): Int {
        return if (isLoading && position < 5) {
            VIEW_TYPE_LOADING
        } else {
            VIEW_TYPE_ITEM
        }
    }

    override fun getItemCount(): Int {
        return if (isLoading) 5 else differ.currentList.size
    }

    override fun onBindViewHolder(holder: AnimeSearchViewHolder, position: Int) {
        if (isLoading && position < 5) {
            holder.bind(null, true)
        } else {
            val anime = differ.currentList.getOrNull(position)
            holder.bind(anime, false)
        }
    }

    private var onItemClickListener: ((Int) -> Unit)? = null

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        onItemClickListener = listener
    }
}