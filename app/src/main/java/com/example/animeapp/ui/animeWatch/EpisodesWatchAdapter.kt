package com.example.animeapp.ui.animeWatch

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.databinding.EpisodeWatchItemBinding
import com.example.animeapp.models.Episode
import com.example.animeapp.utils.StreamingUtils

class EpisodesWatchAdapter(
    private val context: Context,
    private val episodes: List<Episode>,
    private val onEpisodeClick: (String) -> Unit
) : RecyclerView.Adapter<EpisodesWatchAdapter.EpisodeViewHolder>() {

    class EpisodeViewHolder(val binding: EpisodeWatchItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val binding =
            EpisodeWatchItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EpisodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val episode = episodes[position]
        bindEpisodeData(holder.binding, episode)

        holder.itemView.setOnClickListener {
            onEpisodeClick(episode.episodeId)
        }
    }

    private fun bindEpisodeData(binding: EpisodeWatchItemBinding, episode: Episode) {
        binding.apply {
            episode.episodeNo.toString().also { tvEpisodeNumber.text = it }
            tvEpisodeNumber.background = StreamingUtils.getEpisodeBackground(context, episode)
        }
    }

    override fun getItemCount(): Int = episodes.size
}