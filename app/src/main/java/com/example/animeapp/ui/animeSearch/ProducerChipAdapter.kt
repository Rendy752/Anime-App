package com.example.animeapp.ui.animeSearch

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.R
import com.example.animeapp.models.Producer
import com.google.android.material.chip.Chip

class ProducerChipAdapter(
    private val onChipClick: (Producer) -> Unit,
) : RecyclerView.Adapter<ProducerChipAdapter.ChipViewHolder>() {

    var items: List<Producer> = emptyList()
    var selectedIds: List<Int> = emptyList()

    class ChipViewHolder(val chip: Chip) : RecyclerView.ViewHolder(chip)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val chip = LayoutInflater.from(parent.context)
            .inflate(R.layout.chip_layout, parent, false) as Chip
        return ChipViewHolder(chip)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val producer = items[position]

        "${producer.titles?.get(0)?.title ?: "Unknown"} (${producer.count})".also { holder.chip.text = it }
        holder.chip.isChecked = selectedIds.contains(producer.mal_id)
        // ... (Load producer icon as before)
//                val iconUrl = producer.images?.jpg?.image_url
//                if (!iconUrl.isNullOrEmpty()) {
//                    lifecycleScope.launch(Dispatchers.Main) {
//                        val drawable: Drawable? = try {
//                            withContext(Dispatchers.IO) {
//                                Glide.with(requireContext())
//                                    .load(iconUrl)
//                                    .circleCrop()
//                                    .submit()
//                                    .get()
//                            }
//                        } catch (e: Exception) {
//                            null
//                        }
//                        if (drawable != null) {
//                            chipIcon = drawable
//                        } else {
//                            setChipIconResource(R.drawable.ic_error_yellow_24dp)
//                        }
//                    }
//                } else {
//                    setChipIconResource(R.drawable.ic_error_yellow_24dp)
//                }
        holder.chip.setOnLongClickListener {
            val context = holder.chip.context
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(producer.url)))
            true
        }
        holder.chip.setOnClickListener { onChipClick(producer) }
    }

    override fun getItemCount(): Int = items.size
}