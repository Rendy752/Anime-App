package com.example.animeapp.ui.animeWatch

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Rational
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.animeapp.R
import com.example.animeapp.databinding.FragmentAnimeWatchBinding
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeServersResponse
import com.example.animeapp.models.EpisodeSourcesResponse
import com.example.animeapp.models.EpisodeWatch
import com.example.animeapp.models.EpisodesResponse
import com.example.animeapp.utils.BindAnimeUtils
import com.example.animeapp.utils.Debounce
import com.example.animeapp.utils.HlsPlayerUtil
import com.example.animeapp.utils.HlsPlayerUtil.abandonAudioFocus
import com.example.animeapp.utils.HlsPlayerUtil.requestAudioFocus
import com.example.animeapp.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnimeWatchFragment : Fragment() {

    private var _binding: FragmentAnimeWatchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnimeWatchViewModel by viewModels()

    interface OnFullscreenRequestListener {
        fun onFullscreenRequested(fullscreen: Boolean)
    }

    private var mListener: OnFullscreenRequestListener? = null
    private lateinit var audioManager: AudioManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFullscreenRequestListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement OnFullscreenRequestListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimeWatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInitialData()
        setupObservers()
    }

    private fun setupInitialData() {
        val episodeId = arguments?.getString("episodeId") ?: return
        val animeDetail: AnimeDetail = getParcelableArgument("animeDetail") ?: return
        val episodes: EpisodesResponse = getParcelableArgument("episodes") ?: return
        val defaultEpisodeServers: EpisodeServersResponse? =
            getParcelableArgument("defaultEpisodeServers")
        val defaultEpisodeSources: EpisodeSourcesResponse? =
            getParcelableArgument("defaultEpisodeSources")

        viewModel.setInitialState(
            animeDetail,
            episodes,
            defaultEpisodeServers,
            defaultEpisodeSources
        )
        viewModel.handleSelectedEpisode(episodeId)
    }

    private inline fun <reified T : Any> getParcelableArgument(key: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("defaultEpisodeSources")

        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.episodeWatch.collect { response ->
                when (response) {
                    is Resource.Success -> handleEpisodeWatchSuccess(response)
                    is Resource.Error -> handleEpisodeWatchError(response)
                    is Resource.Loading -> handleEpisodeWatchLoading()
                }
            }
        }
    }

    private fun handleEpisodeWatchSuccess(response: Resource.Success<EpisodeWatch>) {
        response.data?.let { episodeWatch ->
            binding.apply {
                episodeWatch.servers.let { servers ->
                    viewModel.episodes.value.let { episodes ->
                        val episodeName = episodes?.episodes?.find { episode ->
                            episode.episodeId == servers.episodeId
                        }?.name
                        tvEpisodeTitle.text = episodeName ?: "Episode Title"
                        "Total Episode: ${episodes?.totalEpisodes}".also {
                            tvTotalEpisodes.text = it
                        }

                        val debounce = Debounce(
                            lifecycleScope,
                            1000L,
                            { query ->
                                handleJumpToEpisode(
                                    query.toInt(),
                                    episodes?.episodes ?: emptyList()
                                )
                            }
                        )

                        svEpisodeSearch.setOnQueryTextListener(object : OnQueryTextListener {
                            override fun onQueryTextSubmit(query: String?): Boolean {
                                return true
                            }

                            override fun onQueryTextChange(newText: String?): Boolean {
                                newText?.let { debounce.query(it) }
                                return true
                            }
                        })
                    }
                    "Eps. ${servers.episodeNo}".also { tvCurrentEpisode.text = it }

                    //server adapter


                }
                episodeWatch.sources.let { sources ->
                    setupVideoPlayer(sources)
                }

                viewModel.animeDetail.value.let { animeDetail ->
                    BindAnimeUtils.bindAnimeHeader(
                        requireContext(),
                        animeHeader,
                        { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                        },
                        animeDetail!!
                    )

                    val embedUrl = animeDetail.trailer.embed_url ?: ""
                    if (embedUrl.isNotEmpty()) {
                        llYoutubePreview.visibility = View.VISIBLE
                        youtubePlayerView.playVideo(embedUrl)
                    }

                    with(animeSynopsis) {
                        animeDetail.synopsis?.let { synopsis ->
                            if (synopsis.isNotBlank()) {
                                tvSynopsis.visibility = View.VISIBLE
                                tvSynopsis.text = synopsis
                            } else {
                                tvSynopsis.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleEpisodeWatchError(response: Resource.Error<EpisodeWatch>) {
        // Handle error
    }

    private fun handleEpisodeWatchLoading() {

    }

    private fun setupVideoPlayer(sources: EpisodeSourcesResponse) {
        audioManager = requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        binding.playerViewContainer.apply {
            val player = ExoPlayer.Builder(requireActivity()).build()
            playerView.player = player

            HlsPlayerUtil.initializePlayer(
                player,
                skipButton,
                sources
            )

            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)

                    if (isPlaying) {
                        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


                        requestAudioFocus(audioManager)
                    } else {
                        abandonAudioFocus(audioManager)
                    }

                    requireActivity().setPictureInPictureParams(
                        PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(16, 9))
                            .build()
                    )
                }
            })
            expandButton.setOnClickListener {
                val currentFullscreenState = isFullscreen
                isFullscreen = !currentFullscreenState

            }

            playerView.setControllerVisibilityListener(
                PlayerView.ControllerVisibilityListener { visibility ->
                    if (visibility == View.VISIBLE) {
                        pipButton.visibility = View.VISIBLE
                        expandButton.visibility = View.VISIBLE
                    } else {
                        pipButton.visibility = View.GONE
                        expandButton.visibility = View.GONE
                    }
                }
            )

            pipButton.setOnClickListener {
                handleEnterPictureInPictureMode()
            }
        }
    }

    private var isFullscreen = false
        set(value) {
            field = value
            if (value) {
                handleEnterFullscreen()
            } else {
                handleExitFullscreen()
            }
        }

    private fun handleExitFullscreen() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mListener?.onFullscreenRequested(false)
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT.also {
                requireActivity().requestedOrientation = it
            }
            binding.playerViewContainer.apply {
                playerView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                playerView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                expandButton.setImageResource(R.drawable.ic_fullscreen_black_24dp)
            }
        }
    }

    private fun handleEnterFullscreen() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mListener?.onFullscreenRequested(true)
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE.also {
                requireActivity().requestedOrientation = it
            }
            binding.playerViewContainer.apply {
                playerView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                playerView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                expandButton.setImageResource(R.drawable.ic_fullscreen_exit_black_24dp)
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)

        binding.apply {
            if (isInPictureInPictureMode) {
                hideUiPictureInPictureMode()
            } else {
                restoreUiPictureInPictureMode()
            }
        }
    }

    private fun handleEnterPictureInPictureMode() {
        val pipParams = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        requireActivity().enterPictureInPictureMode(pipParams)
    }

    private fun restoreUiPictureInPictureMode() {
        binding.apply {
            playerViewContainer.skipButton.visibility = View.VISIBLE
            playerViewContainer.pipButton.visibility = View.VISIBLE
            playerViewContainer.expandButton.visibility = View.VISIBLE
        }
    }

    private fun hideUiPictureInPictureMode() {
        binding.apply {
            playerViewContainer.skipButton.visibility = View.GONE
            playerViewContainer.pipButton.visibility = View.GONE
            playerViewContainer.expandButton.visibility = View.GONE
        }
    }

    private fun handleJumpToEpisode(episodeNumber: Int, episodes: List<Episode>) {
        val foundEpisodeIndex = episodes.indexOfFirst { it.episodeNo == episodeNumber }

        if (foundEpisodeIndex != -1) {
            binding.rvEpisodes.apply {
                (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    foundEpisodeIndex,
                    0
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        HlsPlayerUtil.releasePlayer(binding.playerViewContainer.playerView)
        _binding = null
    }
}