package com.example.animeapp.ui.animeDetail

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.R
import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.databinding.FragmentDetailBinding
import com.example.animeapp.di.JikanApi
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodesResponse
import com.example.animeapp.ui.common.NameAndUrlAdapter
import com.example.animeapp.ui.common.UnorderedListAdapter
import com.example.animeapp.utils.BindAnimeUtils
import com.example.animeapp.utils.Const.Companion.YOUTUBE_URL
import com.example.animeapp.utils.MinMaxInputFilter
import com.example.animeapp.utils.Navigation
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.TextUtils.formatSynopsis
import com.example.animeapp.utils.TextUtils.joinOrNA
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AnimeDetailFragment : Fragment(), MenuProvider {
    @Inject
    @JikanApi
    lateinit var animeAPI: AnimeAPI

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnimeDetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        observeAnimeDetail()
        fetchAnimeDetail()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onPrepareMenu(menu: Menu) {
        val watchItem = menu.findItem(R.id.action_watch)
        watchItem?.isVisible = viewModel.episodes.value is Resource.Success
    }

    private fun observeAnimeDetail() {
        viewModel.animeDetail.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> {
                    viewModel.handleEpisodes()
                    handleAnimeSuccess(response)
                }

                is Resource.Error -> handleAnimeError(response)
                is Resource.Loading -> handleAnimeLoading()
                else -> handleAnimeEmpty()
            }
        }
        viewModel.episodes.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> handleEpisodesSuccess(response)
                is Resource.Loading -> handleEpisodesLoading()
                is Resource.Error -> handleEpisodesError(response)
                else -> handleEpisodesEmpty()
            }
        }
    }

    private fun fetchAnimeDetail(id: Int? = null) {
        val animeId = id ?: arguments?.getInt("id")
        if (animeId != null) {
            viewModel.getAnimeDetail(animeId)
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.detail_fragment_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_watch -> {
                val animeDetailData = viewModel.animeDetail.value?.data?.data
                val episodesData = viewModel.episodes.value?.data
                val defaultEpisodeServers = viewModel.defaultEpisodeServers.value
                val defaultEpisodeSources = viewModel.defaultEpisodeSources.value

                if (animeDetailData != null && episodesData != null && episodesData.episodes.isNotEmpty() && defaultEpisodeServers != null && defaultEpisodeSources != null) {
                    Navigation.navigateToAnimeWatch(
                        this,
                        R.id.action_animeDetailFragment_to_animeWatchFragment,
                        animeDetailData,
                        episodesData.episodes[0].episodeId,
                        episodesData,
                        defaultEpisodeServers,
                        defaultEpisodeSources
                    )
                    true
                } else {
                    false
                }
            }

            R.id.action_share -> {
                viewModel.animeDetail.value?.data?.data?.let { detail ->
                    val animeUrl = detail.url
                    val animeTitle = detail.title
                    val animeScore = detail.score ?: "0"
                    val animeGenres = detail.genres?.joinToString(", ") { it.name }

                    val animeSynopsis = formatSynopsis(detail.synopsis ?: "-")
                    val animeTrailerUrl = detail.trailer.url ?: ""
                    val malId = detail.mal_id
                    val customUrl = "animeapp://anime/detail/$malId"

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
                    Download the app now: https://play.google.com/store/apps/details?id=com.example.animeapp
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

    //Anime Detail
    private fun handleAnimeSuccess(response: Resource.Success<AnimeDetailResponse>) {
        binding.apply {
            shimmerViewContainer.stopShimmer()
            shimmerViewContainer.hideShimmer()
            tvError.visibility = View.GONE

            response.data?.data?.let { detail ->
                BindAnimeUtils.bindAnimeHeader(
                    requireContext(),
                    animeHeader,
                    { url ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    },
                    detail
                )

                with(animeNumberDetail) {
                    tvScore.text = detail.score?.toString() ?: "0"
                    tvScoredBy.text =
                        resources.getString(R.string.scored_by_users, detail.scored_by ?: 0)
                    tvRanked.text =
                        resources.getString(R.string.ranked_number, detail.rank ?: 0)
                    tvPopularity.text =
                        resources.getString(R.string.popularity_number, detail.popularity)
                    detail.members.toString().also { tvMembers.text = it }
                    detail.favorites.toString().also { tvFavorites.text = it }
                }

                val embedUrl = detail.trailer.embed_url ?: ""
                if (embedUrl.isNotEmpty()) {
                    llYoutubePreview.visibility = View.VISIBLE
                    youtubePlayerView.playVideo(embedUrl)
                }

                with(animeBody) {
                    tvStatus.text = detail.status
                    tvType.text = detail.type
                    tvSource.text = detail.source
                    tvSeason.text = detail.season ?: "-"
                    tvReleased.text = detail.year?.toString() ?: "-"
                    tvAired.text = detail.aired.string
                    tvRating.text = detail.rating ?: "Unknown"
                    tvGenres.text = joinOrNA(detail.genres) { it.name }
                    detail.episodes.toString().also { tvEpisodes.text = it }

                    tvStudios.text = joinOrNA(detail.studios) { it.name }
                    tvProducers.text = joinOrNA(detail.producers) { it.name }
                    tvLicensors.text = joinOrNA(detail.licensors) { it.name }
                    tvBroadcast.text = detail.broadcast.string ?: "-"
                    tvDuration.text = detail.duration
                }

                with(animeBackground) {
                    detail.background?.let { background ->
                        if (background.isNotBlank()) {
                            llBackground.visibility = View.VISIBLE
                            tvBackground.text = background
                        } else {
                            llBackground.visibility = View.GONE
                        }
                    }
                }

                with(animeSynopsis) {
                    detail.synopsis?.let { synopsis ->
                        if (synopsis.isNotBlank()) {
                            tvSynopsis.visibility = View.VISIBLE
                            tvSynopsis.text = synopsis
                        } else {
                            tvSynopsis.visibility = View.GONE
                        }
                    }
                }

                with(animeRelation) {
                    if (detail.relations?.size!! > 0) {
                        if (detail.relations.size > 1) "${detail.relations.size} Relations".also {
                            tvRelation.text = it
                        }
                        rvRelations.apply {
                            adapter = RelationsAdapter(animeAPI, detail.relations) { animeId ->
                                scrollView.post {
                                    ObjectAnimator.ofInt(scrollView, "scrollY", 0)
                                        .setDuration(1000)
                                        .start()
                                }
                                fetchAnimeDetail(animeId)
                            }
                            layoutManager = LinearLayoutManager(
                                requireContext(), LinearLayoutManager.HORIZONTAL, false
                            )
                        }
                    } else {
                        relationContainer.visibility = View.GONE
                    }
                }

                with(animeOpening) {
                    if (detail.theme.openings?.size!! > 0) {
                        openingContainer.visibility = View.VISIBLE
                        rvOpening.apply {
                            adapter = detail.theme.openings.let {
                                UnorderedListAdapter(it) { opening ->
                                    val encodedOpening = Uri.encode(opening)
                                    val youtubeSearchUrl =
                                        "${YOUTUBE_URL}/results?search_query=$encodedOpening"
                                    val intent =
                                        Intent(Intent.ACTION_VIEW, Uri.parse(youtubeSearchUrl))
                                    startActivity(intent)
                                }
                            }
                            layoutManager = LinearLayoutManager(
                                requireContext()
                            )
                            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                        }
                    } else {
                        openingContainer.visibility = View.GONE
                    }
                }

                with(animeEnding) {
                    if (detail.theme.endings?.size!! > 0) {
                        endingContainer.visibility = View.VISIBLE
                        rvEnding.apply {
                            adapter = detail.theme.endings.let {
                                UnorderedListAdapter(it)
                                { ending ->
                                    val encodedEnding = Uri.encode(ending)
                                    val youtubeSearchUrl =
                                        "${YOUTUBE_URL}/results?search_query=$encodedEnding"
                                    val intent =
                                        Intent(Intent.ACTION_VIEW, Uri.parse(youtubeSearchUrl))
                                    startActivity(intent)
                                    startActivity(intent)
                                }
                            }
                            layoutManager = LinearLayoutManager(
                                requireContext()
                            )
                            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                        }
                    } else {
                        endingContainer.visibility = View.GONE
                    }
                }

                with(animeExternal) {
                    if (detail.external?.size!! > 0) {
                        externalContainer.visibility = View.VISIBLE
                        rvExternal.apply {
                            adapter = NameAndUrlAdapter(detail.external)
                            layoutManager = LinearLayoutManager(
                                requireContext()
                            )
                            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                        }
                    } else {
                        externalContainer.visibility = View.GONE
                    }
                }

                with(animeStreaming) {
                    if (detail.streaming?.size!! > 0) {
                        streamingContainer.visibility = View.VISIBLE
                        rvStreaming.apply {
                            adapter =
                                NameAndUrlAdapter(detail.streaming)
                            layoutManager = LinearLayoutManager(
                                requireContext()
                            )
                            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                        }
                    } else {
                        streamingContainer.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun handleAnimeLoading() {
        binding.apply {
            shimmerViewContainer.showShimmer(true)
            shimmerViewContainer.startShimmer()

            tvError.visibility = View.GONE

            hideAnimeDetailContent()
        }
    }

    private fun handleAnimeError(response: Resource.Error<AnimeDetailResponse>) {
        binding.apply {
            shimmerViewContainer.stopShimmer()
            shimmerViewContainer.hideShimmer()

            tvError.visibility = View.VISIBLE

            hideAnimeDetailContent()

            response.message.also { tvError.text = it }
        }
    }

    private fun handleAnimeEmpty() {
        binding.apply {
            shimmerViewContainer.stopShimmer()
            shimmerViewContainer.hideShimmer()

            tvError.visibility = View.VISIBLE

            hideAnimeDetailContent()
            "No results found".also { tvError.text = it }
        }
    }

    private fun hideAnimeDetailContent() {
        binding.apply {
            llYoutubePreview.visibility = View.GONE
            animeBackground.llBackground.visibility = View.GONE
            animeSynopsis.tvSynopsis.visibility = View.GONE

            animeOpening.openingContainer.visibility = View.GONE
            animeEnding.endingContainer.visibility = View.GONE
            animeExternal.externalContainer.visibility = View.GONE
            animeStreaming.streamingContainer.visibility = View.GONE
        }
    }

    //Episodes
    private fun handleEpisodesSuccess(response: Resource.Success<EpisodesResponse>) {
        requireActivity().invalidateMenu()
        binding.animeDetailEpisodes.apply {
            progressBar.visibility = View.GONE
            tvEpisodeError.visibility = View.GONE
            response.data?.episodes?.let { episodes ->
                if (episodes.isNotEmpty()) {
                    if (episodes.size >= 5) {
                        etEpisodeNumber.visibility = View.VISIBLE
                        btnJumpToEpisode.visibility = View.VISIBLE
                        etEpisodeNumber.filters =
                            arrayOf(MinMaxInputFilter.createInt(1, episodes.size))
                        btnJumpToEpisode.setOnClickListener {
                            val episodeNumberText = etEpisodeNumber.text.toString()
                            if (episodeNumberText.isNotEmpty()) {
                                val episodeNumber = episodeNumberText.toInt()
                                handleJumpToEpisode(episodeNumber, episodes)
                            } else {
                                handleJumpToEpisode(1, episodes)
                            }
                        }
                    }

                    viewModel.animeEpisodeInfo.value?.episodes?.let { episodeInfo ->
                        episodesField.visibility = View.VISIBLE

                        episodeInfo.sub?.let { sub ->
                            sub.toString().also { tvSubNumber.text = it }
                        } ?: run {
                            subNumberContainer.visibility = View.GONE
                        }
                        episodeInfo.dub?.let { dub ->
                            dub.toString().also { tvDubNumber.text = it }
                        } ?: run {
                            dubNumberContainer.visibility = View.GONE
                        }
                        episodeInfo.eps?.let { eps ->
                            eps.toString().also { tvEpisodeNumber.text = it }
                        } ?: run {
                            episodeNumberContainer.visibility = View.GONE
                        }
                    }

                    rvEpisodes.visibility = View.VISIBLE

                    rvEpisodes.apply {
                        adapter = EpisodesAdapter(requireContext(), episodes) { episodeId ->
                            Navigation.navigateToAnimeWatch(
                                this@AnimeDetailFragment,
                                R.id.action_animeDetailFragment_to_animeWatchFragment,
                                viewModel.animeDetail.value!!.data!!.data,
                                episodeId,
                                viewModel.episodes.value!!.data!!,
                                viewModel.defaultEpisodeServers.value!!,
                                viewModel.defaultEpisodeSources.value!!
                            )
                        }
                        layoutManager = LinearLayoutManager(requireContext())
                    }
                }
            }
        }
    }

    private fun handleJumpToEpisode(episodeNumber: Int, episodes: List<Episode>) {
        val foundEpisodeIndex = episodes.indexOfFirst { it.episodeNo == episodeNumber }

        if (foundEpisodeIndex != -1) {
            binding.animeDetailEpisodes.rvEpisodes.apply {
                (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    foundEpisodeIndex,
                    0
                )
            }
        }
    }

    private fun handleEpisodesError(response: Resource.Error<EpisodesResponse>) {
        binding.animeDetailEpisodes.apply {
            progressBar.visibility = View.GONE
            tvEpisodeError.visibility = View.VISIBLE

            hideEpisodesContent()

            response.message.also { tvEpisodeError.text = it }
        }
    }

    private fun handleEpisodesLoading() {
        binding.animeDetailEpisodes.apply {
            progressBar.visibility = View.VISIBLE
            tvEpisodeError.visibility = View.GONE

            hideEpisodesContent()
        }
    }

    private fun handleEpisodesEmpty() {
        binding.animeDetailEpisodes.apply {
            progressBar.visibility = View.GONE
            tvEpisodeError.visibility = View.VISIBLE

            hideEpisodesContent()
            "No episodes found".also { tvEpisodeError.text = it }
        }
    }

    private fun hideEpisodesContent() {
        binding.animeDetailEpisodes.apply {
            episodesField.visibility = View.GONE
            etEpisodeNumber.visibility = View.GONE
            btnJumpToEpisode.visibility = View.GONE
            rvEpisodes.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}