package com.pfitzer.tootsie.data.paging

import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.FilterAction
import com.pfitzer.tootsie.domain.model.FilterKeyword
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.StatusVisibility
import com.pfitzer.tootsie.domain.model.TimelineFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class TimelineStatusFilterTest {

    @Test
    fun `no filter keeps every status`() {
        val filter = TimelineStatusFilter(TimelineFilter.DEFAULT, emptyList())
        val boost = status("2", reblog = status("1"))

        assertSame(boost, filter.apply(boost))
    }

    @Test
    fun `timeline filter hides boosts and replies`() {
        val filter = TimelineStatusFilter(TimelineFilter(hideBoosts = true, hideReplies = true), emptyList())

        assertNull(filter.apply(status("2", reblog = status("1"))))
        assertNull(filter.apply(status("3", inReplyToId = "1")))
        assertEquals("4", filter.apply(status("4"))?.id)
    }

    @Test
    fun `keyword filter hides or warns`() {
        val filter = TimelineStatusFilter(
            TimelineFilter.DEFAULT,
            listOf(
                keywordFilter("Spoilers", "finale", FilterAction.WARN),
                keywordFilter("Hidden", "crypto", FilterAction.HIDE)
            )
        )

        assertNull(filter.apply(status("1", content = "<p>Crypto news</p>")))
        assertEquals("Spoilers", filter.apply(status("2", content = "<p>The finale!</p>"))?.filteredByKeyword)
        assertNull(filter.apply(status("3", content = "<p>Hello</p>"))?.filteredByKeyword)
    }

    private fun keywordFilter(title: String, keyword: String, action: FilterAction) = Filter(
        id = title,
        title = title,
        context = emptyList(),
        expiresAt = null,
        filterAction = action,
        keywords = listOf(FilterKeyword(id = keyword, keyword = keyword, wholeWord = false))
    )

    private fun status(
        id: String,
        content: String = "<p>$id</p>",
        reblog: Status? = null,
        inReplyToId: String? = null
    ) = Status(
        id = id,
        uri = "https://example.social/statuses/$id",
        url = null,
        account = Account(
            id = "1", username = "alice", acct = "alice", displayName = "Alice", note = "",
            url = "", avatar = "", header = "", locked = false, followersCount = 0,
            followingCount = 0, statusesCount = 0, createdAt = "2025-01-01T00:00:00Z", bot = false
        ),
        content = content,
        createdAt = "2026-10-24T06:00:00Z",
        visibility = StatusVisibility.PUBLIC,
        sensitive = false,
        spoilerText = "",
        reblogsCount = 0,
        favouritesCount = 0,
        repliesCount = 0,
        reblog = reblog,
        inReplyToId = inReplyToId
    )
}
