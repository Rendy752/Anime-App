package com.example.animeapp.ui.animerecommendations

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.animeapp.MainActivity
import com.example.animeapp.R
import com.example.animeapp.adapters.AnimeRecommendationsAdapter
import com.example.animeapp.databinding.FragmentRecommendationBinding
import com.example.animeapp.utils.Resource

class AnimeRecommendationsFragment : Fragment() {
    private var _binding: FragmentRecommendationBinding? = null
    private val binding get() = _binding!!
    lateinit var viewModel: AnimeRecommendationsViewModel
    lateinit var animeRecommendationsAdapter: AnimeRecommendationsAdapter

    val TAG = "AnimeRecommendationsFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = (activity as MainActivity).animeRecommendationsViewModel

        _binding = FragmentRecommendationBinding.inflate(inflater, container, false)
        val root: View = binding.root

        viewModel = (activity as MainActivity).animeRecommendationsViewModel
        setupRecyclerView()
        viewModel.animeRecommendations.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Success -> {
                    hideProgressBar()
                    response.data?.let { animeResponse ->
                        animeRecommendationsAdapter.differ.submitList(animeResponse.data)
                    }
                }

                is Resource.Error -> {
                    hideProgressBar()
                    response.message?.let { message ->
                        Log.e(TAG, "An error occured: ${message}")
                    }
                }

                is Resource.Loading -> {
                    showProgressBar()
                }
            }
        })

        animeRecommendationsAdapter.setOnAnimeTitleClickListener { animeId ->
            val bundle = Bundle().apply {
                putString("id", animeId)
            }
            findNavController().navigate(
                R.id.action_animeRecommendationsFragment_to_detailFragment,
                bundle
            )
        }

        return root
    }

    private fun hideProgressBar() {
        binding.paginationProgressBar.visibility = View.INVISIBLE
    }

    private fun showProgressBar() {
        binding.paginationProgressBar.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
        animeRecommendationsAdapter = AnimeRecommendationsAdapter()
        Log.d("AnimeRecommendationsFragment", "setupRecyclerView: ${animeRecommendationsAdapter}")
        binding.rvAnimeRecommendations.apply {
            adapter = animeRecommendationsAdapter
            layoutManager = LinearLayoutManager(activity)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}