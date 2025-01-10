package com.example.animeappkotlin.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.animeappkotlin.R
import com.example.animeappkotlin.databinding.FragmentAnimeSearchBinding
import com.example.animeappkotlin.models.CompletePagination
import com.example.animeappkotlin.ui.activities.MainActivity
import com.example.animeappkotlin.ui.adapters.AnimeSearchAdapter
import com.example.animeappkotlin.ui.viewmodels.AnimeSearchViewModel
import com.example.animeappkotlin.utils.Debouncer
import com.example.animeappkotlin.utils.Pagination
import com.example.animeappkotlin.utils.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AnimeSearchFragment : Fragment() {

    private var _binding: FragmentAnimeSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AnimeSearchViewModel
    private lateinit var animeSearchAdapter: AnimeSearchAdapter

    private val debouncer = Debouncer(lifecycleScope) { query ->
        viewModel.searchAnime(query,viewModel.animeSearchResults.value.data?.pagination?.current_page ?: 1)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimeSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModel()
        setupRecyclerView()
        setupSearchView()
        setupObservers()
        setupClickListeners()
        setupRefreshFloatingActionButton()

        Pagination.setPaginationButtons(
            binding.paginationButtonContainer,
            null,
            onPaginationClick = { pageNumber ->
                viewModel.searchAnime(binding.searchView.query.toString(), pageNumber)
            }
        )
    }

    private fun setupViewModel() {
        viewModel = (activity as MainActivity).animeSearchViewModel
    }

    private fun setupRecyclerView() {
        animeSearchAdapter = AnimeSearchAdapter()
        binding.rvAnimeSearch.apply {
            adapter = animeSearchAdapter
            layoutManager = LinearLayoutManager(activity)
        }
    }

    private fun setupClickListeners() {
        animeSearchAdapter.setOnItemClickListener { animeId ->
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
                R.id.action_animeSearchFragment_to_animeDetailFragment,
                bundle,
                navOptions
            )
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    debouncer.query(it)
                }
                return true
            }
        })
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.animeSearchResults.collectLatest { response ->
                when (response) {
                    is Resource.Success -> {
                        response.data?.let { searchResponse ->
                            animeSearchAdapter.setLoading(false)
                            updatePagination(response.data?.pagination)
                            animeSearchAdapter.differ.submitList(searchResponse.data)
                        }
                    }
                    is Resource.Error -> {
                        animeSearchAdapter.setLoading(false)
                        Toast.makeText(requireContext(), "An error occurred: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                    is Resource.Loading -> {
                        animeSearchAdapter.setLoading(true)
                    }
                }
            }
        }
    }

    private fun setupRefreshFloatingActionButton() {
        binding.fabRefresh.setOnClickListener {
            viewModel.searchAnime(binding.searchView.query.toString(),viewModel.animeSearchResults.value.data?.pagination?.current_page ?: 1)
        }
    }

    private fun updatePagination(pagination: CompletePagination?) {
        Pagination.setPaginationButtons(
            binding.paginationButtonContainer,
            pagination,
            onPaginationClick = { pageNumber ->
                viewModel.searchAnime(binding.searchView.query.toString(), pageNumber)
            }
        )
        binding.paginationButtonContainer.visibility = if (pagination == null) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}