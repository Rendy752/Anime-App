package com.example.animeappkotlin.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.animeappkotlin.R
import com.example.animeappkotlin.data.local.database.AnimeRecommendationsDatabase
import com.example.animeappkotlin.data.remote.api.RetrofitInstance
import com.example.animeappkotlin.ui.adapters.AnimeRecommendationsAdapter
import com.example.animeappkotlin.databinding.FragmentRecommendationBinding
import com.example.animeappkotlin.repository.AnimeRecommendationsRepository
import com.example.animeappkotlin.ui.providerfactories.AnimeRecommendationsViewModelProviderFactory
import com.example.animeappkotlin.ui.viewmodels.AnimeRecommendationsViewModel
import com.example.animeappkotlin.utils.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AnimeRecommendationsFragment : Fragment() {
    private var _binding: FragmentRecommendationBinding? = null
    private val binding get() = _binding!!

    private lateinit var animeRecommendationsAdapter: AnimeRecommendationsAdapter

    private val viewModel: AnimeRecommendationsViewModel by viewModels {
        val animeRecommendationsRepository = AnimeRecommendationsRepository(
            api = RetrofitInstance.api,
            db = AnimeRecommendationsDatabase.getDatabase(requireActivity())
        )
        AnimeRecommendationsViewModelProviderFactory(animeRecommendationsRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecommendationBinding.inflate(inflater, container, false)
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