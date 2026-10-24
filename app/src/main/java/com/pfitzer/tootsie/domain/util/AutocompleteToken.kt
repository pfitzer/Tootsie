package com.pfitzer.tootsie.domain.util

/**
 * The word being typed at the cursor, if it can be autocompleted: a mention (`@user`),
 * a hashtag (`#tag`) or a custom emoji (`:shortcode`).
 *
 * @param start Index of the trigger character (`@`, `#` or `:`) in the text
 * @param end Index after the last character of the word (the cursor position)
 * @param query The typed text after the trigger character
 */
data class AutocompleteToken(
    val type: Type,
    val start: Int,
    val end: Int,
    val query: String
) {
    enum class Type { MENTION, HASHTAG, EMOJI }

    companion object {
        /** Minimum query length before suggestions are looked up. */
        private const val MIN_QUERY_LENGTH = 2

        // The word before the cursor: trigger + query, at the start or after a space/bracket
        private val TOKEN = Regex("""(?:^|[\s(\[{])([@#:])([\p{L}\p{N}_.@-]*)$""")

        /**
         * Finds the token that ends at [cursor], or null if nothing should be completed.
         */
        fun find(text: String, cursor: Int): AutocompleteToken? {
            if (cursor < 0 || cursor > text.length) return null
            // Only complete at the end of a word, not in the middle of one
            if (cursor < text.length && !text[cursor].isWhitespace()) return null

            val match = TOKEN.find(text.substring(0, cursor)) ?: return null
            val trigger = match.groupValues[1]
            val query = match.groupValues[2]
            if (query.length < MIN_QUERY_LENGTH) return null

            val type = when (trigger) {
                "@" -> Type.MENTION
                "#" -> Type.HASHTAG
                else -> Type.EMOJI
            }
            // Hashtags and emoji shortcodes don't contain these characters
            if (type != Type.MENTION && query.any { it == '@' || it == '.' || it == '-' }) return null

            val start = match.groups[1]!!.range.first
            return AutocompleteToken(type = type, start = start, end = cursor, query = query)
        }
    }
}
