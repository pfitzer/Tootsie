package com.pfitzer.tootsie.presentation.screens.filters.edit

import androidx.lifecycle.SavedStateHandle
import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.FilterAction
import com.pfitzer.tootsie.domain.model.FilterContext
import com.pfitzer.tootsie.domain.model.FilterKeyword
import com.pfitzer.tootsie.domain.usecase.filter.AddKeywordUseCase
import com.pfitzer.tootsie.domain.usecase.filter.CreateFilterUseCase
import com.pfitzer.tootsie.domain.usecase.filter.DeleteFilterUseCase
import com.pfitzer.tootsie.domain.usecase.filter.DeleteKeywordUseCase
import com.pfitzer.tootsie.domain.usecase.filter.GetFilterUseCase
import com.pfitzer.tootsie.domain.usecase.filter.UpdateFilterUseCase
import com.pfitzer.tootsie.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FilterEditViewModelTest {

    private val getFilter = mockk<GetFilterUseCase>()
    private val createFilter = mockk<CreateFilterUseCase>()
    private val updateFilter = mockk<UpdateFilterUseCase>()
    private val deleteFilter = mockk<DeleteFilterUseCase>()
    private val addKeyword = mockk<AddKeywordUseCase>()
    private val deleteKeyword = mockk<DeleteKeywordUseCase>()

    private val existing = Filter(
        id = "f1",
        title = "Spoilers",
        context = listOf(FilterContext.HOME),
        expiresAt = null,
        filterAction = FilterAction.WARN,
        keywords = listOf(
            FilterKeyword(id = "k1", keyword = "finale", wholeWord = true),
            FilterKeyword(id = "k2", keyword = "season", wholeWord = true)
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { getFilter("f1") } returns Result.Success(existing)
        coEvery { createFilter(any(), any(), any(), any(), any()) } returns Result.Success(existing)
        coEvery { updateFilter(any(), any(), any(), any(), any()) } returns Result.Success(existing)
        coEvery { addKeyword(any(), any(), any()) } returns Result.Success(mockk())
        coEvery { deleteKeyword(any()) } returns Result.Success(Unit)
        coEvery { deleteFilter(any()) } returns Result.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(filterId: String? = null) = FilterEditViewModel(
        getFilterUseCase = getFilter,
        createFilterUseCase = createFilter,
        updateFilterUseCase = updateFilter,
        deleteFilterUseCase = deleteFilter,
        addKeywordUseCase = addKeyword,
        deleteKeywordUseCase = deleteKeyword,
        savedStateHandle = SavedStateHandle(mapOf("filterId" to filterId))
    )

    @Test
    fun `typed keyword is taken over on save`() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(FilterEditAction.UpdateTitle("Crypto"))
        viewModel.onAction(FilterEditAction.UpdateKeywordInput("bitcoin"))

        viewModel.onAction(FilterEditAction.Save)

        coVerify {
            createFilter(
                title = "Crypto",
                context = match { it.toSet() == setOf(FilterContext.HOME, FilterContext.PUBLIC) },
                filterAction = FilterAction.WARN,
                expiresIn = null,
                keywords = listOf("bitcoin" to true)
            )
        }
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun `missing title is reported, nothing is sent`() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(FilterEditAction.UpdateKeywordInput("bitcoin"))

        viewModel.onAction(FilterEditAction.Save)

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isDone)
        coVerify(exactly = 0) { createFilter(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `editing sends only the changes`() = runTest {
        val viewModel = viewModel("f1")
        assertEquals("Spoilers", viewModel.uiState.value.title)

        viewModel.onAction(FilterEditAction.SelectAction(FilterAction.HIDE))
        viewModel.onAction(FilterEditAction.RemoveKeyword(1))
        viewModel.onAction(FilterEditAction.ToggleWholeWord(0))
        viewModel.onAction(FilterEditAction.UpdateKeywordInput("ending"))
        viewModel.onAction(FilterEditAction.AddKeyword)
        viewModel.onAction(FilterEditAction.Save)

        // Only the action changed; "never" on a filter without expiry is no change
        coVerify { updateFilter(id = "f1", title = null, context = null, filterAction = FilterAction.HIDE, expiresIn = null) }
        // "season" removed; "finale" changed to partial match = delete + add
        coVerify { deleteKeyword("k2") }
        coVerify { deleteKeyword("k1") }
        coVerify { addKeyword("f1", "finale", false) }
        coVerify { addKeyword("f1", "ending", true) }
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun `unchanged filter sends nothing`() = runTest {
        val viewModel = viewModel("f1")

        viewModel.onAction(FilterEditAction.Save)

        coVerify(exactly = 0) { updateFilter(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { addKeyword(any(), any(), any()) }
        coVerify(exactly = 0) { deleteKeyword(any()) }
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun `expiring filter can be made permanent`() = runTest {
        coEvery { getFilter("f1") } returns Result.Success(existing.copy(expiresAt = "2026-11-01T10:00:00Z"))
        val viewModel = viewModel("f1")
        assertEquals(FilterExpiry.KEEP, viewModel.uiState.value.expiry)

        viewModel.onAction(FilterEditAction.SelectExpiry(FilterExpiry.NEVER))
        viewModel.onAction(FilterEditAction.Save)

        coVerify { updateFilter(id = "f1", title = null, context = null, filterAction = null, expiresIn = 0) }
    }
}
