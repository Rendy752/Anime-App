package com.example.animeappkotlin.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.animeappkotlin.databinding.UnorderedListItemBinding

class UnorderedListAdapter(private val item: List<String>, private val onItemClick: ((String) -> Unit)? = null ) :
    RecyclerView.Adapter<UnorderedListAdapter.UnorderedListViewHolder>() {

    class UnorderedListViewHolder(val binding: UnorderedListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnorderedListViewHolder {
        val binding =
            UnorderedListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UnorderedListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UnorderedListViewHolder, position: Int) {
        holder.binding.tvListItem.text = item[position]
        if (onItemClick != null) holder.binding.tvListItem.setTextColor(holder.itemView.context.getColor(android.R.color.holo_blue_light))
        holder.binding.root.setOnClickListener {
            onItemClick?.invoke(item[position])
        }
    }

    override fun getItemCount(): Int {
        return item.size
    }
}