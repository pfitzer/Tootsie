package com.pfitzer.tootsie.domain.model

/**
 * A quoted status inside a status (Mastodon >= 4.4/4.5).
 *
 * @property quotedStatus The quoted status; only present if [state] allows showing it.
 * @property quotedStatusId ID of the quoted status when only a shallow reference is sent
 *   (nested quotes).
 */
data class Quote(
    val state: QuoteState,
    val quotedStatus: Status? = null,
    val quotedStatusId: String? = null
)

/**
 * State of a quote. Unknown values from the server map to [UNAUTHORIZED].
 */
enum class QuoteState {
    PENDING,
    ACCEPTED,
    REJECTED,
    REVOKED,
    DELETED,
    UNAUTHORIZED,
    BLOCKED_ACCOUNT,
    BLOCKED_DOMAIN,
    MUTED_ACCOUNT;

    companion object {
        fun fromApi(value: String): QuoteState =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNAUTHORIZED
    }
}

/**
 * How the quote policy of a status applies to the current user (`quote_approval.current_user`).
 */
enum class QuotePermission {
    /** Quote is accepted automatically */
    AUTOMATIC,

    /** Quote has to be approved by the author */
    MANUAL,

    /** Not allowed; `unknown` is treated the same as recommended by the API docs */
    DENIED;

    val canQuote: Boolean get() = this != DENIED

    companion object {
        fun fromApi(value: String?): QuotePermission = when (value?.lowercase()) {
            "automatic" -> AUTOMATIC
            "manual" -> MANUAL
            else -> DENIED
        }
    }
}
