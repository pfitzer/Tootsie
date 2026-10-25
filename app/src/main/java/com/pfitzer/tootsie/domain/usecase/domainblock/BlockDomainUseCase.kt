package com.pfitzer.tootsie.domain.usecase.domainblock

import com.pfitzer.tootsie.domain.repository.DomainBlockRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for blocking a domain (instance).
 */
class BlockDomainUseCase @Inject constructor(
    private val domainBlockRepository: DomainBlockRepository
) {
    /**
     * Block a domain.
     *
     * @param domain Domain to block, e.g. "example.social"
     */
    suspend operator fun invoke(domain: String): Result<Unit> {
        return domainBlockRepository.blockDomain(domain)
    }
}
