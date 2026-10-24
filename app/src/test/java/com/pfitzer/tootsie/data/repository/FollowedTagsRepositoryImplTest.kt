package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.remote.LinkHeader
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.data.remote.dto.FollowedTagDto
import com.pfitzer.tootsie.data.remote.dto.FollowedTagHistoryDto
import com.pfitzer.tootsie.domain.model.FollowedTag
import com.pfitzer.tootsie.domain.model.FollowedTagSort
import com.pfitzer.tootsie.domain.model.TagHistory
import com.pfitzer.tootsie.util.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.Headers.Companion.headersOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response

class FollowedTagsRepositoryImplTest {

    private val api = mockk<MastodonApiService>()
    private val repository = FollowedTagsRepositoryImpl(api)

    @Test
    fun `next max_id from the Link header`() {
        assertEquals(
            "123",
            LinkHeader.nextMaxId(
                """<https://example.social/api/v1/followed_tags?limit=100&max_id=123>; rel="next", """ +
                    """<https://example.social/api/v1/followed_tags?limit=100&since_id=456>; rel="prev""""
            )
        )
        assertNull(LinkHeader.nextMaxId("""<https://example.social/api/v1/followed_tags?since_id=456>; rel="prev""""))
        assertNull(LinkHeader.nextMaxId(null))
    }

    @Test
    fun `all pages are loaded`() = runTest {
        coEvery { api.getFollowedTags(limit = 100, maxId = null) } returns page(
            listOf("a", "b"),
            next = "https://example.social/api/v1/followed_tags?max_id=7"
        )
        coEvery { api.getFollowedTags(limit = 100, maxId = "7") } returns page(listOf("c"), next = null)

        val result = repository.getFollowedTags()

        assertEquals(listOf("a", "b", "c"), (result as Result.Success).data.map { it.name })
    }

    @Test
    fun `alphabetical sort ignores case, activity sort uses the last 7 days`() {
        val tags = listOf(tag("zebra", 1), tag("Apple", 5), tag("banana", 5), tag("cherry", 9))

        assertEquals(listOf("Apple", "banana", "cherry", "zebra"), FollowedTagSort.ALPHABETICAL.sort(tags).map { it.name })
        assertEquals(listOf("cherry", "Apple", "banana", "zebra"), FollowedTagSort.ACTIVITY.sort(tags).map { it.name })
        assertEquals(FollowedTagSort.ALPHABETICAL, FollowedTagSort.fromName(null))
    }

    private fun page(names: List<String>, next: String?): Response<List<FollowedTagDto>> {
        val body = names.map { FollowedTagDto(name = it, url = "", history = emptyList<FollowedTagHistoryDto>(), following = true) }
        return if (next != null) {
            Response.success(body, headersOf("Link", "<$next>; rel=\"next\""))
        } else {
            Response.success(body)
        }
    }

    private fun tag(name: String, uses: Int) = FollowedTag(
        name = name,
        url = "",
        history = listOf(TagHistory(day = "0", uses = uses, accounts = 1)),
        following = true
    )
}
