package com.pfitzer.tootsie.data.mapper

import com.pfitzer.tootsie.data.local.entities.FilterEntity
import com.pfitzer.tootsie.data.remote.dto.FilterDto
import com.pfitzer.tootsie.data.remote.dto.FilterKeywordDto
import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.FilterAction
import com.pfitzer.tootsie.domain.model.FilterContext
import com.pfitzer.tootsie.domain.model.FilterKeyword
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * JSON serializer for filter mapping.
 */
private val json = Json { ignoreUnknownKeys = true }

/**
 * Converts FilterDto to Filter domain model.
 */
fun FilterDto.toDomain(): Filter {
    return Filter(
        id = id,
        title = title,
        context = context.map { FilterContext.fromString(it) },
        expiresAt = expiresAt,
        filterAction = FilterAction.fromString(filterAction),
        keywords = keywords.map { it.toDomain() }
    )
}

/**
 * Converts FilterKeywordDto to FilterKeyword domain model.
 */
fun FilterKeywordDto.toDomain(): FilterKeyword {
    return FilterKeyword(
        id = id,
        keyword = keyword,
        wholeWord = wholeWord
    )
}

/**
 * Converts Filter domain model to FilterEntity for Room storage.
 */
fun Filter.toEntity(ownerAccountId: String): FilterEntity {
    return FilterEntity(
        id = id,
        title = title,
        contextJson = json.encodeToString(context.map { it.toApiString() }),
        expiresAt = expiresAt,
        filterAction = filterAction.toApiString(),
        keywordsJson = json.encodeToString(keywords.map {
            mapOf(
                "id" to it.id,
                "keyword" to it.keyword,
                "wholeWord" to it.wholeWord.toString()
            )
        }),
        ownerAccountId = ownerAccountId
    )
}

/**
 * Converts FilterEntity to Filter domain model.
 */
fun FilterEntity.toDomain(): Filter {
    val contextList: List<String> = try {
        json.decodeFromString(contextJson)
    } catch (e: Exception) {
        emptyList()
    }

    val keywordsList: List<Map<String, String>> = try {
        json.decodeFromString(keywordsJson)
    } catch (e: Exception) {
        emptyList()
    }

    return Filter(
        id = id,
        title = title,
        context = contextList.map { FilterContext.fromString(it) },
        expiresAt = expiresAt,
        filterAction = FilterAction.fromString(filterAction),
        keywords = keywordsList.map { map ->
            FilterKeyword(
                id = map["id"] ?: "",
                keyword = map["keyword"] ?: "",
                wholeWord = map["wholeWord"]?.toBooleanStrictOrNull() ?: false
            )
        }
    )
}
