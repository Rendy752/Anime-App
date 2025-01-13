package com.example.animeappkotlin.ui.AnimeDetail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import androidx.core.view.MenuProvider
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.animeappkotlin.R
import com.example.animeappkotlin.data.local.database.AnimeDetailDatabase
import com.example.animeappkotlin.databinding.FragmentDetailBinding
import com.example.animeappkotlin.models.AnimeDetailResponse
import com.example.animeappkotlin.repository.AnimeDetailRepository
import com.example.animeappkotlin.ui.common.TitleSynonymsAdapter
import com.example.animeappkotlin.utils.Navigation
import com.example.animeappkotlin.utils.Resource
import com.example.animeappkotlin.utils.TextUtils.formatSynopsis
import com.example.animeappkotlin.utils.TextUtils.joinOrNA

class AnimeDetailFragment : Fragment(), MenuProvider {
    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnimeDetailViewModel by viewModels {
        val animeDetailRepository = AnimeDetailRepository(
            animeDetailDao = AnimeDetailDatabase.getDatabase(requireActivity()).getAnimeDetailDao()
        )
        AnimeDetailViewModelProviderFactory(animeDetailRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        observeAnimeDetail()
        fetchAnimeDetail()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observeAnimeDetail() {
        viewModel.animeDetail.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> handleSuccess(response)
                is Resource.Error -> handleError(response)
                is Resource.Loading -> handleLoading()
                else -> handleEmpty()
            }
        }
    }

    private fun fetchAnimeDetail() {
        val animeId = arguments?.getInt("id")
        if (animeId != null) {
            viewModel.getAnimeDetail(animeId)
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.detail_fragment_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_share -> {
                viewModel.animeDetail.value?.data?.data?.let { detail ->
                    val animeUrl = detail.url
                    val animeTitle = detail.title
                    val animeScore = detail.score ?: "0"
                    val animeGenres = detail.genres?.joinToString(", ") { it.name }

                    val animeSynopsis = formatSynopsis(detail.synopsis ?: "-")
                    val animeTrailerUrl = detail.trailer.url ?: ""
                    val malId = detail.mal_id
                    val customUrl = "animeappkotlin://anime/detail/$malId"

                    val trailerSection = if (animeTrailerUrl.isNotEmpty()) {
                        """
                            
                    -------
                    Trailer
                    -------
                    $animeTrailerUrl
                    """
                    } else {
                        ""
                    }

                    val sharedText = """
                    Check out this anime on AnimeApp!

                    Title: $animeTitle
                    Score: $animeScore
                    Genres: $animeGenres

                    --------
                    Synopsis
                    --------
                    $animeSynopsis
                    $trailerSection

                    Web URL: $animeUrl
                    App URL: $customUrl
                    Download the app now: https://play.google.com/store/apps/details?id=com.example.animeappkotlin
                """.trimIndent()

                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, sharedText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    startActivity(shareIntent)
                }
                true
            }

            else -> false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun handleSuccess(response: Resource.Success<AnimeDetailResponse>) {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.hideShimmer()
        binding.tvError.visibility = View.GONE

        response.data?.data?.let { detail ->
            Glide.with(this).load(detail.images.jpg.large_image_url)
                .into(binding.ivAnimeImage)
            binding.tvTitle.text = detail.title
            binding.tvTitle.setOnClickListener {
                viewModel.animeDetail.value?.data?.data?.let { detail ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(detail.url))
                    startActivity(intent)
                }
            }
            binding.tvEnglishTitle.text = detail.title_english
            binding.tvJapaneseTitle.text = detail.title_japanese
            binding.rvTitleSynonyms.apply {
                adapter = detail.title_synonyms?.let { TitleSynonymsAdapter(it.toList()) }
                layoutManager = LinearLayoutManager(
                    requireContext(), LinearLayoutManager.HORIZONTAL, false
                )
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
                    binding.tvAiredStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_notifications_active_24dp, 0);
                }

                false -> {
                    binding.tvAiredStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_done_24dp, 0);
                }
            }

            binding.tvStatus.text = detail.status
            binding.tvType.text = detail.type
            binding.tvSource.text = detail.source
            binding.tvSeason.text = detail.season ?: "-"
            binding.tvReleased.text = detail.year?.toString() ?: "-"
            binding.tvAired.text = detail.aired.string
            binding.tvRating.text = detail.rating ?: "Unknown"
            binding.tvGenres.text = joinOrNA(detail.genres) { it.name }
            binding.tvEpisodes.text = detail.episodes.toString()

            binding.tvStudios.text = joinOrNA(detail.studios) { it.name }
            binding.tvProducers.text = joinOrNA(detail.producers) { it.name }
            binding.tvLicensors.text = joinOrNA(detail.licensors) { it.name }
            binding.tvBroadcast.text = detail.broadcast.string ?: "-"
            binding.tvDuration.text = detail.duration

            val embedUrl = detail.trailer.embed_url ?: ""
            if (embedUrl.isNotEmpty()) {
                binding.llYoutubePreview.visibility = View.VISIBLE
                binding.youtubePlayerView.playVideo(embedUrl)
            }

            binding.tvScore.text = detail.score?.toString() ?: "0"
            binding.tvScoredBy.text =
                resources.getString(R.string.scored_by_users, detail.scored_by ?: 0)
            binding.tvRanked.text = resources.getString(R.string.ranked_number, detail.rank ?: 0)
            binding.tvPopularity.text =
                resources.getString(R.string.popularity_number, detail.popularity)
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

            binding.tvSynopsis.text = detail.synopsis ?: "-"

            if (detail.relations?.size!! > 0) {
                if (detail.relations.size > 1) binding.tvRelation.text = "${detail.relations.size} Relations"
                binding.rvRelations.apply {
                    adapter = RelationsAdapter(detail.relations) { animeId ->
                        Navigation.navigateToAnimeDetail(
                            this@AnimeDetailFragment,
                            animeId,
                            R.id.action_animeDetailFragment_self
                        )
                    }
                    layoutManager = LinearLayoutManager(
                        requireContext(), LinearLayoutManager.HORIZONTAL, false
                    )
                }
            } else {
                binding.relationContainer.visibility = View.GONE
            }

            binding.tvOpening.text = joinOrNA(detail.theme.openings) { it }
            binding.tvEnding.text = joinOrNA(detail.theme.endings) { it }
            binding.tvExternalLinks.text = joinOrNA(detail.external) { it.name }
            binding.tvStreamingLinks.text = joinOrNA(detail.streaming) { it.name }
        }
    }

    private fun handleError(response: Resource.Error<AnimeDetailResponse>) {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.hideShimmer()
        binding.tvError.visibility = View.VISIBLE
        "An error occurred: ${response.message}".also { binding.tvError.text = it }
    }

    private fun handleLoading() {
        binding.shimmerViewContainer.showShimmer(true)
        binding.shimmerViewContainer.startShimmer()
    }

    private fun handleEmpty() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.hideShimmer()
        binding.tvError.visibility = View.VISIBLE
        "An error occurred while fetching the anime detail.".also { binding.tvError.text = it }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}