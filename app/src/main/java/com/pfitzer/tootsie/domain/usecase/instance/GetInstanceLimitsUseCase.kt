package com.pfitzer.tootsie.domain.usecase.instance

import com.pfitzer.tootsie.domain.model.InstanceLimits
import com.pfitzer.tootsie.domain.repository.InstanceRepository
import javax.inject.Inject

/**
 * Use case to get the posting limits of the active account's instance.
 */
class GetInstanceLimitsUseCase @Inject constructor(
    private val instanceRepository: InstanceRepository
) {
    suspend operator fun invoke(): InstanceLimits = instanceRepository.getLimits()
}
