package com.example.animeapp.ui.animeWatch

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.content.res.AppCompatResources
import com.example.animeapp.R
import com.example.animeapp.models.EpisodeSourcesQuery

class ServerAdapter(
    private val serverItems: List<EpisodeSourcesQuery>,
    private val selectedServerItems: EpisodeSourcesQuery,
    private val onClick: (episodeSourcesQuery: EpisodeSourcesQuery) -> Unit
) :
    RecyclerView.Adapter<ServerAdapter.ServerViewHolder>() {

    inner class ServerViewHolder(itemView: TextView) : RecyclerView.ViewHolder(itemView) {
        val tvServerName: TextView = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.server_item, parent, false) as TextView
        return ServerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        val currentItem = serverItems[position]
        holder.tvServerName.text = currentItem.server
        if (currentItem.server == selectedServerItems.server && currentItem.category == selectedServerItems.category) {
            AppCompatResources.getDrawable(
                holder.tvServerName.context,
                R.drawable.rounded_corner_blue
            )?.let {
                holder.tvServerName.background = it
                holder.tvServerName.setTextColor(Color.WHITE)
            }
            holder.tvServerName.isClickable = false
        } else {
            holder.tvServerName.setOnClickListener { onClick(currentItem) }
            holder.tvServerName.isClickable = true
        }
    }

    override fun getItemCount() = serverItems.size
}