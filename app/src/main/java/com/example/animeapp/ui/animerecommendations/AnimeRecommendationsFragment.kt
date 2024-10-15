package com.example.animeapp.ui.animerecommendations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.animeapp.MainActivity
import com.example.animeapp.databinding.FragmentRecommendationBinding

class AnimeRecommendationsFragment: Fragment() {
    private var _binding: FragmentRecommendationBinding? = null
    private val binding get() = _binding!!
    lateinit var viewModel: AnimeRecommendationsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = (activity as MainActivity).animeRecommendationsViewModel

        _binding = FragmentRecommendationBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textRecommendation
        viewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}