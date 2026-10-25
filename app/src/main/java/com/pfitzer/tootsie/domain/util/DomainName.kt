package com.pfitzer.tootsie.domain.util

import java.util.Locale

/**
 * Turns user input into a domain that can be blocked.
 */
object DomainName {

    // Labels of letters (also non-Latin), digits and hyphens, at least one dot
    private val VALID = Regex("""^[\p{L}\p{N}](?:[\p{L}\p{N}-]*[\p{L}\p{N}])?(?:\.[\p{L}\p{N}](?:[\p{L}\p{N}-]*[\p{L}\p{N}])?)+$""")

    /**
     * The domain from a domain, URL or handle ("example.social", "https://example.social/about",
     * "@user@example.social"), lowercase; null if it is not a valid domain.
     */
    fun normalize(input: String): String? {
        val domain = input.trim()
            .substringAfter("://")
            .substringAfterLast('@')
            .substringBefore('/')
            .substringBefore(':')
            .trimEnd('.')
            .lowercase(Locale.ROOT)
        return domain.takeIf { VALID.matches(it) }
    }
}
