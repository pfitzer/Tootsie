package com.pfitzer.tootsie.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Utilities for opening URLs in external browser.
 */

/**
 * Open a URL in the default external browser.
 * Validates URL scheme for security (only allows http/https).
 *
 * @param url The URL to open
 */
fun Context.openInBrowser(url: String) {
    try {
        val uri = Uri.parse(url)

        // Security: Only allow http and https schemes
        if (uri.scheme != "http" && uri.scheme != "https") {
            Toast.makeText(this, "Invalid URL scheme", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show()
    }
}
