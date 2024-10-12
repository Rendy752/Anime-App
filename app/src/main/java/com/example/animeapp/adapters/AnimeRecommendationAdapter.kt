package com.example.animeapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.animeapp.databinding.ItemAnimeDetailBinding
import com.example.animeapp.models.AnimeRecommendation

class AnimeRecommendationAdapter : RecyclerView.Adapter<AnimeRecommendationAdapter.AnimeRecommendationViewHolder>() {
    inner class AnimeRecommendationViewHolder(private val binding: ItemAnimeDetailBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(animeRecommendation: AnimeRecommendation) {
            binding.apply {
                Glide.with(itemView.context).load(animeRecommendation.entry[0].images.jpg.image_url).into(ivArticleImage)
                tvDate.text = animeRecommendation.date
                tvContent.text = animeRecommendation.content
                itemView.setOnClickListener {
                    onItemClickListener?.let { it(animeRecommendation) }
                }
            }
        }
    }
    private var _binding: ItemAnimeDetailBinding? = null
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
        val binding = ItemAnimeDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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