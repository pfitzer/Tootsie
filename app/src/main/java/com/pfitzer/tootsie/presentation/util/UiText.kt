package com.pfitzer.tootsie.presentation.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Text for the UI that ViewModels can produce without access to resources.
 * Resolved in the UI, so it follows the current (possibly changed at runtime) app language.
 */
sealed interface UiText {

    /** A string resource with optional format arguments */
    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    /** Text that is not translatable, e.g. user content */
    data class Dynamic(val value: String) : UiText

    fun asString(context: Context): String = when (this) {
        is Resource -> context.getString(id, *args.toTypedArray())
        is Dynamic -> value
    }

    @Composable
    fun asString(): String = asString(LocalContext.current)

    companion object {
        fun of(@StringRes id: Int, vararg args: Any): UiText = Resource(id, args.toList())
    }
}
