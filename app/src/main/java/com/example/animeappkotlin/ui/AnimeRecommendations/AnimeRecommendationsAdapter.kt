package com.example.animeappkotlin.ui.AnimeRecommendations

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.animeappkotlin.databinding.AnimeRecommendationItemBinding
import com.example.animeappkotlin.models.AnimeRecommendation
import com.example.animeappkotlin.utils.DateUtils

class AnimeRecommendationsAdapter : RecyclerView.Adapter<AnimeRecommendationsAdapter.AnimeRecommendationViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1
    }

    inner class AnimeRecommendationViewHolder(private val binding: AnimeRecommendationItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(animeRecommendation: AnimeRecommendation?, isLoading: Boolean) {
            binding.apply {
                if (isLoading) {
                    shimmerViewContainer.startShimmer()
                } else {
                    shimmerViewContainer.stopShimmer()
                    shimmerViewContainer.hideShimmer()

                    animeRecommendation?.let { recommendation ->
                        bindAnimeData(recommendation)
                        setupClickListeners(recommendation)
                        resetBackgrounds()
                    }
                }
            }
        }

        private fun AnimeRecommendationItemBinding.bindAnimeData(recommendation: AnimeRecommendation) {
            Glide.with(itemView.context)
                .load(recommendation.entry[0].images.jpg.image_url)
                .into(ivFirstAnimeImage)
            tvFirstAnimeTitle.text = recommendation.entry[0].title
            Glide.with(itemView.context)
                .load(recommendation.entry[1].images.jpg.image_url)
                .into(ivSecondAnimeImage)
            tvSecondAnimeTitle.text = recommendation.entry[1].title
            tvContent.text = recommendation.content
            tvRecommendedBy.text = "recommended by ${recommendation.user.username}"
            tvDate.text = "~ ${DateUtils.formatDateToAgo(recommendation.date)}"
        }

        private fun AnimeRecommendationItemBinding.setupClickListeners(recommendation: AnimeRecommendation) {
            ivFirstAnimeImage.setOnClickListener {
                onItemClickListener?.invoke(recommendation.entry[0].mal_id)
            }
            tvFirstAnimeTitle.setOnClickListener {
                onItemClickListener?.invoke(recommendation.entry[0].mal_id)
            }

            ivSecondAnimeImage.setOnClickListener {
                onItemClickListener?.invoke(recommendation.entry[1].mal_id)
            }
            tvSecondAnimeTitle.setOnClickListener {
                onItemClickListener?.invoke(recommendation.entry[1].mal_id)
            }
        }

        private fun AnimeRecommendationItemBinding.resetBackgrounds() {
            ivFirstAnimeImage.background = null
            tvFirstAnimeTitle.background = null
            ivSecondAnimeImage.background = null
            tvSecondAnimeTitle.background = null
            tvContent.background = null
            tvRecommendedBy.background = null
            tvDate.background = null
        }
    }


    private var isLoading = false

    fun setLoading(isLoading: Boolean) {
        this.isLoading = isLoading
        notifyDataSetChanged()
    }

    private val differCallback = object : DiffUtil.ItemCallback<AnimeRecommendation>() {
        override fun areItemsTheSame(oldItem: AnimeRecommendation, newItem: AnimeRecommendation): Boolean {
            return oldItem.mal_id == newItem.mal_id
        }

        override fun areContentsTheSame(oldItem: AnimeRecommendation, newItem: AnimeRecommendation): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimeRecommendationViewHolder {
        val binding = AnimeRecommendationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnimeRecommendationViewHolder(binding)
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

    override fun onBindViewHolder(holder: AnimeRecommendationViewHolder, position: Int) {
        val animeRecommendation = if (!isLoading || position >= differ.currentList.size) {
            differ.currentList.getOrNull(position)
        } else {
            null
        }
        holder.bind(animeRecommendation, isLoading && position < 5)
    }

    private var onItemClickListener: ((Int) -> Unit)? = null

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        onItemClickListener = listener
    }
}