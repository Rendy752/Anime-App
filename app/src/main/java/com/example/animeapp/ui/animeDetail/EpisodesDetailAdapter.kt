package com.example.animeapp.ui.animeDetail

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.databinding.EpisodeDetailItemBinding
import com.example.animeapp.models.Episode
import com.example.animeapp.utils.StreamingUtils

class EpisodesDetailAdapter(
    private val context: Context,
    private val episodes: List<Episode>,
    private val onEpisodeClick: (String) -> Unit
) : RecyclerView.Adapter<EpisodesDetailAdapter.EpisodeViewHolder>() {

    class EpisodeViewHolder(val binding: EpisodeDetailItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val binding =
            EpisodeDetailItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EpisodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val episode = episodes[position]
        bindEpisodeData(holder.binding, episode)

        holder.itemView.setOnClickListener {
            onEpisodeClick(episode.episodeId)
        }
    }

    private fun bindEpisodeData(binding: EpisodeDetailItemBinding, episode: Episode) {
        binding.apply {
            tvEpisodeNumber.background = StreamingUtils.getEpisodeBackground(context, episode)
            "Ep. ${episode.episodeNo}".also { tvEpisodeNumber.text = it }
            tvEpisodeTitle.text = episode.name
        }
    }

    override fun getItemCount(): Int {
        return episodes.size
    }
}