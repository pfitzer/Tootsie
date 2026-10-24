package com.pfitzer.tootsie.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pfitzer.tootsie.data.local.TootsieDatabase
import com.pfitzer.tootsie.data.local.entities.StatusEntity
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.remote.dto.StatusDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
@RunWith(AndroidJUnit4::class)
class TimelineRemoteMediatorTest {

    private lateinit var database: TootsieDatabase
    private val requests = mutableListOf<String?>()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            TootsieDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun refreshReplacesCachedTimelineWithNewestPage() = runBlocking {
        insert(mediator { _, _ -> listOf(status("5")) }, LoadType.REFRESH)

        val result = mediator { _, _ -> listOf(status("12"), status("11"), status("10")) }
            .load(LoadType.REFRESH, emptyState())

        assertFalse((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(listOf("12", "11", "10"), cachedIds())
        assertEquals(listOf<String?>(null), requests.takeLast(1))
    }

    @Test
    fun appendLoadsPageOlderThanOldestCachedStatus() = runBlocking {
        insert(mediator { _, _ -> listOf(status("12"), status("10")) }, LoadType.REFRESH)

        mediator { _, _ -> listOf(status("9"), status("8")) }.load(LoadType.APPEND, emptyState())

        assertEquals("10", requests.last())
        // Sorted numerically: "9" comes after "10"
        assertEquals(listOf("12", "10", "9", "8"), cachedIds())
    }

    @Test
    fun appendWithoutOlderPostsEndsPagination() = runBlocking {
        insert(mediator { _, _ -> listOf(status("10")) }, LoadType.REFRESH)

        val result = mediator { _, _ -> emptyList() }.load(LoadType.APPEND, emptyState())

        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
    }

    @Test
    fun networkErrorKeepsCache() = runBlocking {
        insert(mediator { _, _ -> listOf(status("10")) }, LoadType.REFRESH)

        val result = mediator { _, _ -> throw IOException("offline") }.load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Error)
        assertEquals(listOf("10"), cachedIds())
    }

    @Test
    fun timelinesAndAccountsAreCachedSeparately() = runBlocking {
        insert(mediator { _, _ -> listOf(status("10")) }, LoadType.REFRESH)
        insert(mediator(timelineType = "federated") { _, _ -> listOf(status("10")) }, LoadType.REFRESH)
        insert(mediator(owner = "acc-2") { _, _ -> listOf(status("10")) }, LoadType.REFRESH)

        // Refreshing one timeline doesn't touch the others
        insert(mediator { _, _ -> listOf(status("11")) }, LoadType.REFRESH)

        assertEquals(listOf("11"), cachedIds())
        assertEquals(listOf("10"), cachedIds(timelineType = "federated"))
        assertEquals(listOf("10"), cachedIds(owner = "acc-2"))
    }

    @Test
    fun initializeSkipsRefreshOnlyForRecentCache() = runBlocking {
        assertEquals(
            RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH,
            mediator { _, _ -> emptyList() }.initialize()
        )

        insert(mediator { _, _ -> listOf(status("10")) }, LoadType.REFRESH)
        assertEquals(
            RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH,
            mediator { _, _ -> emptyList() }.initialize()
        )

        val later = System.currentTimeMillis() + TimelineRemoteMediator.CACHE_TIMEOUT_MS + 1
        assertEquals(
            RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH,
            mediator(now = { later }) { _, _ -> emptyList() }.initialize()
        )
    }

    @Test
    fun interactionUpdateReachesBoostsAndKeepsFullStatus() = runBlocking {
        val boost = status(
            "20",
            extra = ""","reblog":${statusJson("7")},"quote":{"state":"accepted","quoted_status_id":"3"}"""
        )
        insert(mediator { _, _ -> listOf(boost, status("7")) }, LoadType.REFRESH)

        database.statusDao().updateInteractionState(
            statusId = "7",
            favourited = true,
            favouritesCount = 1,
            reblogged = false,
            reblogsCount = 0,
            bookmarked = true,
            pinned = false
        )

        val statuses = loadPage().map { it.toDomain() }
        val boosted = statuses.first { it.id == "20" }
        assertTrue(boosted.reblog!!.favourited)
        assertTrue(boosted.reblog!!.bookmarked)
        assertTrue(statuses.first { it.id == "7" }.favourited)
        // Fields the old cache dropped survive the round trip
        assertEquals("3", boosted.quote?.quotedStatusId)
        assertNull(boosted.reblog!!.quote)
    }

    private fun mediator(
        timelineType: String = "home",
        owner: String = "acc-1",
        now: () -> Long = System::currentTimeMillis,
        fetch: suspend (String?, Int) -> List<StatusDto>
    ) = TimelineRemoteMediator(
        database = database,
        timelineType = timelineType,
        ownerAccountId = owner,
        fetchPage = { maxId, limit ->
            requests += maxId
            fetch(maxId, limit)
        },
        now = now
    )

    private suspend fun insert(mediator: TimelineRemoteMediator, loadType: LoadType) {
        assertTrue(mediator.load(loadType, emptyState()) is RemoteMediator.MediatorResult.Success)
    }

    private fun emptyState() = PagingState<Int, StatusEntity>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = 20, initialLoadSize = 40),
        leadingPlaceholderCount = 0
    )

    private suspend fun loadPage(timelineType: String = "home", owner: String = "acc-1"): List<StatusEntity> {
        val result = database.statusDao().pagingSource(timelineType, owner).load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 100, placeholdersEnabled = false)
        )
        return (result as PagingSource.LoadResult.Page).data
    }

    private suspend fun cachedIds(timelineType: String = "home", owner: String = "acc-1") =
        loadPage(timelineType, owner).map { it.id }

    private fun status(id: String, extra: String = ""): StatusDto =
        json.decodeFromString(statusJson(id, extra))

    private fun statusJson(id: String, extra: String = "") = """{"id":"$id",
        "uri":"https://example.social/statuses/$id","account":$ACCOUNT,"content":"<p>$id</p>",
        "created_at":"2026-10-24T06:00:00.000Z","visibility":"public","sensitive":false,
        "spoiler_text":"","reblogs_count":0,"favourites_count":0,"replies_count":0$extra}"""

    private companion object {
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

        const val ACCOUNT = """{"id":"1","username":"alice","acct":"alice@example.social",
            "display_name":"Alice","note":"","url":"https://example.social/@alice","avatar":"",
            "header":"","locked":false,"bot":false,"followers_count":0,"following_count":0,
            "statuses_count":0,"created_at":"2025-01-01T00:00:00.000Z"}"""
    }
}
