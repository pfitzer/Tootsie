package com.pfitzer.tootsie.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.domain.repository.TimelineRepository
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Home Screen.
 * Manages timeline data and status interactions.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val timelineRepository: TimelineRepository,
    private val statusRepository: StatusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Paging data flow for timeline.
     */
    val timelinePagingData: Flow<PagingData<Status>> = timelineRepository
        .getHomeTimeline()
        .cachedIn(viewModelScope)

    /**
     * Handle user actions.
     */
    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.FavoriteStatus -> favoriteStatus(action.status)
            is HomeAction.UnfavoriteStatus -> unfavoriteStatus(action.status)
            is HomeAction.BoostStatus -> boostStatus(action.status)
            is HomeAction.UnboostStatus -> unboostStatus(action.status)
            is HomeAction.RefreshTimeline -> refreshTimeline()
            is HomeAction.DismissError -> dismissError()
        }
    }

    /**
     * Favorite a status.
     */
    private fun favoriteStatus(status: Status) {
        viewModelScope.launch {
            when (statusRepository.favoriteStatus(status.id)) {
                is Result.Success -> {
                    // Success - Paging will refresh automatically
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to favorite post")
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Unfavorite a status.
     */
    private fun unfavoriteStatus(status: Status) {
        viewModelScope.launch {
            when (statusRepository.unfavoriteStatus(status.id)) {
                is Result.Success -> {
                    // Success - Paging will refresh automatically
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to unfavorite post")
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Boost (reblog) a status.
     */
    private fun boostStatus(status: Status) {
        viewModelScope.launch {
            when (statusRepository.reblogStatus(status.id)) {
                is Result.Success -> {
                    // Success - Paging will refresh automatically
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to boost post")
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Unboost (unreblog) a status.
     */
    private fun unboostStatus(status: Status) {
        viewModelScope.launch {
            when (statusRepository.unreblogStatus(status.id)) {
                is Result.Success -> {
                    // Success - Paging will refresh automatically
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to unboost post")
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Refresh the timeline.
     */
    private fun refreshTimeline() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            timelineRepository.refreshHomeTimeline()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    /**
     * Dismiss error message.
     */
    private fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for Home Screen.
 */
data class HomeUiState(
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

/**
 * User actions for Home Screen.
 */
sealed class HomeAction {
    data class FavoriteStatus(val status: Status) : HomeAction()
    data class UnfavoriteStatus(val status: Status) : HomeAction()
    data class BoostStatus(val status: Status) : HomeAction()
    data class UnboostStatus(val status: Status) : HomeAction()
    data object RefreshTimeline : HomeAction()
    data object DismissError : HomeAction()
}
