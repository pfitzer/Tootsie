package com.pfitzer.tootsie.data.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.testing.TestPager
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.data.remote.dto.AccountDto
import com.pfitzer.tootsie.data.remote.dto.RelationshipDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.Headers.Companion.headersOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class FollowListPagingSourceTest {

    private val api = mockk<MastodonApiService>()
    private val config = PagingConfig(pageSize = 40, initialLoadSize = 40)

    @Test
    fun `relationships are merged into the page and the next page comes from the Link header`() = runTest {
        coEvery { api.getRelationships(listOf("a", "b")) } returns listOf(relationship("a", following = true))
        val pager = TestPager(config, FollowListPagingSource(api) { _, _ ->
            Response.success(
                listOf(account("a"), account("b")),
                headersOf("Link", "<https://example.social/api/v1/accounts/1/followers?max_id=99>; rel=\"next\"")
            )
        })

        val page = pager.refresh() as PagingSource.LoadResult.Page

        assertEquals(listOf("a", "b"), page.data.map { it.account.id })
        assertTrue(page.data[0].relationship!!.following)
        assertNull(page.data[1].relationship)
        assertEquals("99", page.nextKey)
    }

    @Test
    fun `the list is shown without relationships if they fail to load`() = runTest {
        coEvery { api.getRelationships(any()) } throws IOException("offline")
        val pager = TestPager(config, FollowListPagingSource(api) { _, _ -> Response.success(listOf(account("a"))) })

        val page = pager.refresh() as PagingSource.LoadResult.Page

        assertEquals(listOf("a"), page.data.map { it.account.id })
        assertNull(page.data.single().relationship)
        assertNull(page.nextKey)
    }

    private fun account(id: String) = AccountDto(
        id = id,
        username = id,
        acct = id,
        displayName = id,
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

    private fun relationship(id: String, following: Boolean) = RelationshipDto(
        id = id,
        following = following,
        followedBy = false,
        blocking = false,
        blockedBy = false,
        muting = false,
        mutingNotifications = false,
        requested = false,
        domainBlocking = false,
        showingReblogs = true,
        endorsed = false,
        notifying = false,
        note = ""
    )
}
