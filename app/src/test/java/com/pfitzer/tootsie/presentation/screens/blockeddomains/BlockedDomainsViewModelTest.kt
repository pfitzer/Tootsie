package com.pfitzer.tootsie.presentation.screens.blockeddomains

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import com.pfitzer.tootsie.domain.repository.DomainBlockRepository
import com.pfitzer.tootsie.domain.usecase.domainblock.BlockDomainUseCase
import com.pfitzer.tootsie.domain.usecase.domainblock.UnblockDomainUseCase
import com.pfitzer.tootsie.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class BlockedDomainsViewModelTest {

    private val domainBlockRepository = mockk<DomainBlockRepository>()
    private val blockDomainUseCase = mockk<BlockDomainUseCase>()
    private val unblockDomainUseCase = mockk<UnblockDomainUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { domainBlockRepository.getBlockedDomains() } returns
            flowOf(
                PagingData.from(
                    listOf("a.social", "b.social", "c.social"),
                    // All loaded, otherwise asSnapshot() waits for further pages forever
                    sourceLoadStates = LoadStates(
                        refresh = LoadState.NotLoading(endOfPaginationReached = true),
                        prepend = LoadState.NotLoading(endOfPaginationReached = true),
                        append = LoadState.NotLoading(endOfPaginationReached = true)
                    )
                )
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = BlockedDomainsViewModel(domainBlockRepository, blockDomainUseCase, unblockDomainUseCase)

    @Test
    fun `unblocked domains are removed from the list`() = runTest {
        coEvery { unblockDomainUseCase(any()) } returns Result.Success(Unit)
        val viewModel = viewModel()

        viewModel.onAction(BlockedDomainsAction.UnblockDomain("a.social"))
        viewModel.onAction(BlockedDomainsAction.UnblockDomain("c.social"))

        assertEquals(listOf("b.social"), viewModel.blockedDomainsPagingData.asSnapshot())
        assertTrue(viewModel.uiState.value.processingDomains.isEmpty())
    }

    @Test
    fun `failed unblock keeps the domain and shows an error`() = runTest {
        coEvery { unblockDomainUseCase(any()) } returns Result.Error("boom")
        val viewModel = viewModel()

        viewModel.onAction(BlockedDomainsAction.UnblockDomain("b.social"))

        assertEquals(listOf("a.social", "b.social", "c.social"), viewModel.blockedDomainsPagingData.asSnapshot())
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `blocking normalizes the input and reloads the list`() = runTest {
        coEvery { blockDomainUseCase(any()) } returns Result.Success(Unit)
        val viewModel = viewModel()
        viewModel.onAction(BlockedDomainsAction.ShowBlockDialog)

        viewModel.onAction(BlockedDomainsAction.BlockDomain("https://New.Social/about"))

        coVerify { blockDomainUseCase("new.social") }
        assertFalse(viewModel.uiState.value.showBlockDialog)
        assertTrue(viewModel.uiState.value.reload)
    }

    @Test
    fun `blocking a domain unblocked before shows it again`() = runTest {
        coEvery { unblockDomainUseCase(any()) } returns Result.Success(Unit)
        coEvery { blockDomainUseCase(any()) } returns Result.Success(Unit)
        val viewModel = viewModel()

        viewModel.onAction(BlockedDomainsAction.UnblockDomain("a.social"))
        viewModel.onAction(BlockedDomainsAction.BlockDomain("a.social"))

        assertEquals(listOf("a.social", "b.social", "c.social"), viewModel.blockedDomainsPagingData.asSnapshot())
    }

    @Test
    fun `invalid input is not sent`() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(BlockedDomainsAction.BlockDomain("not a domain"))

        coVerify(exactly = 0) { blockDomainUseCase(any()) }
    }
}
