package com.pfitzer.tootsie.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pfitzer.tootsie.data.local.entities.AccountEntity
import com.pfitzer.tootsie.data.local.entities.DraftEntity
import com.pfitzer.tootsie.data.local.entities.FilterEntity
import com.pfitzer.tootsie.data.local.entities.StatusEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Removing an account deletes its cached data, and only its data.
 */
@RunWith(AndroidJUnit4::class)
class AccountDaoDeleteTest {

    private lateinit var database: TootsieDatabase

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
    fun deleteAccountWithData_removesOnlyThatAccountsData() = runBlocking {
        for (id in listOf("gone", "kept")) {
            database.accountDao().insertAccount(account(id))
            database.statusDao().insertStatuses(listOf(status("s-$id", owner = id)))
            database.draftDao().insertDraft(DraftEntity(accountId = id, content = "draft", createdAt = 0, updatedAt = 0))
            database.filterDao().insertFilter(filter("f-$id", owner = id))
        }

        database.accountDao().deleteAccountWithData(account("gone"))

        assertNull(database.accountDao().getAccountById("gone"))
        assertEquals("kept", database.accountDao().getAccountById("kept")?.id)
        assertEquals(0, database.draftDao().getAllDrafts("gone").first().size)
        assertEquals(1, database.draftDao().getAllDrafts("kept").first().size)
        assertNull(database.statusDao().getStatusById("s-gone"))
        assertEquals("s-kept", database.statusDao().getStatusById("s-kept")?.id)
        assertEquals(0, database.filterDao().getFilters("gone").first().size)
        assertEquals(listOf("f-kept"), database.filterDao().getFilters("kept").first().map { it.id })
    }

    private fun account(id: String) = AccountEntity(
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

    private fun status(id: String, owner: String) = StatusEntity(
        id = id,
        timelineType = "home",
        ownerAccountId = owner,
        contentId = id,
        statusJson = "{}",
        favourited = false,
        favouritesCount = 0,
        reblogged = false,
        reblogsCount = 0,
        bookmarked = false,
        pinned = false
    )

    private fun filter(id: String, owner: String) = FilterEntity(
        id = id,
        title = id,
        contextJson = "[]",
        expiresAt = null,
        filterAction = "warn",
        keywordsJson = "[]",
        ownerAccountId = owner
    )
}
