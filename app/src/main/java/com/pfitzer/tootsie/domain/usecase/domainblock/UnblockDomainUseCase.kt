package com.pfitzer.tootsie.domain.usecase.domainblock

import com.pfitzer.tootsie.domain.repository.DomainBlockRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for unblocking a domain (instance).
 */
class UnblockDomainUseCase @Inject constructor(
    private val domainBlockRepository: DomainBlockRepository
) {
    /**
     * Unblock a domain.
     *
     * @param domain Domain to unblock
     */
    suspend operator fun invoke(domain: String): Result<Unit> {
        return domainBlockRepository.unblockDomain(domain)
    }
}
