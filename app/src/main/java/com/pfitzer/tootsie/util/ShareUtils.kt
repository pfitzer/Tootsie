package com.pfitzer.tootsie.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.Html
import android.widget.Toast
import com.pfitzer.tootsie.R

/**
 * Utility class for sharing content and copying to clipboard.
 */
object ShareUtils {

    /**
     * Shares a status URL using the Android share sheet.
     */
    fun shareStatus(context: Context, url: String?, content: String) {
        if (url == null) return

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.share_status))
        context.startActivity(shareIntent)
    }

    /**
     * Copies the given link to the clipboard.
     */
    fun copyLinkToClipboard(context: Context, url: String?) {
        if (url == null) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Mastodon Link", url)
        clipboard.setPrimaryClip(clip)
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies the status content (plain text) to the clipboard.
     */
    fun copyTextToClipboard(context: Context, htmlContent: String) {
        val plainText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(htmlContent).toString()
        }
        
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Mastodon Post", plainText)
        clipboard.setPrimaryClip(clip)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, R.string.text_copied, Toast.LENGTH_SHORT).show()
        }
    }
}
