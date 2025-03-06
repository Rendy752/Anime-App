package com.example.animeapp.ui.animeWatch

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Rational
import androidx.fragment.app.Fragment
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.os.Handler
import android.os.Looper
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.R
import androidx.media3.ui.R as RMedia3
import com.example.animeapp.databinding.FragmentAnimeWatchBinding
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeServersResponse
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.models.EpisodeSourcesResponse
import com.example.animeapp.models.EpisodeWatch
import com.example.animeapp.models.EpisodesResponse
import com.example.animeapp.models.Server
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
    private var mediaSession: MediaSession? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupInitialData()
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

        setupBackButtonListener()
        handleStaticAnimeDetailData()
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

        (requireActivity() as AppCompatActivity).supportActionBar?.title = animeDetail.title
        viewModel.setInitialState(
            animeDetail,
            episodes,
            defaultEpisodeServers,
            defaultEpisodeSources
        )
        viewModel.handleSelectedEpisodeServer(episodeId)
    }

    private inline fun <reified T : Any> getParcelableArgument(key: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("defaultEpisodeSources")

        }
    }

    private fun handleStaticAnimeDetailData() {
        binding.apply {
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

    private fun setupBackButtonListener() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFullscreen) {
                    isFullscreen = false
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.episodeWatch.collect { response ->
                when (response) {
                    is Resource.Success -> handleEpisodeWatchSuccess(response)
                    is Resource.Error -> handleEpisodeWatchError()
                    is Resource.Loading -> handleEpisodeWatchLoading()
                }
            }
        }
    }

    private fun setupServerRecyclerView(
        textView: View,
        recyclerView: RecyclerView,
        servers: List<Server>,
        category: String,
        episodeId: String
    ) {
        if (servers.isNotEmpty()) {
            textView.visibility = View.VISIBLE
            val serverQueries = servers.map { server ->
                EpisodeSourcesQuery(episodeId, server.serverName, category)
            }
            recyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = viewModel.episodeSourcesQuery.value?.let {
                    ServerAdapter(serverQueries, it) { episodeSourcesQuery ->
                        viewModel.handleSelectedEpisodeServer(episodeId, episodeSourcesQuery)
                    }
                }
            }
        } else {
            textView.visibility = View.GONE
        }
    }

    private fun setupEpisodeRecyclerView(episodes: List<Episode>, episodeNo: Int) {
        binding.apply {
            episodeSearchContainer.visibility = if (episodes.size > 1) View.VISIBLE else View.GONE
            rvEpisodes.apply {
                visibility = if (episodes.isNotEmpty()) View.VISIBLE else View.GONE
                if (episodes.isNotEmpty()) {
                    layoutManager = GridLayoutManager(context, 4)
                    adapter = EpisodesWatchAdapter(requireContext(), episodes, episodeNo) {
                        viewModel.handleSelectedEpisodeServer(it)
                    }
                    handleJumpToEpisode(episodeNo, episodes)
                }
            }
        }
    }

    private fun handleEpisodeWatchSuccess(response: Resource.Success<EpisodeWatch>) {
        response.data?.let { episodeWatch ->
            binding.apply {
                setupVideoPlayer(episodeWatch.sources)
                episodeInfoProgressBar.visibility = View.GONE
                episodeProgressBar.visibility = View.GONE
                tvEpisodeTitle.visibility = View.VISIBLE
                tvCurrentEpisode.visibility = View.VISIBLE
                serverScrollView.visibility = View.VISIBLE
                tvTotalEpisodes.visibility = View.VISIBLE
                svEpisodeSearch.visibility = View.VISIBLE

                episodeWatch.servers.let { servers ->
                    viewModel.episodes.value.let { episodes ->
                        val episodeName = episodes?.episodes?.find { episode ->
                            episode.episodeId == servers.episodeId
                        }?.name
                        tvEpisodeTitle.text =
                            if (episodeName != "Full") episodeName else viewModel.animeDetail.value?.title
                                ?: ""
                        episodes?.episodes?.find { it.episodeId == servers.episodeId }
                            ?.let { episode ->
                                val backgroundColor = if (episode.filler) {
                                    ContextCompat.getColor(requireContext(), R.color.filler_episode)
                                } else {
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.default_episode
                                    )
                                }
                                tvEpisodeTitle.setTextColor(backgroundColor)
                            }

                        "Total Episode: ${episodes?.totalEpisodes}".also {
                            tvTotalEpisodes.text = it
                        }
                        "Eps. ${servers.episodeNo}".also { tvCurrentEpisode.text = it }


                        val debounce = Debounce(
                            lifecycleScope,
                            1000L,
                            { query ->
                                if (query.isNotEmpty()) {
                                    handleJumpToEpisode(
                                        query.toInt(),
                                        episodes?.episodes ?: emptyList()
                                    )
                                }
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

                        setupEpisodeRecyclerView(
                            episodes?.episodes ?: emptyList(),
                            servers.episodeNo
                        )
                    }
                    setupServerRecyclerView(
                        tvSub,
                        rvSubServer,
                        servers.sub,
                        "sub",
                        servers.episodeId
                    )
                    setupServerRecyclerView(
                        tvDub,
                        rvDubServer,
                        servers.dub,
                        "dub",
                        servers.episodeId
                    )
                    setupServerRecyclerView(
                        tvRaw,
                        rvRawServer,
                        servers.raw,
                        "raw",
                        servers.episodeId
                    )
                }
            }
        }
    }

    private fun handleEpisodeWatchError() {
        viewModel.handleSelectedEpisodeServer(viewModel.episodes.value!!.episodes.first().episodeId)
    }

    private fun handleEpisodeWatchLoading() {
        binding.apply {
            episodeInfoProgressBar.visibility = View.VISIBLE
            episodeProgressBar.visibility = View.VISIBLE
            tvEpisodeTitle.visibility = View.GONE
            tvCurrentEpisode.visibility = View.GONE
            serverScrollView.visibility = View.GONE
            tvTotalEpisodes.visibility = View.GONE
            svEpisodeSearch.visibility = View.GONE
            rvEpisodes.visibility = View.GONE
        }
    }

    @OptIn(UnstableApi::class)
    private fun setupVideoPlayer(sources: EpisodeSourcesResponse) {
        audioManager = requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager

        mediaSession?.release()
        mediaSession = null
        HlsPlayerUtil.releasePlayer(binding.playerViewContainer.playerView)

        binding.playerViewContainer.apply {
            val exoPlayer = ExoPlayer.Builder(requireActivity()).build()
            with(playerView) {
                player = exoPlayer
                setShowPreviousButton(false)
                setShowNextButton(false)

                setFullscreenButtonState(true)
                setFullscreenButtonClickListener {
                    val currentFullscreenState = isFullscreen
                    isFullscreen = !currentFullscreenState
                }

                var isHolding = false
                val handler = Handler(Looper.getMainLooper())
                val holdRunnable = Runnable {
                    if (isHolding) {
                        exoPlayer.playbackParameters =
                            exoPlayer.playbackParameters.withSpeed(2f)
                        useController = false
                        pipButton.visibility = View.GONE
                        speedUpContainer.visibility = View.VISIBLE
                        "2x speed".also { tvSpeedUp.text = it }
                    }
                }

                playerView.setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            isHolding = true
                            handler.postDelayed(holdRunnable, 1000)
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            handler.removeCallbacks(holdRunnable)
                            exoPlayer.playbackParameters =
                                exoPlayer.playbackParameters.withSpeed(1f)
                            useController = true
                            speedUpContainer.visibility = View.GONE
                            "1x speed".also { tvSpeedUp.text = it }
                            isHolding = false
                        }

                    }

                    if (!isHolding) {
                        playerView.performClick()
                    }
                    true
                }

                setControllerVisibilityListener(
                    PlayerView.ControllerVisibilityListener { visibility ->
                        val subtitleView = playerView.subtitleView
                        val bottomBar =
                            playerView.findViewById<ViewGroup>(RMedia3.id.exo_bottom_bar)
                        val orientation = playerView.resources.configuration.orientation
                        pipButton.visibility = when (visibility) {
                            View.VISIBLE -> View.VISIBLE
                            else -> View.GONE
                        }
                        subtitleView?.setPadding(
                            0, 0, 0,
                            if (visibility == View.VISIBLE && orientation == Configuration.ORIENTATION_LANDSCAPE || (visibility == View.VISIBLE && !isFullscreen)) bottomBar.height else 0
                        )
                    }
                )
            }

            HlsPlayerUtil.initializePlayer(
                exoPlayer,
                skipButton,
                sources
            )

            val sessionId = "episode_${System.currentTimeMillis()}"
            mediaSession = MediaSession.Builder(requireActivity(), exoPlayer)
                .setId(sessionId)
                .build()

            exoPlayer.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)

                    if (isPlaying) {
                        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        requestAudioFocus(audioManager)
                    } else {
                        abandonAudioFocus(audioManager)
                    }
                    updateMediaSessionPlaybackState(exoPlayer)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == Player.STATE_READY) {
                        if (!isFullscreen) {
                            this@apply.root.layoutParams.height =
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        } else {
                            this@apply.root.layoutParams.height =
                                ViewGroup.LayoutParams.MATCH_PARENT
                        }
                    }
                }
            })

            pipButton.setOnClickListener {
                handleEnterPictureInPictureMode()
            }
        }
    }

    private fun updateMediaSessionPlaybackState(player: Player) {
        val playbackState = when (player.playbackState) {
            Player.STATE_IDLE -> PlaybackStateCompat.STATE_NONE
            Player.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
            Player.STATE_READY -> if (player.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            Player.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
            else -> PlaybackStateCompat.STATE_NONE
        }

        PlaybackStateCompat.Builder()
            .setState(playbackState, player.currentPosition, 1f, System.currentTimeMillis())
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE)

        mediaSession?.setPlayer(player)
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
        mListener?.onFullscreenRequested(false)
        binding.svContent.visibility = View.VISIBLE
        binding.playerViewContainer.apply {
            root.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            playerView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }

    private fun handleEnterFullscreen() {
        mListener?.onFullscreenRequested(true)
        binding.svContent.visibility = View.GONE
        binding.playerViewContainer.apply {
            root.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            playerView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    fun handleEnterPictureInPictureMode() {
        val pipParams = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        requireActivity().enterPictureInPictureMode(pipParams)
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

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        binding.playerViewContainer.playerView.useController = !isInPictureInPictureMode
    }

    override fun onDestroyView() {
        super.onDestroyView()
        HlsPlayerUtil.releasePlayer(binding.playerViewContainer.playerView)
        mediaSession?.release()
        mediaSession = null
        _binding = null
    }
}