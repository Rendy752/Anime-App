package com.example.animeapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.animeapp.databinding.FragmentLoggingBinding

class LoggingFragment : Fragment() {
    private var _binding: FragmentLoggingBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_LOGS = "logs"

        fun newInstance(logs: String): LoggingFragment {
            val fragment = LoggingFragment()
            val args = Bundle()
            args.putString(ARG_LOGS, logs)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoggingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val tvLogging: TextView = binding.tvLogging
        tvLogging.text = arguments?.getString(ARG_LOGS)
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}