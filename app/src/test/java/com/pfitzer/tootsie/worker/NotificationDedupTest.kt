package com.pfitzer.tootsie.worker

import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Notification
import com.pfitzer.tootsie.domain.model.NotificationType
import com.pfitzer.tootsie.domain.util.MastodonIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDedupTest {

    @Test
    fun `the first poll shows nothing`() {
        assertTrue(notificationsToShow(null, listOf(notification("100"))).isEmpty())
    }

    @Test
    fun `only notifications newer than the last shown one are shown`() {
        // A server ignoring since_id returns the last shown and older ones again
        val fetched = listOf(notification("1000"), notification("999"), notification("998"), notification("95"))

        assertEquals(listOf("1000"), notificationsToShow("999", fetched).map { it.id })
    }

    @Test
    fun `ids are compared numerically`() {
        assertTrue(MastodonIds.isNewer("1000", "999"))
        assertTrue(MastodonIds.isNewer("110000000000000001", "110000000000000000"))
        assertFalse(MastodonIds.isNewer("999", "1000"))
        assertFalse(MastodonIds.isNewer("5", "5"))
    }

    private fun notification(id: String) = Notification(
        id = id,
        type = NotificationType.MENTION,
        createdAt = "2026-10-25T10:00:00Z",
        account = Account(
            id = "a",
            username = "a",
            acct = "a",
            displayName = "A",
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
        )
    )
}
