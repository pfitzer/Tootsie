package com.pfitzer.tootsie.presentation.components.status

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.MediaAttachment
import com.pfitzer.tootsie.domain.model.MediaType
import com.pfitzer.tootsie.domain.model.Poll
import com.pfitzer.tootsie.domain.model.PollOption
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.StatusVisibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What TalkBack gets from a status card.
 */
@RunWith(AndroidJUnit4::class)
class StatusCardAccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun actionButtons_includeTheirCount() {
        setContent(status(replies = 1, favourites = 12))

        val favourite = resources.getString(R.string.cd_favorite) + ", " +
            resources.getQuantityString(R.plurals.cd_favourites_count, 12, 12)
        val reply = resources.getString(R.string.cd_reply) + ", " +
            resources.getQuantityString(R.plurals.cd_replies_count, 1, 1)
        composeRule.onNodeWithContentDescription(favourite, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithContentDescription(reply, useUnmergedTree = true).assertExists()
        // Without boosts there is no count to read
        composeRule.onNodeWithContentDescription(resources.getString(R.string.cd_boost), useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun card_offersItsMainActions() {
        val replied = mutableListOf<String>()
        setContent(status(), onReplyClick = { replied += it.id })

        val card = composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
        val actions = card.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(
            listOf(
                resources.getString(R.string.cd_reply),
                resources.getString(R.string.cd_boost),
                resources.getString(R.string.cd_favorite),
                resources.getString(R.string.cd_bookmark),
                resources.getString(R.string.cd_profile_of, "User")
            ),
            actions.map { it.label }
        )

        composeRule.runOnIdle { actions.first().action() }
        assertEquals(listOf("status-1"), replied)
    }

    @Test
    fun avatar_namesTheProfile() {
        setContent(status())

        composeRule.onNodeWithContentDescription(resources.getString(R.string.cd_profile_of, "User"), useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun mediaWithoutAltText_isStillDescribed() {
        setContent(status(media = listOf(image("1"), image("2"))))

        val images = composeRule.onAllNodesWithContentDescription(resources.getString(R.string.cd_image), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertEquals(2, images.size)
    }

    @Test
    fun pollOptions_areRadioButtons() {
        setContent(status(poll = poll(multiple = false)))

        val options = composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected))
            .fetchSemanticsNodes()
        assertEquals(2, options.size)
        assertTrue(options.all { it.config.getOrNull(SemanticsProperties.Role) == Role.RadioButton })
    }

    private fun setContent(status: Status, onReplyClick: (Status) -> Unit = {}) {
        composeRule.setContent {
            MaterialTheme {
                StatusCard(
                    status = status,
                    onStatusClick = {},
                    onAvatarClick = {},
                    onFavoriteClick = {},
                    onBoostClick = {},
                    onReplyClick = onReplyClick,
                    onBookmarkClick = {}
                )
            }
        }
    }

    private fun image(id: String) = MediaAttachment(
        id = id,
        type = MediaType.IMAGE,
        url = "https://example.social/$id.png",
        previewUrl = "https://example.social/$id-small.png",
        description = null
    )

    private fun poll(multiple: Boolean) = Poll(
        id = "poll-1",
        expiresAt = null,
        expired = false,
        multiple = multiple,
        votesCount = 0,
        votersCount = 0,
        voted = false,
        options = listOf(PollOption("Yes"), PollOption("No"))
    )

    private fun status(
        replies: Int = 0,
        favourites: Int = 0,
        media: List<MediaAttachment> = emptyList(),
        poll: Poll? = null
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
        content = "<p>Body</p>",
        createdAt = "2025-01-01T00:00:00Z",
        visibility = StatusVisibility.PUBLIC,
        sensitive = false,
        spoilerText = "",
        mediaAttachments = media,
        reblogsCount = 0,
        favouritesCount = favourites,
        repliesCount = replies,
        poll = poll
    )
}
