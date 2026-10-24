package com.pfitzer.tootsie.presentation.screens.compose

import androidx.lifecycle.SavedStateHandle
import com.pfitzer.tootsie.domain.model.InstanceLimits
import com.pfitzer.tootsie.domain.repository.DraftRepository
import com.pfitzer.tootsie.domain.usecase.emoji.GetCustomEmojisUseCase
import com.pfitzer.tootsie.domain.usecase.instance.GetInstanceLimitsUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComposeViewModelTest {

    private val draftRepository = mockk<DraftRepository>()
    private val getCustomEmojisUseCase = mockk<GetCustomEmojisUseCase>()
    private val getInstanceLimitsUseCase = mockk<GetInstanceLimitsUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { getCustomEmojisUseCase() } returns Result.Success(emptyList())
        coEvery { getInstanceLimitsUseCase() } returns InstanceLimits(maxCharacters = 1000)
        coEvery { draftRepository.saveDraft(any()) } returns Result.Success(1L)
        coEvery { draftRepository.deleteDraft(any()) } returns Result.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ComposeViewModel(
        postStatusUseCase = mockk(),
        uploadMediaUseCase = mockk(),
        statusRepository = mockk(),
        draftRepository = draftRepository,
        getCustomEmojisUseCase = getCustomEmojisUseCase,
        getInstanceLimitsUseCase = getInstanceLimitsUseCase,
        savedStateHandle = SavedStateHandle()
    )

    @Test
    fun `leaving right after typing saves the draft before exiting`() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(ComposeAction.UpdateContent("Not saved by the 5 s auto-save yet"))
        viewModel.onAction(ComposeAction.Exit)

        coVerify(exactly = 1) { draftRepository.saveDraft(match { it.content == "Not saved by the 5 s auto-save yet" }) }
        assertTrue(viewModel.uiState.value.isExited)
    }

    @Test
    fun `leaving without content saves nothing`() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(ComposeAction.Exit)

        coVerify(exactly = 0) { draftRepository.saveDraft(any()) }
        assertTrue(viewModel.uiState.value.isExited)
    }

    @Test
    fun `character limit comes from the instance`() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(ComposeAction.UpdateContent("x".repeat(800)))

        val state = viewModel.uiState.value
        assertEquals(1000, state.maxCharacters)
        assertFalse(state.isOverCharacterLimit)
        assertTrue(state.canPost)

        viewModel.onAction(ComposeAction.UpdateContent("x".repeat(1001)))
        assertTrue(viewModel.uiState.value.isOverCharacterLimit)
        assertFalse(viewModel.uiState.value.canPost)
    }

    @Test
    fun `long links don't block posting`() = runTest {
        coEvery { getInstanceLimitsUseCase() } returns InstanceLimits()
        val viewModel = viewModel()
        val url = "https://example.social/" + "a".repeat(600)

        viewModel.onAction(ComposeAction.UpdateContent("Read this: $url"))

        assertEquals("Read this: ".length + 23, viewModel.uiState.value.characterCount)
        assertTrue(viewModel.uiState.value.canPost)
    }
}
