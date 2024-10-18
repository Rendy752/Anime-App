package com.example.animeapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
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
            val adapter = LogEntryAdapter(logEntries)
            binding.recyclerViewLogs.adapter = adapter
            binding.recyclerViewLogs.layoutManager =
                LinearLayoutManager(context)
        } else {
            binding.recyclerViewLogs.visibility = View.GONE
            binding.tvNoLogs.visibility = View.VISIBLE
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}