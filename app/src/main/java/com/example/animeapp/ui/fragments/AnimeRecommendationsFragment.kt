package com.example.animeapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.animeapp.ui.activities.MainActivity
import com.example.animeapp.R
import com.example.animeapp.ui.adapters.AnimeRecommendationsAdapter
import com.example.animeapp.databinding.FragmentRecommendationBinding
import com.example.animeapp.ui.viewmodels.AnimeRecommendationsViewModel
import com.example.animeapp.utils.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AnimeRecommendationsFragment : Fragment() {
    private var _binding: FragmentRecommendationBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AnimeRecommendationsViewModel
    private lateinit var animeRecommendationsAdapter: AnimeRecommendationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecommendationBinding.inflate(inflater, container, false)
        viewModel = (activity as MainActivity).animeRecommendationsViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        setupRefreshFloatingActionButton()
    }

    private fun setupRecyclerView() {
        animeRecommendationsAdapter = AnimeRecommendationsAdapter()
        binding.rvAnimeRecommendations.apply {
            adapter = animeRecommendationsAdapter
            layoutManager = LinearLayoutManager(activity)
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.animeRecommendations.collectLatest { response ->
                when (response) {
                    is Resource.Success -> {
                        response.data?.let { animeResponse ->
                            animeRecommendationsAdapter.setLoading(false)
                            animeRecommendationsAdapter.differ.submitList(animeResponse.data)
                        }
                    }
                    is Resource.Error -> {
                        animeRecommendationsAdapter.setLoading(false)
                    }
                    is Resource.Loading -> {
                        animeRecommendationsAdapter.setLoading(true)
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        animeRecommendationsAdapter.setOnItemClickListener { animeId ->
            val bundle = Bundle().apply {
                putInt("id", animeId)
            }
            val navOptions = NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_left)
                .setPopExitAnim(R.anim.slide_out_right)
                .build()

            findNavController().navigate(
                R.id.action_animeRecommendationsFragment_to_animeDetailFragment,
                bundle,
                navOptions
            )
        }
    }

    private fun setupRefreshFloatingActionButton() {
        binding.fabRefresh.setOnClickListener {
            viewModel.refreshData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}