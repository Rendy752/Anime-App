package com.example.animeapp.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.animeapp.databinding.AnimeRecommendationItemBinding
import com.example.animeapp.models.AnimeRecommendation
import com.example.animeapp.utils.DateUtils

class AnimeRecommendationsAdapter : RecyclerView.Adapter<AnimeRecommendationsAdapter.AnimeRecommendationViewHolder>() {
    inner class AnimeRecommendationViewHolder(private val binding: AnimeRecommendationItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(animeRecommendation: AnimeRecommendation) {
            binding.apply {
                Glide.with(itemView.context).load(animeRecommendation.entry[0].images.jpg.image_url).into(ivFirstAnimeImage)
                tvFirstAnimeTitle.text = animeRecommendation.entry[0].title
                Glide.with(itemView.context).load(animeRecommendation.entry[1].images.jpg.image_url).into(ivSecondAnimeImage)
                tvSecondAnimeTitle.text = animeRecommendation.entry[1].title
                tvContent.text = animeRecommendation.content
                tvRecommendedBy.text = "recommended by ${animeRecommendation.user.username}"
                tvDate.text = "~ ${DateUtils.formatDateToAgo(animeRecommendation.date)}"

                tvFirstAnimeTitle.setOnClickListener {
                    onAnimeTitleClickListener?.let { it(animeRecommendation.entry[0].mal_id.toString()) }
                }

                tvSecondAnimeTitle.setOnClickListener {
                    onAnimeTitleClickListener?.let { it(animeRecommendation.entry[1].mal_id.toString()) }
                }
            }
        }
    }

    private var onAnimeTitleClickListener: ((String) -> Unit)? = null
    fun setOnAnimeTitleClickListener(listener: (String) -> Unit) {
        onAnimeTitleClickListener = listener
    }

    private var _binding: AnimeRecommendationItemBinding? = null
    private val binding get() = _binding!!

    private val differCallback = object : DiffUtil.ItemCallback<AnimeRecommendation>() {
        override fun areItemsTheSame(oldItem: AnimeRecommendation, newItem: AnimeRecommendation): Boolean {
            return oldItem.mal_id == newItem.mal_id
        }

        override fun areContentsTheSame(
            oldItem: AnimeRecommendation,
            newItem: AnimeRecommendation
        ): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AnimeRecommendationViewHolder {
        val binding = AnimeRecommendationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnimeRecommendationViewHolder(binding)
    }

    override fun getItemCount(

    ): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: AnimeRecommendationViewHolder, position: Int) {
        val animeRecommendation = differ.currentList[position]
        holder.bind(animeRecommendation)
    }

    private var onItemClickListener: ((AnimeRecommendation) -> Unit)? = null

    fun setOnItemClickListener(listener: (AnimeRecommendation) -> Unit) {
        onItemClickListener = listener
    }
}