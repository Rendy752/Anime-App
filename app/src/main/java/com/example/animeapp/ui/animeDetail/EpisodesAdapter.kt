package com.example.animeapp.ui.animeDetail

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.R
import com.example.animeapp.databinding.EpisodeItemBinding
import com.example.animeapp.models.Episode

class EpisodesAdapter(
    private val context: Context,
    private val episodes: List<Episode>,
    private val onEpisodeClick: (String) -> Unit
) : RecyclerView.Adapter<EpisodesAdapter.EpisodeViewHolder>() {

    class EpisodeViewHolder(val binding: EpisodeItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val binding =
            EpisodeItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EpisodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val episode = episodes[position]
        bindEpisodeData(holder.binding, episode)

        holder.itemView.setOnClickListener {
            onEpisodeClick(episode.episodeId)
        }
    }

    private fun bindEpisodeData(binding: EpisodeItemBinding, episode: Episode) {
        val backgroundColor = if (episode.filler) {
            ContextCompat.getColor(context, R.color.filler_episode)
        } else {
            ContextCompat.getColor(context, R.color.default_episode)
        }

        val backgroundDrawable = GradientDrawable().apply {
            setColor(backgroundColor)
            cornerRadius = 16f
        }

        val layerDrawable = LayerDrawable(arrayOf(backgroundDrawable))

        binding.apply {
            tvEpisodeNumber.background = layerDrawable
            "Ep. ${episode.episodeNo}".also { tvEpisodeNumber.text = it }
            tvEpisodeTitle.text = episode.name
        }
    }

    override fun getItemCount(): Int {
        return episodes.size
    }
}