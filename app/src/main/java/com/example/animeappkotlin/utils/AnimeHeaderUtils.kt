package com.example.animeappkotlin.utils

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.animeappkotlin.R
import com.example.animeappkotlin.databinding.AnimeSearchItemBinding
import com.example.animeappkotlin.models.AnimeDetail
import com.example.animeappkotlin.ui.Common.TitleSynonymsAdapter

object AnimeHeaderUtils {
    fun bindAnimeData(binding: AnimeSearchItemBinding, data: AnimeDetail) {
        Glide.with(binding.root.context)
            .load(data.images.jpg.image_url)
            .into(binding.ivAnimeImage)

        when (data.approved) {
            true -> binding.ivApproved.visibility = View.VISIBLE
            false -> binding.ivApproved.visibility = View.GONE
        }

        when (data.airing) {
            true -> {
                binding.tvAiredStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_notifications_active_24dp, 0);
            }

            false -> {
                binding.tvAiredStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_done_24dp, 0);
            }
        }

        binding.tvAnimeTitle.text = data.title
        binding.rvTitleSynonyms.apply {
            adapter = data.title_synonyms?.let { TitleSynonymsAdapter(it.toList()) }
            layoutManager =
                LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
        }
        binding.tvAnimeType.text = "${data.type} (${data.episodes} eps)"
        binding.tvAnimeRanked.text = "Ranked #${data.rank ?: 0}"
        binding.tvAnimePopularity.text = "Popularity #${data.popularity}"
        binding.tvAnimeScore.text = "Scored ${data.score ?: 0} by ${data.scored_by ?: 0} users"
        binding.tvAnimeMembers.text = "${data.members} members"

        resetBackground(binding)
    }


    fun handleNullData(binding: AnimeSearchItemBinding, title: String? = null) {
        Glide.with(binding.root.context)
            .load(R.drawable.ic_error_yellow_24dp)
            .into(binding.ivAnimeImage)

        binding.tvAnimeTitle.text = title ?: "Unknown Title"
        binding.rvTitleSynonyms.apply {
            adapter = TitleSynonymsAdapter(emptyList())
            layoutManager =
                LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
        }
        binding.tvAnimeType.text = "Unknown Type (Unknown Episodes)"
        binding.tvAnimeRanked.text = "Ranked #Unknown"
        binding.tvAnimePopularity.text = "Popularity #Unknown"
        binding.tvAnimeScore.text = "Scored Unknown by Unknown users"
        binding.tvAnimeMembers.text = "Unknown members"

        resetBackground(binding)
    }

    private fun resetBackground(binding: AnimeSearchItemBinding) {
        binding.ivAnimeImage.background = null
        binding.contentLayout.background = null
        binding.rvTitleSynonyms.background = null
        binding.tvAnimeType.background = null
        binding.tvAnimeRanked.background = null
        binding.tvAnimePopularity.background = null
        binding.tvAnimeScore.background = null
        binding.tvAnimeMembers.background = null
    }
}