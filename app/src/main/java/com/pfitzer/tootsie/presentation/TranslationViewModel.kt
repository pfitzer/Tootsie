package com.pfitzer.tootsie.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.Translation
import com.pfitzer.tootsie.domain.repository.InstanceRepository
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.domain.usecase.account.ObserveActiveAccountUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Translations of statuses by the server, for status cards on every screen (used by the
 * nav host). Translations are kept for the session, so switching back and forth between
 * translation and original doesn't request them again.
 */
@HiltViewModel
class TranslationViewModel @Inject constructor(
    private val statusRepository: StatusRepository,
    private val instanceRepository: InstanceRepository,
    observeActiveAccountUseCase: ObserveActiveAccountUseCase
) : ViewModel() {

    private val _translations = MutableStateFlow<Map<String, TranslationState>>(emptyMap())

    /** Translation state by status ID; statuses without an entry show the original. */
    val translations: StateFlow<Map<String, TranslationState>> = _translations.asStateFlow()

    private val _isServerTranslationAvailable = MutableStateFlow(false)

    /** The active account's server can translate; otherwise a translator app is offered. */
    val isServerTranslationAvailable: StateFlow<Boolean> = _isServerTranslationAvailable.asStateFlow()

    init {
        // Translations and availability belong to the active account's server
        viewModelScope.launch {
            observeActiveAccountUseCase()
                .map { it?.id }
                .distinctUntilChanged()
                .collect { accountId ->
                    _translations.value = emptyMap()
                    _isServerTranslationAvailable.value =
                        accountId != null && instanceRepository.isTranslationAvailable()
                }
        }
    }

    /**
     * Show the translation of a status into [language], or the original again.
     */
    fun toggle(status: Status, language: String) {
        when (val state = _translations.value[status.id]) {
            is TranslationState.Shown -> set(status.id, TranslationState.Hidden(state.translation))
            is TranslationState.Hidden -> set(status.id, TranslationState.Shown(state.translation))
            TranslationState.Loading -> Unit
            TranslationState.Failed, null -> translate(status.id, language)
        }
    }

    private fun translate(statusId: String, language: String) {
        set(statusId, TranslationState.Loading)
        viewModelScope.launch {
            val state = when (val result = statusRepository.translateStatus(statusId, language)) {
                is Result.Success -> TranslationState.Shown(result.data)
                else -> TranslationState.Failed
            }
            set(statusId, state)
        }
    }

    private fun set(statusId: String, state: TranslationState) {
        _translations.update { it + (statusId to state) }
    }
}

/**
 * Translation state of one status.
 */
sealed class TranslationState {
    data object Loading : TranslationState()
    data class Shown(val translation: Translation) : TranslationState()

    /** Translated before, the original is shown again. */
    data class Hidden(val translation: Translation) : TranslationState()
    data object Failed : TranslationState()
}
