package com.example.animeapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.animeapp.databinding.FragmentLoggingBinding
import com.example.animeapp.models.LogEntry
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

class LoggingFragment : Fragment() {
    private var _binding: FragmentLoggingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoggingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val logEntries = arguments?.getParcelableArrayList<LogEntry>("logEntries")

        if (logEntries != null && logEntries.isNotEmpty()) {
            binding.btnReadMore.setOnClickListener {
                if (binding.tvBody.maxLines == 5) {
                    binding.tvBody.maxLines = Integer.MAX_VALUE
                    binding.btnReadMore.text = "Read Less"
                } else {
                    binding.tvBody.maxLines = 5
                    binding.btnReadMore.text = "Read More"
                }
            }

            val logEntry = logEntries[0]

            if (logEntry.responseBody.length > 100) {
                binding.btnReadMore.visibility = View.VISIBLE
            }

            binding.tvUrl.text = "Request URL:\n${logEntry.requestUrl}"
            binding.tvCode.text = "Response Code: ${logEntry.responseCode}"
            try {
                val gson = GsonBuilder().setPrettyPrinting().create()
                val jsonElement = JsonParser.parseString(logEntry.responseBody)
                val formattedJson = gson.toJson(jsonElement)
                binding.tvBody.text = "Response Body:\n$formattedJson"
            } catch (e: Exception) {
                binding.tvBody.text = "Response Body:\n${logEntry.responseBody}"
            }
        } else {
            binding.tvUrl.text = "No logs to display."
            binding.tvCode.text = ""
            binding.tvBody.text = ""
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}