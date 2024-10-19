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
//                    data class AnimeDetail(
//                        val mal_id: Int,
//                        val url: String,
//                        val images: Images,
//                        val trailer: Trailer,
//                        val approved: Boolean,
//                        val titles: List<Title>,
//                        val title: String,
//                        val title_english: String?,
//                        val title_japanese: String?,
//                        val title_synonyms: Array<String>,
//                        val type: String,
//                        val source: String,
//                        val episodes: Int,
//                        val status: String,
//                        val airing: Boolean,
//                        val aired: Aired,
//                        val duration: String,
//                        val rating: String,
//                        val score: Double,
//                        val scored_by: Int,
//                        val rank: Int,
//                        val popularity: Int,
//                        val members: Int,
//                        val favorites: Int,
//                        val synopsis: String,
//                        val background: String,
//                        val season: String?,
//                        val year: Int,
//                        val broadcast: Broadcast,
//                        val producers: List<CommonIdentity>,
//                        val licensors: List<CommonIdentity>,
//                        val studios: List<CommonIdentity>,
//                        val genres: List<CommonIdentity>,
//                        val explicit_genres: List<CommonIdentity>,
//                        val themes: List<CommonIdentity>,
//                        val demographics: List<CommonIdentity>,
//                        val relations: List<Relation>,
//                        val theme: Theme,
//                        val external: List<NameAndUrl>,
//                        val streaming: List<NameAndUrl>
//                    )
                    response.data?.data?.let { detail ->
                        Glide.with(this)
                            .load(detail.images.jpg.large_image_url)
                            .into(binding.ivAnimeImage)
                        binding.tvTitle.text = detail.title
                        binding.tvEnglishTitle.text = detail.title_english
                        binding.rvTitleSynonyms.apply {
                            adapter = TitleSynonymsAdapter(detail.title_synonyms.toList())
                            layoutManager = LinearLayoutManager(requireContext(),
                                LinearLayoutManager.HORIZONTAL, false)
                        }

//                        binding.tvUrl.text = detail.url
//                        binding.tvTrailerUrl.text = detail.trailer.url
//                        binding.tvTrailerEmbedUrl.text = detail.trailer.embed_url
//                        binding.tvApproved.text = detail.approved.toString()
//                        binding.tvJapaneseTitle.text = detail.title_japanese
                        binding.tvType.text = detail.type
                        binding.tvSource.text = detail.source
                        binding.tvEpisodes.text = detail.episodes.toString()
                        binding.tvStatus.text = detail.status
                        binding.tvAiring.text = detail.airing.toString()
//                        binding.tvAired.text = detail.aired
//                        binding.tvDuration.text = detail.duration
//                        binding.tvRating.text = detail.rating
//                        binding.tvScore.text = detail.score
//                        binding.tvScoredBy.text = detail.scored_by
//                        binding.tvRank.text = detail.rank
//                        binding.tvPopularity.text = detail.popularity
//                        binding.tvMembers.text = detail.members
//                        binding.tvFavorites.text = detail.favorites
//                        binding.tvSynopsis.text = detail.synopsis
//                        binding.tvBackground.text = detail.background
//                        binding.tvSeason.text = detail.season
//                        binding.tvYear.text = detail.year
//                        binding.tvBroadcast.text = detail.broadcast
//                        binding.tvProducers.text = detail.producers
//                        binding.tvLicensors.text = detail.licensors
//                        binding.tvStudios.text = detail.studios
//                        binding.tvGenres.text = detail.genres
//                        binding.tvExplicitGenres.text = detail.explicit_genres
//                        binding.tvThemes.text = detail.themes
//                        binding.tvDemographics.text = detail.demographics
//                        binding.tvRelations.text = detail.relations
//                        binding.tvTheme.text = detail.theme
//                        binding.tvExternal.text = detail.external
//                        binding.tvStreaming.text = detail.streaming
//                        binding.tvTitles.text = detail.titles
//                        binding.tvTrailerImagesUrl.text = detail.trailer.images.image_url

                        binding.tvTitle.background = null
                        binding.tvEnglishTitle.background = null
                        binding.tvType.background = null
                        binding.tvSource.background = null
                        binding.tvEpisodes.background = null
                        binding.tvStatus.background = null
                        binding.tvAiring.background = null
                        binding.llAnimeBody.background = null
                        binding.rvTitleSynonyms.background = null
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