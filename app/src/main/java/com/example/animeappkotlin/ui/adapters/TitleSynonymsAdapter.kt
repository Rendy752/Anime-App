package com.example.animeappkotlin.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.animeappkotlin.databinding.TitleSynonymItemBinding

class TitleSynonymsAdapter(private val synonyms: List<String>) :
    RecyclerView.Adapter<TitleSynonymsAdapter.SynonymViewHolder>() {

    class SynonymViewHolder(val binding: TitleSynonymItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SynonymViewHolder {
        val binding = TitleSynonymItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SynonymViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SynonymViewHolder, position: Int) {
        holder.binding.tvTitleSynonym.text = synonyms[position]
    }

    override fun getItemCount(): Int {
        return synonyms.size
    }
}