package com.pfitzer.tootsie.data.mapper

import com.pfitzer.tootsie.data.remote.dto.ContextDto
import com.pfitzer.tootsie.domain.model.Context

/**
 * Converts ContextDto (API response) to Context domain model.
 */
fun ContextDto.toDomain(): Context {
    return Context(
        ancestors = ancestors.map { it.toDomain() },
        descendants = descendants.map { it.toDomain() }
    )
}
