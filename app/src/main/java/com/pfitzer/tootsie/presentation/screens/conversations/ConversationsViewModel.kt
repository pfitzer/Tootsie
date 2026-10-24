package com.pfitzer.tootsie.presentation.screens.conversations

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.Conversation
import com.pfitzer.tootsie.domain.usecase.conversation.DeleteConversationUseCase
import com.pfitzer.tootsie.domain.usecase.conversation.GetConversationsUseCase
import com.pfitzer.tootsie.domain.usecase.conversation.MarkConversationAsReadUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Conversations Screen.
 */
@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val getConversationsUseCase: GetConversationsUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
    private val markConversationAsReadUseCase: MarkConversationAsReadUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationsUiState())
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    fun onAction(action: ConversationsAction) {
        when (action) {
            ConversationsAction.Refresh -> loadConversations()
            is ConversationsAction.DeleteConversation -> deleteConversation(action.id)
            is ConversationsAction.MarkAsRead -> markAsRead(action.id)
            ConversationsAction.DismissError -> dismissError()
        }
    }

    private fun loadConversations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = getConversationsUseCase()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            conversations = result.data,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.of(R.string.error_load_conversations)
                        )
                    }
                }
                else -> {}
            }
        }
    }

    private fun deleteConversation(id: String) {
        viewModelScope.launch {
            when (val result = deleteConversationUseCase(id)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(conversations = state.conversations.filter { it.id != id })
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_delete_conversation)) }
                }
                else -> {}
            }
        }
    }

    private fun markAsRead(id: String) {
        viewModelScope.launch {
            when (val result = markConversationAsReadUseCase(id)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(conversations = state.conversations.map {
                            if (it.id == id) result.data else it
                        })
                    }
                }
                else -> {}
            }
        }
    }

    private fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class ConversationsUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null
)

sealed class ConversationsAction {
    data object Refresh : ConversationsAction()
    data class DeleteConversation(val id: String) : ConversationsAction()
    data class MarkAsRead(val id: String) : ConversationsAction()
    data object DismissError : ConversationsAction()
}
