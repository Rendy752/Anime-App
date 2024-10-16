package com.example.animeapp.ui.detail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.example.animeapp.MainActivity
import com.example.animeapp.databinding.FragmentDetailBinding
import com.example.animeapp.ui.home.DetailViewModel
import com.example.animeapp.utils.Resource

class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    lateinit var viewModel: DetailViewModel

    val TAG = "AnimeDetailFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = (activity as MainActivity).detailViewModel
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val animeId = arguments?.getString("id")
        if (animeId != null) {
            viewModel.getAnimeDetail(animeId.toInt())
        }

        viewModel.animeDetail.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Success -> {
                    hideProgressBar()
                    response.data?.let { animeDetail ->
                        binding.textTitle.text = animeDetail.data.title
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

        return root
    }

    private fun hideProgressBar() {
        binding.paginationProgressBar.visibility = View.INVISIBLE
    }

    private fun showProgressBar() {
        binding.paginationProgressBar.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}