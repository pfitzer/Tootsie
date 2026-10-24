package com.pfitzer.tootsie.presentation.util

import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pfitzer.tootsie.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Resolves every ViewModel message (error_*, success_*) in English and German, with a
 * placeholder argument, so broken formats or escapes fail here instead of in the app.
 */
@RunWith(AndroidJUnit4::class)
class UiTextResourcesTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val messageIds = R.string::class.java.fields
        .filter { it.name.startsWith("error_") || it.name.startsWith("success_") }
        .associate { it.name to it.getInt(null) }

    private fun localized(locale: Locale) = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply { setLocale(locale) }
    )

    @Test
    fun allMessagesResolveInEnglishAndGerman() {
        assertTrue("expected the ViewModel messages", messageIds.size >= 70)
        val english = localized(Locale.ENGLISH)
        val german = localized(Locale.GERMAN)

        messageIds.forEach { (name, id) ->
            val en = UiText.of(id, "DETAIL").asString(english)
            val de = UiText.of(id, "DETAIL").asString(german)
            assertFalse("$name contains an unresolved placeholder: $de", de.contains("%"))
            assertFalse("$name contains an unresolved placeholder: $en", en.contains("%"))
            assertNotEquals("$name is not translated", en, de)
        }
    }

    @Test
    fun placeholdersAreFilled() {
        val german = localized(Locale.GERMAN)

        assertTrue(UiText.of(R.string.error_follow_hashtag, "kotlin").asString(german).contains("#kotlin"))
        assertTrue(UiText.of(R.string.error_post_failed, "HTTP 422").asString(german).endsWith("HTTP 422"))
    }
}
