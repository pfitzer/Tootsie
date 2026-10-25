package com.pfitzer.tootsie.presentation.components.status

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.presentation.TranslationState
import com.pfitzer.tootsie.presentation.util.htmlToPlainText
import com.pfitzer.tootsie.presentation.util.translatorIntent
import java.util.Locale

/**
 * Translations for status cards, provided once by the nav host.
 *
 * @param states Translation state by status ID
 * @param isServerTranslationAvailable The server translates; otherwise a translator app is used
 * @param onToggle Translate a status into the given language, or show the original again
 */
data class StatusTranslations(
    val states: Map<String, TranslationState>,
    val isServerTranslationAvailable: Boolean,
    val onToggle: (status: Status, language: String) -> Unit
)

val LocalStatusTranslations = compositionLocalOf<StatusTranslations?> { null }

/**
 * Language the app shows (and translates into), e.g. "de".
 */
@Composable
internal fun appLanguage(): String = LocalConfiguration.current.locales[0].language

/**
 * Whether a status can be translated: its language is known and not the app language.
 */
internal fun Status.isTranslatable(appLanguage: String): Boolean {
    val language = language ?: return false
    return !language.equals(appLanguage, ignoreCase = true) && htmlToPlainText(content).isNotBlank()
}

/**
 * "Translate" below the content of a status in another language. With server translation
 * it toggles between translation and original and names the source and provider; without,
 * it hands the text to a translator app (hidden if none is installed).
 */
@Composable
internal fun TranslateButton(
    status: Status,
    translations: StatusTranslations,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language = appLanguage()

    if (!translations.isServerTranslationAvailable) {
        // Looking up translator apps is too slow for every recomposition while scrolling
        val intent = remember(status.id, status.content, status.spoilerText) {
            val text = listOf(status.spoilerText, htmlToPlainText(status.content))
                .filter { it.isNotBlank() }
                .joinToString("\n\n")
            translatorIntent(context, text)
        } ?: return
        TextButton(onClick = { context.startActivity(intent) }, modifier = modifier) {
            TranslateLabel(stringResource(R.string.translate))
        }
        return
    }

    val state = translations.states[status.id]
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = { translations.onToggle(status, language) },
            enabled = state != TranslationState.Loading
        ) {
            if (state == TranslationState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            TranslateLabel(
                stringResource(
                    when (state) {
                        is TranslationState.Shown -> R.string.translate_show_original
                        TranslationState.Failed -> R.string.translate_retry
                        else -> R.string.translate
                    }
                )
            )
        }
        if (state is TranslationState.Shown) {
            val from = state.translation.detectedSourceLanguage ?: status.language
            val fromName = from?.let { Locale.forLanguageTag(it).getDisplayLanguage(Locale.forLanguageTag(language)) }
            val provider = state.translation.provider
            Text(
                text = if (provider.isNullOrBlank()) {
                    stringResource(R.string.translated_from, fromName.orEmpty())
                } else {
                    stringResource(R.string.translated_from_with, fromName.orEmpty(), provider)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

@Composable
private fun TranslateLabel(text: String) {
    Icon(Icons.Outlined.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
    Spacer(modifier = Modifier.width(4.dp))
    Text(text, style = MaterialTheme.typography.labelMedium)
}
