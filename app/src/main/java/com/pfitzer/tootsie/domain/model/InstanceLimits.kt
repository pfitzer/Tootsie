package com.pfitzer.tootsie.domain.model

/**
 * Posting limits of a Mastodon instance (from the instance configuration).
 * The defaults are Mastodon's defaults, used when the instance doesn't report them.
 */
data class InstanceLimits(
    val maxCharacters: Int = DEFAULT_MAX_CHARACTERS,
    val maxMediaAttachments: Int = DEFAULT_MAX_MEDIA_ATTACHMENTS,
    /** Every URL counts as this many characters, regardless of its length. */
    val charactersReservedPerUrl: Int = DEFAULT_CHARACTERS_RESERVED_PER_URL
) {
    companion object {
        const val DEFAULT_MAX_CHARACTERS = 500
        const val DEFAULT_MAX_MEDIA_ATTACHMENTS = 4
        const val DEFAULT_CHARACTERS_RESERVED_PER_URL = 23
    }
}
