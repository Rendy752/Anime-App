package com.example.animeapp.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.animeapp.MainActivity
import com.example.animeapp.databinding.FragmentDetailBinding
import com.example.animeapp.ui.animerecommendations.AnimeRecommendationsViewModel
import com.example.animeapp.ui.home.DetailViewModel

class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    lateinit var viewModel: AnimeRecommendationsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = (activity as MainActivity).viewModel
        _binding = FragmentDetailBinding.inflate(inflater, container, false)

        val detailViewModel = ViewModelProvider(this).get(DetailViewModel::class.java)
        detailViewModel.text.observe(viewLifecycleOwner) {
            binding.textHome.text = it
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}