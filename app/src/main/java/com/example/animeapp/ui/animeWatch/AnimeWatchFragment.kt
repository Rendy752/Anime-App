package com.example.animeapp.ui.animeWatch

import android.os.Build
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.example.animeapp.databinding.FragmentAnimeWatchBinding
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.EpisodeServersResponse
import com.example.animeapp.models.EpisodeSourcesResponse
import com.example.animeapp.models.EpisodeWatch
import com.example.animeapp.models.EpisodesResponse
import com.example.animeapp.utils.HlsPlayerUtil
import com.example.animeapp.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnimeWatchFragment : Fragment() {

    private var _binding: FragmentAnimeWatchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnimeWatchViewModel by viewModels()

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
                episodeWatch.servers.let {

                }
                episodeWatch.sources.let { sources ->

                }
            }
        }
    }

    private fun handleEpisodeWatchError(response: Resource.Error<EpisodeWatch>) {
        // Handle error
    }

    private fun handleEpisodeWatchLoading() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        HlsPlayerUtil.releasePlayer(binding.playerViewContainer.playerView)
        _binding = null
    }
}