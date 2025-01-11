package com.example.animeappkotlin.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.animeappkotlin.databinding.AnimeSearchItemBinding
import com.example.animeappkotlin.models.AnimeDetail

class AnimeSearchAdapter : RecyclerView.Adapter<AnimeSearchAdapter.AnimeSearchViewHolder>() {

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
                        bindAnimeData(data)
                        setupClickListeners(data)
                        resetBackgrounds()
                    }
                }
            }
        }
    }

    private fun AnimeSearchItemBinding.bindAnimeData(data: AnimeDetail) {
        Glide.with(root.context)
            .load(data.images.jpg.image_url)
            .into(ivAnimeImage)

        when (data.approved) {
            true -> {
                ivApproved.visibility = View.VISIBLE
            }

            false -> {
                ivApproved.visibility = View.GONE
            }
        }
        when (data.airing) {
            true -> {
                ivAired.visibility = View.VISIBLE
                ivNotAired.visibility = View.GONE
            }

            false -> {
                ivAired.visibility = View.GONE
                ivNotAired.visibility = View.VISIBLE
            }
        }

        tvAnimeTitle.text = data.title
        rvTitleSynonyms.apply {
            adapter = data.title_synonyms?.let { TitleSynonymsAdapter(it.toList()) }
            layoutManager = LinearLayoutManager(root.context, LinearLayoutManager.HORIZONTAL, false)
        }
        tvAnimeType.text = data.type
        tvAnimeScore.text = data.score.toString()
    }

    private fun AnimeSearchItemBinding.setupClickListeners(data: AnimeDetail) {
        root.setOnClickListener {
            onItemClickListener?.invoke(data.mal_id)
        }
    }

    private fun AnimeSearchItemBinding.resetBackgrounds() {
        ivAnimeImage.background = null
        contentLayout.background = null
        rvTitleSynonyms.background = null
        tvAnimeType.background = null
        tvAnimeScore.background = null
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