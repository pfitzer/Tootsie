package com.pfitzer.tootsie.presentation.util

import android.text.Html

/**
 * Converts HTML content to plain text.
 * Mastodon posts contain HTML formatting that needs to be rendered.
 *
 * @param html HTML string
 * @return Plain text with HTML stripped
 */
fun htmlToPlainText(html: String): String {
    return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
}
