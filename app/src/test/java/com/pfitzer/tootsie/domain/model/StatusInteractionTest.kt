package com.pfitzer.tootsie.domain.model

import com.pfitzer.tootsie.domain.repository.StatusInteractionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusInteractionTest {

    @Test
    fun `withInteractions applies override to matching status`() {
        val status = status("1")
        val result = status.withInteractions(
            mapOf("1" to status.toInteraction().copy(favourited = true, favouritesCount = 6))
        )

        assertTrue(result.favourited)
        assertEquals(6, result.favouritesCount)
    }

    @Test
    fun `withInteractions applies override to boosted status`() {
        val boost = status("wrapper", reblog = status("original"))
        val result = boost.withInteractions(
            mapOf("original" to status("original").toInteraction().copy(reblogged = true, reblogsCount = 3))
        )

        assertTrue(result.reblog!!.reblogged)
        assertEquals(3, result.reblog!!.reblogsCount)
        assertFalse(result.reblogged)
    }

    @Test
    fun `withInteractions returns same instance without matching override`() {
        val status = status("1", reblog = status("2"))

        assertSame(status, status.withInteractions(mapOf("other" to status.toInteraction())))
        assertSame(status, status.withInteractions(emptyMap()))
    }

    @Test
    fun `store records the original status of a boost response`() {
        val store = StatusInteractionStore()
        val response = status("wrapper", reblog = status("original").copy(reblogged = true))

        store.update(response)

        assertEquals(setOf("original"), store.interactions.value.keys)
        assertTrue(store.interactions.value.getValue("original").reblogged)
    }

    @Test
    fun `store keeps the latest state and can be cleared`() {
        val store = StatusInteractionStore()
        store.update(status("1").copy(favourited = true))
        store.update(status("1").copy(favourited = false))

        assertFalse(store.interactions.value.getValue("1").favourited)

        store.clear()
        assertTrue(store.interactions.value.isEmpty())
    }

    @Test
    fun `an edit is applied to the status and to boosts of it, and kept by later interactions`() {
        val store = StatusInteractionStore()
        val edited = status("1").copy(content = "<p>New</p>", editedAt = "2026-10-25T10:00:00Z")
        store.updateEdited(edited)
        // A later favourite response must not drop the edit
        store.update(edited.copy(content = "<p>New</p>", favourited = true))

        val boost = status("wrapper", reblog = status("1"))
        val shown = boost.withInteractions(store.interactions.value).reblog!!

        assertEquals("<p>New</p>", shown.content)
        assertEquals("2026-10-25T10:00:00Z", shown.editedAt)
        assertTrue(shown.favourited)
        assertEquals("<p>New</p>", status("1").withInteractions(store.interactions.value).content)
    }

    private fun status(id: String, reblog: Status? = null) = Status(
        id = id,
        uri = "https://example.social/statuses/$id",
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
        content = "",
        createdAt = "2025-01-01T00:00:00Z",
        visibility = StatusVisibility.PUBLIC,
        sensitive = false,
        spoilerText = "",
        reblogsCount = 0,
        favouritesCount = 5,
        repliesCount = 0,
        reblog = reblog
    )
}
