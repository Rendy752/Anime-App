package com.example.animeapp.ui.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.R
import com.example.animeapp.models.LogEntry
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

class LogEntryAdapter(private val logEntries: List<LogEntry>) :
    RecyclerView.Adapter<LogEntryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUrl: TextView = itemView.findViewById(R.id.tvUrl)
        val tvCode: TextView = itemView.findViewById(R.id.tvCode)
        val tvBody: TextView = itemView.findViewById(R.id.tvBody)
        val btnReadMore: Button = itemView.findViewById(R.id.btnReadMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.log_entry_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val logEntry = logEntries[position]
        holder.tvUrl.text = "Request URL:\n${logEntry.requestUrl}"
        holder.tvCode.text = "Response Code: ${logEntry.responseCode}"

        try {
            val gson = GsonBuilder().setPrettyPrinting().create()
            val jsonElement = JsonParser.parseString(logEntry.responseBody)
            val formattedJson = gson.toJson(jsonElement)
            holder.tvBody.text = "Response Body:\n$formattedJson"
        } catch (e: Exception) {
            holder.tvBody.text = "Response Body:\n${logEntry.responseBody}"
        }

        if (logEntry.responseBody.length > 100) {
            holder.btnReadMore.visibility = View.VISIBLE
            holder.btnReadMore.setOnClickListener {
                toggleReadMore(holder)
            }

            holder.tvBody.setOnClickListener {
                toggleReadMore(holder)
            }
        } else {
            holder.btnReadMore.visibility = View.GONE
        }
    }

    private fun toggleReadMore(holder: ViewHolder) {
        if (holder.tvBody.maxLines == 5) {
            holder.tvBody.maxLines = Integer.MAX_VALUE
            holder.btnReadMore.text = "Read Less"
        } else {
            holder.tvBody.maxLines = 5
            holder.btnReadMore.text = "Read More"
        }
    }

    override fun getItemCount(): Int {
        return logEntries.size
    }
}