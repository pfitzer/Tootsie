package com.pfitzer.tootsie.presentation.screens.editprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.usecase.account.GetAccountUseCase
import com.pfitzer.tootsie.domain.usecase.account.UpdateProfileUseCase
import com.pfitzer.tootsie.domain.repository.AccountManagementRepository
import com.pfitzer.tootsie.presentation.util.htmlToPlainText
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Edit Profile Screen.
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val accountManagementRepository: AccountManagementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadCurrentProfile()
    }

    private fun loadCurrentProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val activeAccount = accountManagementRepository.getActiveAccount()
            if (activeAccount != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        displayName = activeAccount.displayName,
                        note = htmlToPlainText(activeAccount.note)
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load active account"
                    )
                }
            }
        }
    }

    fun onAction(action: EditProfileAction) {
        when (action) {
            is EditProfileAction.DisplayNameChanged -> {
                _uiState.update { it.copy(displayName = action.displayName) }
            }
            is EditProfileAction.NoteChanged -> {
                _uiState.update { it.copy(note = action.note) }
            }
            EditProfileAction.SaveProfile -> saveProfile()
            EditProfileAction.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun saveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = updateProfileUseCase(
                displayName = _uiState.value.displayName,
                note = _uiState.value.note
            )
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, isSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = result.message ?: "Failed to update profile"
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

data class EditProfileUiState(
    val displayName: String = "",
    val note: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

sealed class EditProfileAction {
    data class DisplayNameChanged(val displayName: String) : EditProfileAction()
    data class NoteChanged(val note: String) : EditProfileAction()
    data object SaveProfile : EditProfileAction()
    data object DismissError : EditProfileAction()
}
