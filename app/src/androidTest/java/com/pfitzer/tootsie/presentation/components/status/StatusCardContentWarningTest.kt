package com.pfitzer.tootsie.presentation.components.status

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.MediaAttachment
import com.pfitzer.tootsie.domain.model.MediaType
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.StatusVisibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatusCardContentWarningTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val showMore = context.getString(R.string.status_show_more)
    private val showLess = context.getString(R.string.status_show_less)
    private val tapToShow = context.getString(R.string.media_sensitive_tap_to_show)

    @Test
    fun contentWarning_hidesBodyUntilExpanded() {
        setContent(status(spoilerText = "Spoilers", content = "<p>Secret body</p>"))

        composeRule.onNodeWithText("CW: Spoilers").assertIsDisplayed()
        composeRule.onNodeWithText("Secret body").assertDoesNotExist()

        composeRule.onNodeWithText(showMore).performClick()
        composeRule.onNodeWithText("Secret body").assertIsDisplayed()

        composeRule.onNodeWithText(showLess).performClick()
        composeRule.onNodeWithText("Secret body").assertDoesNotExist()
    }

    @Test
    fun contentWarning_hidesMediaUntilExpanded() {
        setContent(status(spoilerText = "Spoilers", media = listOf(image("1"))))

        composeRule.onNodeWithContentDescription("alt 1").assertDoesNotExist()
        composeRule.onNodeWithText(showMore).performClick()
        composeRule.onNodeWithContentDescription("alt 1").assertExists()
    }

    @Test
    fun withoutContentWarning_bodyIsShown() {
        setContent(status(content = "<p>Plain body</p>"))

        composeRule.onNodeWithText("Plain body").assertIsDisplayed()
        composeRule.onNodeWithText(showMore).assertDoesNotExist()
    }

    @Test
    fun sensitiveMedia_isCoveredAndNotClickableUntilRevealed() {
        val opened = mutableListOf<String>()
        setContent(
            status(sensitive = true, media = listOf(image("1"))),
            onImageClick = { url, _ -> opened += url }
        )

        // First tap only reveals the media
        composeRule.onNodeWithText(tapToShow).assertIsDisplayed().performClick()
        assertTrue(opened.isEmpty())
        composeRule.onNodeWithText(tapToShow).assertDoesNotExist()

        // Second tap opens the image
        composeRule.onNodeWithContentDescription("alt 1").performClick()
        assertEquals(listOf("https://example.social/1.png"), opened)
    }

    @Test
    fun nonSensitiveMedia_isNotCovered() {
        setContent(status(media = listOf(image("1"))))

        composeRule.onNodeWithText(tapToShow).assertDoesNotExist()
    }

    private fun setContent(
        status: Status,
        onImageClick: (String, String?) -> Unit = { _, _ -> }
    ) {
        composeRule.setContent {
            MaterialTheme {
                StatusCard(
                    status = status,
                    onStatusClick = {},
                    onAvatarClick = {},
                    onFavoriteClick = {},
                    onBoostClick = {},
                    onReplyClick = {},
                    onBookmarkClick = {},
                    onImageClick = onImageClick
                )
            }
        }
    }

    private fun image(id: String) = MediaAttachment(
        id = id,
        type = MediaType.IMAGE,
        url = "https://example.social/$id.png",
        previewUrl = "https://example.social/$id-small.png",
        description = "alt $id"
    )

    private fun status(
        content: String = "<p>Body</p>",
        spoilerText: String = "",
        sensitive: Boolean = false,
        media: List<MediaAttachment> = emptyList()
    ) = Status(
        id = "status-1",
        uri = "https://example.social/statuses/1",
        url = null,
        account = Account(
            id = "acc-1",
            username = "user",
            acct = "user",
            displayName = "User",
            note = "",
            url = "https://example.social/@user",
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
        sensitive = sensitive,
        spoilerText = spoilerText,
        mediaAttachments = media,
        reblogsCount = 0,
        favouritesCount = 0,
        repliesCount = 0
    )
}
