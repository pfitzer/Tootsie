package com.pfitzer.tootsie.data.paging

import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.FilterAction
import com.pfitzer.tootsie.domain.model.FilterContext
import com.pfitzer.tootsie.domain.model.FilterKeyword
import com.pfitzer.tootsie.domain.model.MediaAttachment
import com.pfitzer.tootsie.domain.model.MediaType
import com.pfitzer.tootsie.domain.model.Poll
import com.pfitzer.tootsie.domain.model.PollOption
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.StatusVisibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class KeywordFilterMatcherTest {

    // Typical Mastodon status HTML with a mention, a hashtag and a link
    private val mastodonHtml = """<p><span class="h-card" translate="no"><a href="https://example.social/@alice" class="u-url mention">@<span>alice</span></a></span> look at <a href="https://example.social/tags/kotlin" class="mention hashtag" rel="tag">#<span>kotlin</span></a> <a href="https://blog.example.com/post" target="_blank" rel="nofollow noopener"><span class="invisible">https://</span><span class="">blog.example.com/post</span><span class="invisible"></span></a></p>"""

    @Test
    fun `markup does not match keywords`() {
        listOf("class", "span", "href", "mention", "hashtag", "invisible", "nofollow", "h-card").forEach { keyword ->
            val matcher = matcher(keyword, wholeWord = false, action = FilterAction.HIDE)
            val status = status(content = mastodonHtml)
            assertSame("keyword '$keyword' must not match markup", status, matcher.apply(status))
        }
    }

    @Test
    fun `visible text matches keywords`() {
        val matcher = matcher("look at", wholeWord = true, action = FilterAction.HIDE)
        assertNull(matcher.apply(status(content = mastodonHtml)))
    }

    @Test
    fun `text split by tags matches`() {
        // Link text is split into invisible/visible spans by Mastodon
        val matcher = matcher("https://blog.example.com", wholeWord = false, action = FilterAction.HIDE)
        assertNull(matcher.apply(status(content = mastodonHtml)))
    }

    @Test
    fun `html entities are decoded`() {
        val matcher = matcher("Tom & Jerry's", wholeWord = false, action = FilterAction.HIDE)
        assertNull(matcher.apply(status(content = "<p>Tom &amp; Jerry&#39;s</p>")))
        assertNull(matcher.apply(status(content = "<p>Tom &amp; Jerry&#x27;s</p>")))
    }

    @Test
    fun `whole word does not match inside words`() {
        val matcher = matcher("cat", wholeWord = true, action = FilterAction.HIDE)
        val status = status(content = "<p>concatenate</p>")
        assertSame(status, matcher.apply(status))
        assertNull(matcher.apply(status(content = "<p>my cat</p>")))
    }

    @Test
    fun `paragraphs do not glue words together`() {
        val matcher = matcher("endstart", wholeWord = false, action = FilterAction.HIDE)
        val status = status(content = "<p>end</p><p>start</p>")
        assertSame(status, matcher.apply(status))
    }

    @Test
    fun `matching is case insensitive`() {
        val matcher = matcher("KOTLIN", wholeWord = true, action = FilterAction.HIDE)
        assertNull(matcher.apply(status(content = mastodonHtml)))
    }

    @Test
    fun `warn marks the status with the filter title`() {
        val matcher = matcher("spoiler", wholeWord = true, action = FilterAction.WARN)
        assertEquals("Test filter", matcher.apply(status(content = "<p>big spoiler</p>"))?.filteredByKeyword)
    }

    @Test
    fun `content warning, poll options and media descriptions are checked`() {
        val matcher = matcher("secret", wholeWord = true, action = FilterAction.HIDE)
        assertNull(matcher.apply(status(spoilerText = "a secret")))
        assertNull(
            matcher.apply(
                status(poll = Poll("p", null, false, false, 0, 0, false, options = listOf(PollOption("secret"))))
            )
        )
        assertNull(
            matcher.apply(
                status(media = listOf(MediaAttachment("m", MediaType.IMAGE, "u", null, description = "the secret")))
            )
        )
    }

    @Test
    fun `boosted status is checked`() {
        val matcher = matcher("boosted", wholeWord = true, action = FilterAction.HIDE)
        assertNull(matcher.apply(status(reblog = status(content = "<p>boosted text</p>"))))
    }

    @Test
    fun `blank keywords and empty filter lists keep the status`() {
        val status = status(content = mastodonHtml)
        assertSame(status, KeywordFilterMatcher(emptyList()).apply(status))
        assertSame(status, matcher("  ", wholeWord = false, action = FilterAction.HIDE).apply(status))
    }

    @Test
    fun `regex characters in keywords are literal`() {
        val matcher = matcher("c++", wholeWord = false, action = FilterAction.HIDE)
        assertNull(matcher.apply(status(content = "<p>I like c++</p>")))
        val other = status(content = "<p>I like cc</p>")
        assertSame(other, matcher.apply(other))
    }

    private fun matcher(keyword: String, wholeWord: Boolean, action: FilterAction) = KeywordFilterMatcher(
        listOf(
            Filter(
                id = "f",
                title = "Test filter",
                context = listOf(FilterContext.HOME),
                expiresAt = null,
                filterAction = action,
                keywords = listOf(FilterKeyword("k", keyword, wholeWord))
            )
        )
    )

    private fun status(
        content: String = "<p>Body</p>",
        spoilerText: String = "",
        poll: Poll? = null,
        media: List<MediaAttachment> = emptyList(),
        reblog: Status? = null
    ) = Status(
        id = "1",
        uri = "https://example.social/statuses/1",
        url = null,
        account = Account(
            id = "acc",
            username = "user",
            acct = "user",
            displayName = "User",
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
        spoilerText = spoilerText,
        mediaAttachments = media,
        reblogsCount = 0,
        favouritesCount = 0,
        repliesCount = 0,
        poll = poll,
        reblog = reblog
    )
}
