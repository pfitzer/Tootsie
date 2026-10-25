package com.pfitzer.tootsie.domain.usecase.account

import com.pfitzer.tootsie.domain.model.AccountPushConfig
import com.pfitzer.tootsie.domain.repository.AccountManagementRepository
import com.pfitzer.tootsie.domain.repository.PushRepository
import com.pfitzer.tootsie.domain.usecase.push.DisablePushUseCase
import com.pfitzer.tootsie.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoveAccountUseCaseTest {

    private val accountManagementRepository = mockk<AccountManagementRepository>()
    private val pushRepository = mockk<PushRepository>()
    private val disablePushUseCase = mockk<DisablePushUseCase>()
    private val useCase = RemoveAccountUseCase(accountManagementRepository, pushRepository, disablePushUseCase)

    @Test
    fun `push is ended on the server before the account is removed`() = runTest {
        coEvery { pushRepository.getPushConfig("a") } returns Result.Success(pushConfig(enabled = true))
        coEvery { disablePushUseCase("a") } returns Result.Success(Unit)
        coEvery { accountManagementRepository.removeAccount("a") } returns true

        assertTrue(useCase("a"))

        coVerifyOrder {
            disablePushUseCase("a")
            accountManagementRepository.removeAccount("a")
        }
    }

    @Test
    fun `the account is removed even if ending push fails`() = runTest {
        coEvery { pushRepository.getPushConfig("a") } returns Result.Success(pushConfig(enabled = true))
        coEvery { disablePushUseCase("a") } returns Result.Error("offline")
        coEvery { accountManagementRepository.removeAccount("a") } returns false

        useCase("a")

        coVerify { accountManagementRepository.removeAccount("a") }
    }

    @Test
    fun `without push nothing is sent to the server`() = runTest {
        coEvery { pushRepository.getPushConfig("a") } returns Result.Success(pushConfig(enabled = false))
        coEvery { accountManagementRepository.removeAccount("a") } returns false

        useCase("a")

        coVerify(exactly = 0) { disablePushUseCase(any()) }
        coVerify { accountManagementRepository.removeAccount("a") }
    }

    private fun pushConfig(enabled: Boolean) = mockk<AccountPushConfig> {
        every { this@mockk.enabled } returns enabled
    }
}
