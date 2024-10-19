package com.example.animeapp.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.animeapp.ui.activities.MainActivity
import com.example.animeapp.databinding.FragmentDetailBinding
import com.example.animeapp.ui.adapters.TitleSynonymsAdapter
import com.example.animeapp.ui.viewmodels.AnimeDetailViewModel
import com.example.animeapp.utils.Resource

class AnimeDetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    lateinit var viewModel: AnimeDetailViewModel

    private val tag = "AnimeDetailFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = (activity as MainActivity).animeDetailViewModel
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val animeId = arguments?.getInt("id")
        if (animeId != null) {
            viewModel.getAnimeDetail(animeId)
        }

        viewModel.animeDetail.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> {
                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.hideShimmer()
                    response.data?.data?.let { detail ->
                        Glide.with(this)
                            .load(detail.images.jpg.large_image_url)
                            .into(binding.ivAnimeImage)

                        binding.tvTitle.text = detail.title
                        binding.tvEnglishTitle.text = detail.title_english
                        binding.tvJapaneseTitle.text = detail.title_japanese
                        binding.rvTitleSynonyms.apply {
                            adapter = TitleSynonymsAdapter(detail.title_synonyms.toList())
                            layoutManager = LinearLayoutManager(requireContext(),
                                LinearLayoutManager.HORIZONTAL, false)
                        }

                        when (detail.approved) {
                            true -> {
                                binding.ivApproved.visibility = View.VISIBLE
                            }
                            false -> {
                                binding.ivApproved.visibility = View.GONE
                            }
                        }
                        when (detail.airing) {
                            true -> {
                                binding.ivAired.visibility = View.VISIBLE
                                binding.ivNotAired.visibility = View.GONE
                            }
                            false -> {
                                binding.ivAired.visibility = View.GONE
                                binding.ivNotAired.visibility = View.VISIBLE
                            }
                        }

                        binding.tvStatus.text = detail.status
                        binding.tvType.text = detail.type
                        binding.tvSource.text = detail.source
                        binding.tvSeason.text = detail.season
                        binding.tvReleased.text = detail.year.toString()
                        binding.tvAired.text = detail.aired.string
                        binding.tvRating.text = detail.rating
                        binding.tvGenres.text = detail.genres.joinToString(", ") {it.name}
                        binding.tvEpisodes.text = detail.episodes.toString()

                        binding.tvStudios.text = detail.studios.joinToString(", ") { it.name }
                        binding.tvProducers.text = detail.producers.joinToString(", ") {it.name}
                        binding.tvLicensors.text = detail.licensors.joinToString(", ") {it.name}
                        binding.tvBroadcast.text = detail.broadcast.string
                        binding.tvDuration.text = detail.duration

//                        binding.tvUrl.text = detail.url
//                        binding.tvTrailerUrl.text = detail.trailer.url
                        val embedUrl = detail.trailer.embed_url ?: ""
                        if (embedUrl.isNotEmpty()) {
                            binding.llYoutubePreview.visibility = View.VISIBLE
                            binding.youtubePlayerView.playVideo(embedUrl)
                        }

                        binding.tvScore.text = detail.score.toString()
                        binding.tvScoredBy.text = "${detail.scored_by.toString()} Users"
                        binding.tvRanked.text = "#${detail.rank.toString()}"
                        binding.tvPopularity.text = "#${detail.popularity.toString()}"
                        binding.tvMembers.text = detail.members.toString()
                        binding.tvFavorites.text = detail.favorites.toString()

                        detail.background?.let { background ->
                            if (background.isNotBlank()) {
                                binding.llBackground.visibility = View.VISIBLE
                                binding.tvBackground.text = background
                            } else {
                                binding.llBackground.visibility = View.GONE
                            }
                        }

                        binding.tvSynopsis.text = detail.synopsis

//                        binding.tvExplicitGenres.text = detail.explicit_genres
//                        binding.tvRelations.text = detail.relations
//                        binding.tvTheme.text = detail.theme
//                        binding.tvExternal.text = detail.external
//                        binding.tvStreaming.text = detail.streaming
//                        binding.tvTitles.text = detail.titles
                    }
                }

                is Resource.Error -> {
                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.hideShimmer()
                    response.message?.let { message ->
                        Log.e(tag, "An error occured: ${message}")
                    }
                }

                is Resource.Loading -> {
                    binding.shimmerViewContainer.showShimmer(true)
                    binding.shimmerViewContainer.startShimmer()
                }
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}