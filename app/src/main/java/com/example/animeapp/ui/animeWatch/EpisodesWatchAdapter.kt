package com.example.animeapp.ui.animeWatch

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.databinding.EpisodeWatchItemBinding
import com.example.animeapp.models.Episode
import com.example.animeapp.utils.Debounce
import com.example.animeapp.utils.StreamingUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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

    private var selectedEpisodeNo: Int? = null

    @SuppressLint("NotifyDataSetChanged")
    fun updateSelectedEpisode(selectedEpisodeNo: Int) {
        this.selectedEpisodeNo = selectedEpisodeNo
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val episode = episodes[position]
        bindEpisodeData(holder, episode)
    }

    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val episodeDebounce = Debounce(adapterScope, onDebounced = { episodeId ->
        onEpisodeClick(episodeId)
    })

    private fun bindEpisodeData(
        holder: EpisodeViewHolder,
        episode: Episode
    ) {
        holder.binding.apply {
            episode.episodeNo.toString().also { tvEpisodeNumber.text = it }
            selectedEpisodeNo?.let { StreamingUtils.getEpisodeBackground(context, episode, it) }
                ?.let { tvEpisodeNumber.background = it }

            holder.itemView.setOnClickListener {
                updateSelectedEpisode(episode.episodeNo)
                episodeDebounce.query(episode.episodeId)
            }
        }
    }

    override fun getItemCount(): Int = episodes.size
}