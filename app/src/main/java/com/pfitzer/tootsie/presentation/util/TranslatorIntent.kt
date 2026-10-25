package com.pfitzer.tootsie.presentation.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/**
 * Intent that hands [text] to an installed translator app (DeepL, Google Translate,
 * Bergamot, …), or null if none is installed. Used when the server can't translate.
 * Needs the matching <queries> in the manifest.
 */
fun translatorIntent(context: Context, text: String): Intent? {
    val translate = Intent(Intent.ACTION_TRANSLATE).putExtra(Intent.EXTRA_TEXT, text)
    // Many translator apps only offer themselves for selected text
    val processText = Intent(Intent.ACTION_PROCESS_TEXT)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_PROCESS_TEXT, text)
        .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)

    return listOf(translate, processText)
        .firstOrNull { context.packageManager.hasActivityFor(it) }
        ?.let { Intent.createChooser(it, null) }
}

private fun PackageManager.hasActivityFor(intent: Intent): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0)).isNotEmpty()
    } else {
        @Suppress("DEPRECATION")
        queryIntentActivities(intent, 0).isNotEmpty()
    }
