package com.pfitzer.tootsie.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DomainNameTest {

    @Test
    fun `plain domains are lowercased and trimmed`() {
        assertEquals("example.social", DomainName.normalize("  Example.Social "))
        assertEquals("sub.example.social", DomainName.normalize("sub.example.social."))
        assertEquals("my-server.co.uk", DomainName.normalize("my-server.co.uk"))
    }

    @Test
    fun `domain is taken from urls and handles`() {
        assertEquals("example.social", DomainName.normalize("https://example.social/about"))
        assertEquals("example.social", DomainName.normalize("http://example.social:443/"))
        assertEquals("example.social", DomainName.normalize("@alice@example.social"))
        assertEquals("example.social", DomainName.normalize("alice@example.social"))
    }

    @Test
    fun `non latin domains are accepted`() {
        assertEquals("mastodon.zürich", DomainName.normalize("mastodon.Zürich"))
    }

    @Test
    fun `invalid input is rejected`() {
        assertNull(DomainName.normalize(""))
        assertNull(DomainName.normalize("localhost"))
        assertNull(DomainName.normalize("exa mple.social"))
        assertNull(DomainName.normalize("-example.social"))
        assertNull(DomainName.normalize("example..social"))
    }
}
