package com.pfitzer.tootsie.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationTest {

    @Test
    fun `text, content warning, poll options and alt texts are replaced, nothing else`() {
        val status = status()
        val translated = status.withTranslation(
            Translation(
                content = "<p>Hello</p>",
                spoilerText = "Food",
                pollOptions = listOf("Yes"),
                mediaDescriptions = mapOf("m1" to "A cat"),
                detectedSourceLanguage = "de",
                provider = "DeepL.com"
            )
        )

        assertEquals("<p>Hello</p>", translated.content)
        assertEquals("Food", translated.spoilerText)
        // Options without a translation keep their title
        assertEquals(listOf("Yes", "Nein"), translated.poll!!.options.map { it.title })
        assertEquals(listOf("A cat", "Ein Hund"), translated.mediaAttachments.map { it.description })
        assertEquals(status.id, translated.id)
        assertEquals(status.favouritesCount, translated.favouritesCount)
    }

    @Test
    fun `an empty translated content warning keeps the original`() {
        val translated = status().withTranslation(
            Translation(content = "<p>Hello</p>", spoilerText = "", detectedSourceLanguage = null, provider = null)
        )

        assertEquals("Essen", translated.spoilerText)
    }

    private fun status() = Status(
        id = "1",
        uri = "https://example.social/statuses/1",
        url = null,
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
        ),
        content = "<p>Hallo</p>",
        createdAt = "2026-10-25T09:00:00Z",
        visibility = StatusVisibility.PUBLIC,
        sensitive = false,
        spoilerText = "Essen",
        mediaAttachments = listOf(
            MediaAttachment(id = "m1", type = MediaType.IMAGE, url = "", previewUrl = null, description = "Eine Katze"),
            MediaAttachment(id = "m2", type = MediaType.IMAGE, url = "", previewUrl = null, description = "Ein Hund")
        ),
        reblogsCount = 0,
        favouritesCount = 7,
        repliesCount = 0,
        poll = Poll(
            id = "p",
            expiresAt = null,
            expired = false,
            multiple = false,
            votesCount = 0,
            votersCount = 0,
            voted = false,
            options = listOf(PollOption("Ja"), PollOption("Nein"))
        ),
        language = "de"
    )
}
