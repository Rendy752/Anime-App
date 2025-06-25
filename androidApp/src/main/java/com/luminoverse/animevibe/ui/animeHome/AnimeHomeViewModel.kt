package com.luminoverse.animevibe.ui.animeHome

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.ListAnimeDetailResponse
import com.luminoverse.animevibe.models.AnimeSchedulesSearchQueryState
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.repository.AnimeHomeRepository
import com.luminoverse.animevibe.utils.resource.Resource
import com.luminoverse.animevibe.utils.TimeUtils.calculateRemainingTime
import com.luminoverse.animevibe.utils.workers.BroadcastNotificationWorker
import com.luminoverse.animevibe.utils.workers.UnfinishedWatchNotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*
import javax.inject.Inject

data class HomeState(
    val animeSchedules: Resource<ListAnimeDetailResponse> = Resource.Loading(),
    val top10Anime: Resource<ListAnimeDetailResponse> = Resource.Loading(),
    val queryState: AnimeSchedulesSearchQueryState = AnimeSchedulesSearchQueryState(),
    val continueWatchingEpisode: EpisodeDetailComplement? = null,
    val isRefreshing: Boolean = false,
    val isShowPopup: Boolean = false,
    val isMinimized: Boolean = false
)

data class CarouselState(
    val currentCarouselPage: Int = 0,
    val autoScrollEnabled: Boolean = true,
    val carouselLastInteractionTime: Long = Date().time
)

sealed class HomeAction {
    data object GetAnimeSchedules : HomeAction()
    data object GetTop10Anime : HomeAction()
    data class ApplyFilters(val updatedQueryState: AnimeSchedulesSearchQueryState) : HomeAction()
    data object FetchContinueWatchingEpisode : HomeAction()
    data class SetMinimized(val minimize: Boolean) : HomeAction()
    data class SetShowPopup(val show: Boolean) : HomeAction()
    data class SetAutoScrollEnabled(val enabled: Boolean) : HomeAction()
    data object UpdateCarouselLastInteractionTime : HomeAction()
    data class SetCurrentCarouselPage(val page: Int) : HomeAction()
    data class StartUpdatingBroadcastTimes(val animeSchedules: List<AnimeDetail>) : HomeAction()
}

@HiltViewModel
class AnimeHomeViewModel @Inject constructor(
    private val application: Application,
    private val animeHomeRepository: AnimeHomeRepository,
    private val animeEpisodeDetailRepository: AnimeEpisodeDetailRepository
) : ViewModel() {

    private val _homeState = MutableStateFlow(HomeState())
    val homeState: StateFlow<HomeState> = _homeState.asStateFlow()

    private val _carouselState = MutableStateFlow(CarouselState())
    val carouselState: StateFlow<CarouselState> = _carouselState.asStateFlow()

    private val _remainingTimes = MutableStateFlow<Map<String, String>>(emptyMap())
    val remainingTimes: StateFlow<Map<String, String>> = _remainingTimes.asStateFlow()

    private var timeUpdateJob: Job? = null

    init {
        scheduleWorkers()
        onAction(HomeAction.GetAnimeSchedules)
        onAction(HomeAction.GetTop10Anime)
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.GetAnimeSchedules -> getAnimeSchedules()
            HomeAction.GetTop10Anime -> getTop10Anime()
            is HomeAction.ApplyFilters -> applyFilters(action.updatedQueryState)
            HomeAction.FetchContinueWatchingEpisode -> fetchContinueWatchingEpisode()
            is HomeAction.SetMinimized -> setMinimized(action.minimize)
            is HomeAction.SetShowPopup -> setShowPopup(action.show)
            is HomeAction.SetAutoScrollEnabled -> setAutoScrollEnabled(action.enabled)
            HomeAction.UpdateCarouselLastInteractionTime -> updateCarouselLastInteractionTime()
            is HomeAction.SetCurrentCarouselPage -> setCurrentCarouselPage(action.page)
            is HomeAction.StartUpdatingBroadcastTimes -> startUpdatingBroadcastTimes(action.animeSchedules)
        }
    }

    private fun scheduleWorkers() {
        BroadcastNotificationWorker.schedule(application, false)
        UnfinishedWatchNotificationWorker.schedule(application, false)
        Log.d("AnimeHomeViewModel", "Workers scheduled from ViewModel.")
    }

    private fun getAnimeSchedules() = viewModelScope.launch {
        _homeState.update { it.copy(isRefreshing = true, animeSchedules = Resource.Loading()) }
        val result = animeHomeRepository.getAnimeSchedules(_homeState.value.queryState)
        _homeState.update { it.copy(isRefreshing = false, animeSchedules = result) }
        if (result is Resource.Success) {
            onAction(HomeAction.StartUpdatingBroadcastTimes(result.data.data))
        }
    }

    private fun getTop10Anime() = viewModelScope.launch {
        _homeState.update { it.copy(top10Anime = Resource.Loading()) }
        val result = animeHomeRepository.getTop10Anime()
        _homeState.update { it.copy(top10Anime = result) }
    }

    private fun applyFilters(updatedQueryState: AnimeSchedulesSearchQueryState) {
        _homeState.update { it.copy(queryState = updatedQueryState) }
        onAction(HomeAction.GetAnimeSchedules)
    }

    private fun fetchContinueWatchingEpisode() {
        viewModelScope.launch {
            val episode =
                animeEpisodeDetailRepository.getCachedLatestWatchedEpisodeDetailComplement()
            _homeState.update { it.copy(continueWatchingEpisode = episode) }
            _homeState.update { it.copy(isShowPopup = episode != null, isMinimized = false) }
            if (episode != null) {
                delay(10000)
                _homeState.update { it.copy(isMinimized = true) }
            }
        }
    }

    private fun setMinimized(minimize: Boolean) {
        _homeState.update { it.copy(isMinimized = minimize) }
    }

    private fun setShowPopup(show: Boolean) {
        _homeState.update { it.copy(isShowPopup = show) }
    }

    private fun setAutoScrollEnabled(enabled: Boolean) {
        _carouselState.update { it.copy(autoScrollEnabled = enabled) }
    }

    private fun updateCarouselLastInteractionTime() {
        _carouselState.update { it.copy(carouselLastInteractionTime = Date().time) }
    }

    private fun setCurrentCarouselPage(page: Int) {
        _carouselState.update { it.copy(currentCarouselPage = page) }
    }

    private fun startUpdatingBroadcastTimes(animeSchedules: List<AnimeDetail>) {
        timeUpdateJob?.cancel()
        timeUpdateJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                while (true) {
                    val nowCurrent = ZonedDateTime.now(ZoneId.systemDefault())
                    val nextSecond = nowCurrent.plusSeconds(1).withNano(0)
                    val delayMillis = ChronoUnit.MILLIS.between(nowCurrent, nextSecond)
                    delay(delayMillis)

                    val updatedTimes = animeSchedules.associate { animeDetail ->
                        val broadcast = animeDetail.broadcast
                        val remainingTime = calculateRemainingTime(broadcast)
                        animeDetail.mal_id.toString() to remainingTime
                    }
                    _remainingTimes.value = updatedTimes
                }
            } catch (e: Exception) {
                _remainingTimes.value = emptyMap()
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        timeUpdateJob?.cancel()
        super.onCleared()
    }
}