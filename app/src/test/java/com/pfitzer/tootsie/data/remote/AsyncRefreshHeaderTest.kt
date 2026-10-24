package com.pfitzer.tootsie.data.remote

import com.pfitzer.tootsie.domain.model.AsyncRefresh
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AsyncRefreshHeaderTest {

    @Test
    fun `parses the documented format`() {
        val id = "ImNvbnRleHQ6MTEzNjQwNTczMzAzNzg1MTc4OnJlZnJlc2gi--c526259eb4a1f3ef0d4b91cf8c99bf501330a815"

        assertEquals(
            AsyncRefresh(id = id, retrySeconds = 3, resultCount = 2),
            AsyncRefreshHeader.parse("id=\"$id\", retry=3, result_count=2")
        )
    }

    @Test
    fun `result count is optional`() {
        assertEquals(
            AsyncRefresh(id = "abc", retrySeconds = 5, resultCount = null),
            AsyncRefreshHeader.parse("id=\"abc\", retry=5")
        )
    }

    @Test
    fun `tolerates spacing and order and clamps retry`() {
        assertEquals(
            AsyncRefresh(id = "abc", retrySeconds = 1, resultCount = 0),
            AsyncRefreshHeader.parse("result_count=0,retry=0 , id = \"abc\"")
        )
    }

    @Test
    fun `missing retry uses a default`() {
        assertEquals(3, AsyncRefreshHeader.parse("id=\"abc\"")?.retrySeconds)
    }

    @Test
    fun `missing header or id yields null`() {
        assertNull(AsyncRefreshHeader.parse(null))
        assertNull(AsyncRefreshHeader.parse(""))
        assertNull(AsyncRefreshHeader.parse("retry=3, result_count=1"))
        assertNull(AsyncRefreshHeader.parse("id=\"\", retry=3"))
    }
}
