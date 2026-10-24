package com.pfitzer.tootsie.presentation.screens.filters.edit

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.FilterAction
import com.pfitzer.tootsie.domain.model.FilterContext
import com.pfitzer.tootsie.domain.usecase.filter.AddKeywordUseCase
import com.pfitzer.tootsie.domain.usecase.filter.CreateFilterUseCase
import com.pfitzer.tootsie.domain.usecase.filter.DeleteFilterUseCase
import com.pfitzer.tootsie.domain.usecase.filter.DeleteKeywordUseCase
import com.pfitzer.tootsie.domain.usecase.filter.GetFilterUseCase
import com.pfitzer.tootsie.domain.usecase.filter.UpdateFilterUseCase
import com.pfitzer.tootsie.presentation.navigation.TootsieDestinations
import com.pfitzer.tootsie.presentation.util.UiText
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for creating and editing a keyword filter.
 *
 * Changes are kept locally and only sent to the server on save. For an existing filter,
 * only the changed fields are updated, and keywords are added/removed as needed.
 */
@HiltViewModel
class FilterEditViewModel @Inject constructor(
    private val getFilterUseCase: GetFilterUseCase,
    private val createFilterUseCase: CreateFilterUseCase,
    private val updateFilterUseCase: UpdateFilterUseCase,
    private val deleteFilterUseCase: DeleteFilterUseCase,
    private val addKeywordUseCase: AddKeywordUseCase,
    private val deleteKeywordUseCase: DeleteKeywordUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val filterId: String? = savedStateHandle[TootsieDestinations.FilterEdit.ARG_FILTER_ID]

    /** The filter as loaded from the server, to find out what changed. */
    private var original: Filter? = null

    private val _uiState = MutableStateFlow(FilterEditUiState(isNew = filterId == null, isLoading = filterId != null))
    val uiState: StateFlow<FilterEditUiState> = _uiState.asStateFlow()

    init {
        filterId?.let { loadFilter(it) }
    }

    fun onAction(action: FilterEditAction) {
        when (action) {
            is FilterEditAction.UpdateTitle -> _uiState.update { it.copy(title = action.title) }
            is FilterEditAction.ToggleContext -> _uiState.update {
                it.copy(contexts = if (action.context in it.contexts) it.contexts - action.context else it.contexts + action.context)
            }
            is FilterEditAction.SelectAction -> _uiState.update { it.copy(filterAction = action.filterAction) }
            is FilterEditAction.SelectExpiry -> _uiState.update { it.copy(expiry = action.expiry) }
            is FilterEditAction.UpdateKeywordInput -> _uiState.update { it.copy(keywordInput = action.text) }
            is FilterEditAction.AddKeyword -> addKeywordFromInput()
            is FilterEditAction.ToggleWholeWord -> _uiState.update { state ->
                state.copy(keywords = state.keywords.mapIndexed { i, k -> if (i == action.index) k.copy(wholeWord = !k.wholeWord) else k })
            }
            is FilterEditAction.RemoveKeyword -> _uiState.update { state ->
                state.copy(keywords = state.keywords.filterIndexed { i, _ -> i != action.index })
            }
            is FilterEditAction.Save -> save()
            is FilterEditAction.Delete -> delete()
            is FilterEditAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadFilter(id: String) {
        viewModelScope.launch {
            when (val result = getFilterUseCase(id)) {
                is Result.Success -> {
                    val filter = result.data
                    original = filter
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            title = filter.title,
                            contexts = filter.context.toSet(),
                            filterAction = filter.filterAction,
                            expiry = if (filter.expiresAt != null) FilterExpiry.KEEP else FilterExpiry.NEVER,
                            currentExpiresAt = filter.expiresAt,
                            keywords = filter.keywords.map { k -> KeywordDraft(k.id, k.keyword, k.wholeWord) }
                        )
                    }
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = UiText.of(R.string.error_load_filter))
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Adds the typed keyword (whole word by default, like Mastodon). Duplicates are ignored.
     */
    private fun addKeywordFromInput() {
        _uiState.update { state ->
            val keyword = state.keywordInput.trim()
            if (keyword.isEmpty()) return@update state
            val exists = state.keywords.any { it.keyword.equals(keyword, ignoreCase = true) }
            state.copy(
                keywords = if (exists) state.keywords else state.keywords + KeywordDraft(id = null, keyword = keyword, wholeWord = true),
                keywordInput = ""
            )
        }
    }

    private fun save() {
        // A keyword that was typed but not added yet counts as well
        addKeywordFromInput()
        val state = _uiState.value
        val error = state.validationError
        if (error != null) {
            _uiState.update { it.copy(errorMessage = UiText.of(error)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val success = original?.let { saveChanges(it, state) } ?: create(state)
            _uiState.update {
                if (success) it.copy(isSaving = false, isDone = true)
                else it.copy(isSaving = false, errorMessage = UiText.of(R.string.error_update_filter))
            }
        }
    }

    private suspend fun create(state: FilterEditUiState): Boolean {
        val result = createFilterUseCase(
            title = state.title.trim(),
            context = state.contexts.toList(),
            filterAction = state.filterAction,
            expiresIn = state.expiry.seconds?.takeIf { it > 0 },
            keywords = state.keywords.map { it.keyword to it.wholeWord }
        )
        return result is Result.Success
    }

    private suspend fun saveChanges(original: Filter, state: FilterEditUiState): Boolean {
        val title = state.title.trim().takeIf { it != original.title }
        val contexts = state.contexts.toList().takeIf { it.toSet() != original.context.toSet() }
        val action = state.filterAction.takeIf { it != original.filterAction }
        // "Never" on a filter that doesn't expire is no change
        val expiresIn = state.expiry.seconds?.takeUnless { it == 0 && original.expiresAt == null }
        if (title != null || contexts != null || action != null || expiresIn != null) {
            val result = updateFilterUseCase(
                id = original.id,
                title = title,
                context = contexts,
                filterAction = action,
                expiresIn = expiresIn
            )
            if (result is Result.Error) return false
        }

        // Keywords: removed or changed ones are deleted, new or changed ones added
        val kept = state.keywords.filter { it.id != null }.associateBy { it.id }
        for (keyword in original.keywords) {
            val draft = kept[keyword.id]
            if (draft == null || draft.wholeWord != keyword.wholeWord) {
                if (deleteKeywordUseCase(keyword.id) is Result.Error) return false
            }
        }
        for (draft in state.keywords) {
            val unchanged = draft.id != null && original.keywords.any { it.id == draft.id && it.wholeWord == draft.wholeWord }
            if (!unchanged && addKeywordUseCase(original.id, draft.keyword, draft.wholeWord) is Result.Error) return false
        }
        return true
    }

    private fun delete() {
        val id = original?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (deleteFilterUseCase(id)) {
                is Result.Success -> _uiState.update { it.copy(isSaving = false, isDone = true) }
                else -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = UiText.of(R.string.error_delete_filter))
                }
            }
        }
    }
}

/**
 * A keyword in the editor; [id] is null for keywords that aren't saved yet.
 */
data class KeywordDraft(val id: String?, val keyword: String, val wholeWord: Boolean)

/**
 * Expiry choices. [seconds] is what is sent to the API: null keeps the current expiry,
 * 0 removes it.
 */
enum class FilterExpiry(@StringRes val labelRes: Int, val seconds: Int?) {
    KEEP(R.string.filter_expiration_keep, null),
    NEVER(R.string.filter_expiration_never, 0),
    MINUTES_30(R.string.filter_expiration_30min, 30 * 60),
    HOUR_1(R.string.filter_expiration_1hour, 60 * 60),
    HOURS_6(R.string.filter_expiration_6hours, 6 * 60 * 60),
    HOURS_12(R.string.filter_expiration_12hours, 12 * 60 * 60),
    DAY_1(R.string.filter_expiration_1day, 24 * 60 * 60),
    WEEK_1(R.string.filter_expiration_1week, 7 * 24 * 60 * 60)
}

data class FilterEditUiState(
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    /** Saved or deleted: the screen closes. */
    val isDone: Boolean = false,
    val title: String = "",
    val contexts: Set<FilterContext> = setOf(FilterContext.HOME, FilterContext.PUBLIC),
    val filterAction: FilterAction = FilterAction.WARN,
    val expiry: FilterExpiry = FilterExpiry.NEVER,
    /** Expiry of the existing filter (ISO 8601), shown for "keep". */
    val currentExpiresAt: String? = null,
    val keywords: List<KeywordDraft> = emptyList(),
    val keywordInput: String = "",
    val errorMessage: UiText? = null
) {
    /** Expiry choices; "keep" only applies to an existing filter that expires. */
    val expiryOptions: List<FilterExpiry>
        get() = if (!isNew && currentExpiresAt != null) FilterExpiry.entries else FilterExpiry.entries - FilterExpiry.KEEP

    @get:StringRes
    val validationError: Int?
        get() = when {
            title.isBlank() -> R.string.filter_error_title_required
            contexts.isEmpty() -> R.string.filter_error_context_required
            keywords.isEmpty() && keywordInput.isBlank() -> R.string.filter_error_keyword_required
            else -> null
        }
}

sealed class FilterEditAction {
    data class UpdateTitle(val title: String) : FilterEditAction()
    data class ToggleContext(val context: FilterContext) : FilterEditAction()
    data class SelectAction(val filterAction: FilterAction) : FilterEditAction()
    data class SelectExpiry(val expiry: FilterExpiry) : FilterEditAction()
    data class UpdateKeywordInput(val text: String) : FilterEditAction()
    data object AddKeyword : FilterEditAction()
    data class ToggleWholeWord(val index: Int) : FilterEditAction()
    data class RemoveKeyword(val index: Int) : FilterEditAction()
    data object Save : FilterEditAction()
    data object Delete : FilterEditAction()
    data object DismissError : FilterEditAction()
}
