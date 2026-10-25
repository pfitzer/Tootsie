package com.pfitzer.tootsie.presentation

import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.StatusVisibility
import com.pfitzer.tootsie.domain.model.Translation
import com.pfitzer.tootsie.domain.repository.InstanceRepository
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.domain.usecase.account.ObserveActiveAccountUseCase
import com.pfitzer.tootsie.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class TranslationViewModelTest {

    private val statusRepository = mockk<StatusRepository>()
    private val instanceRepository = mockk<InstanceRepository>()
    private val observeActiveAccountUseCase = mockk<ObserveActiveAccountUseCase>()
    private val activeAccount = MutableStateFlow<Account?>(account("one"))
    private val translation = Translation(
        content = "<p>Hello</p>",
        spoilerText = "",
        detectedSourceLanguage = "de",
        provider = "DeepL.com"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { observeActiveAccountUseCase() } returns activeAccount
        coEvery { instanceRepository.isTranslationAvailable() } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = TranslationViewModel(statusRepository, instanceRepository, observeActiveAccountUseCase)

    @Test
    fun `toggling switches between translation and original without requesting again`() = runTest {
        coEvery { statusRepository.translateStatus("1", "en") } returns Result.Success(translation)
        val viewModel = viewModel()

        viewModel.toggle(status("1"), "en")
        assertEquals(TranslationState.Shown(translation), viewModel.translations.value["1"])

        viewModel.toggle(status("1"), "en")
        assertEquals(TranslationState.Hidden(translation), viewModel.translations.value["1"])

        viewModel.toggle(status("1"), "en")
        assertEquals(TranslationState.Shown(translation), viewModel.translations.value["1"])
        coVerify(exactly = 1) { statusRepository.translateStatus(any(), any()) }
    }

    @Test
    fun `a failed translation can be tried again`() = runTest {
        coEvery { statusRepository.translateStatus("1", "en") } returns Result.Error("quota") andThen Result.Success(translation)
        val viewModel = viewModel()

        viewModel.toggle(status("1"), "en")
        assertEquals(TranslationState.Failed, viewModel.translations.value["1"])

        viewModel.toggle(status("1"), "en")
        assertEquals(TranslationState.Shown(translation), viewModel.translations.value["1"])
    }

    @Test
    fun `switching accounts forgets translations and checks the new server`() = runTest {
        coEvery { statusRepository.translateStatus("1", "en") } returns Result.Success(translation)
        val viewModel = viewModel()
        assertTrue(viewModel.isServerTranslationAvailable.value)
        viewModel.toggle(status("1"), "en")

        coEvery { instanceRepository.isTranslationAvailable() } returns false
        activeAccount.value = account("two")

        assertTrue(viewModel.translations.value.isEmpty())
        assertFalse(viewModel.isServerTranslationAvailable.value)
    }

    private fun status(id: String) = Status(
        id = id,
        uri = "",
        url = null,
        account = account("author"),
        content = "<p>Hallo</p>",
        createdAt = "2026-10-25T09:00:00Z",
        visibility = StatusVisibility.PUBLIC,
        sensitive = false,
        spoilerText = "",
        reblogsCount = 0,
        favouritesCount = 0,
        repliesCount = 0,
        language = "de"
    )

    private fun account(id: String) = Account(
        id = id,
        username = id,
        acct = id,
        displayName = id,
        note = "",
        url = "",
        avatar = "",
        header = "",
        locked = false,
        followersCount = 0,
        followingCount = 0,
        statusesCount = 0,
        createdAt = "2025-01-01T00:00:00Z",
        bot = false
    )
}
