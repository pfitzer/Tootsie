package com.pfitzer.tootsie.presentation.components.status

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Quote
import com.pfitzer.tootsie.domain.model.QuotePermission
import com.pfitzer.tootsie.domain.model.QuoteState
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.StatusVisibility
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatusCardQuoteTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val boostDescription = context.getString(R.string.cd_boost)
    private val boostLabel = context.getString(R.string.status_boost)
    private val quoteLabel = context.getString(R.string.status_quote)

    private val boosted = mutableListOf<String>()
    private val quoted = mutableListOf<String>()
    private val opened = mutableListOf<String>()

    @Test
    fun boostButton_offersBoostAndQuote() {
        setContent(status("1", permission = QuotePermission.AUTOMATIC))

        composeRule.onNodeWithContentDescription(boostDescription).performClick()
        composeRule.onNodeWithText(quoteLabel).assertIsDisplayed().performClick()

        assertEquals(listOf("1"), quoted)
        assertEquals(emptyList<String>(), boosted)
    }

    @Test
    fun boostOption_boostsAsBefore() {
        setContent(status("1", permission = QuotePermission.AUTOMATIC))

        composeRule.onNodeWithContentDescription(boostDescription).performClick()
        composeRule.onNodeWithText(boostLabel).performClick()

        assertEquals(listOf("1"), boosted)
        assertEquals(emptyList<String>(), quoted)
    }

    @Test
    fun deniedQuote_isNotSelectable() {
        setContent(status("1", permission = QuotePermission.DENIED))

        composeRule.onNodeWithContentDescription(boostDescription).performClick()
        composeRule.onNodeWithText(context.getString(R.string.status_quote_not_allowed)).assertIsDisplayed()
        composeRule.onNodeWithText(quoteLabel).performClick()

        assertEquals(emptyList<String>(), quoted)
    }

    @Test
    fun serverWithoutQuoteSupport_boostsDirectly() {
        setContent(status("1", permission = null))

        composeRule.onNodeWithContentDescription(boostDescription).performClick()

        assertEquals(listOf("1"), boosted)
        composeRule.onNodeWithText(quoteLabel).assertDoesNotExist()
    }

    @Test
    fun quotedStatus_isShownAndOpensOnClick() {
        val original = status("2", content = "<p>The original post</p>")
        setContent(status("1", quote = Quote(QuoteState.ACCEPTED, original, "2")))

        composeRule.onNodeWithText("The original post").assertIsDisplayed().performClick()

        assertEquals(listOf("2"), opened)
    }

    @Test
    fun pendingQuote_showsHintInsteadOfContent() {
        setContent(status("1", quote = Quote(QuoteState.PENDING, null, "2")))

        composeRule.onNodeWithText(context.getString(R.string.status_quote_pending)).assertIsDisplayed()
    }

    private fun setContent(status: Status) {
        composeRule.setContent {
            CompositionLocalProvider(LocalOnQuoteStatus provides { quoted += it.id }) {
                MaterialTheme {
                    StatusCard(
                        status = status,
                        onStatusClick = { opened += it },
                        onAvatarClick = {},
                        onFavoriteClick = {},
                        onBoostClick = { boosted += it.id },
                        onReplyClick = {},
                        onBookmarkClick = {}
                    )
                }
            }
        }
    }

    private fun status(
        id: String,
        content: String = "<p>Body</p>",
        permission: QuotePermission? = QuotePermission.AUTOMATIC,
        quote: Quote? = null
    ) = Status(
        id = id,
        uri = "https://example.social/statuses/$id",
        url = null,
        account = Account(
            id = "acc-$id",
            username = "user$id",
            acct = "user$id",
            displayName = "User $id",
            note = "",
            url = "",
            avatar = "",
            header = "",
            locked = false,
            followersCount = 0,
            followingCount = 0,
            statusesCount = 0,
            createdAt = "2025-01-01T00:00:00Z",
            bot = false
        ),
        content = content,
        createdAt = "2025-01-01T00:00:00Z",
        visibility = StatusVisibility.PUBLIC,
        sensitive = false,
        spoilerText = "",
        reblogsCount = 0,
        favouritesCount = 0,
        repliesCount = 0,
        quote = quote,
        quotePermission = permission
    )
}
