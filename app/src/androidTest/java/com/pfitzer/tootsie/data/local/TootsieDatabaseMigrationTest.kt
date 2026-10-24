package com.pfitzer.tootsie.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the manual Room migrations against the exported schemas in app/schemas.
 */
@RunWith(AndroidJUnit4::class)
class TootsieDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TootsieDatabase::class.java
    )

    @Test
    fun migrate2To3_assignsDraftsToActiveAccount() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            insertAccount(db, "acc-inactive", isActive = false)
            insertAccount(db, "acc-active", isActive = true)
            insertDraftV2(db, id = 1, content = "first draft")
            insertDraftV2(db, id = 2, content = "second draft")
            insertStatus(db, "status-1", ownerAccountId = null, schemaVersion = 2)
            insertRemoteKeyV2(db, "status-1")
        }

        helper.runMigrationsAndValidate(TEST_DB, 3, true, TootsieDatabase.MIGRATION_2_3).use { db ->
            db.query("SELECT id, accountId, content FROM drafts ORDER BY id").use { cursor ->
                assertEquals(2, cursor.count)
                cursor.moveToFirst()
                assertEquals(1L, cursor.getLong(0))
                assertEquals("acc-active", cursor.getString(1))
                assertEquals("first draft", cursor.getString(2))
                cursor.moveToNext()
                assertEquals("acc-active", cursor.getString(1))
                assertEquals("second draft", cursor.getString(2))
            }

            // Existing statuses are kept, but without an owner
            db.query("SELECT ownerAccountId FROM statuses WHERE id = 'status-1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.isNull(0))
            }

            // Remote keys are recreated empty with the new composite key
            assertEquals(0, count(db, "timeline_remote_keys"))
        }
    }

    @Test
    fun migrate2To3_fallsBackToFirstAccountWhenNoneIsActive() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            insertAccount(db, "acc-only", isActive = false)
            insertDraftV2(db, id = 1, content = "draft")
        }

        helper.runMigrationsAndValidate(TEST_DB, 3, true, TootsieDatabase.MIGRATION_2_3).use { db ->
            db.query("SELECT accountId FROM drafts").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("acc-only", cursor.getString(0))
            }
        }
    }

    @Test
    fun migrate2To3_dropsDraftsWhenNoAccountExists() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            insertDraftV2(db, id = 1, content = "orphaned draft")
        }

        helper.runMigrationsAndValidate(TEST_DB, 3, true, TootsieDatabase.MIGRATION_2_3).use { db ->
            assertEquals(0, count(db, "drafts"))
        }
    }

    @Test
    fun migrate3To4_deletesStatusesWithoutOwner() {
        helper.createDatabase(TEST_DB, 3).use { db ->
            insertStatus(db, "orphan", ownerAccountId = null, schemaVersion = 3)
            insertStatus(db, "owned", ownerAccountId = "acc-1", schemaVersion = 3)
        }

        helper.runMigrationsAndValidate(TEST_DB, 4, true, TootsieDatabase.MIGRATION_3_4).use { db ->
            db.query("SELECT id FROM statuses").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("owned", cursor.getString(0))
            }
        }
    }

    @Test
    fun migrate4To5_createsFiltersTable() {
        helper.createDatabase(TEST_DB, 4).use { db ->
            insertStatus(db, "owned", ownerAccountId = "acc-1", schemaVersion = 4)
        }

        helper.runMigrationsAndValidate(TEST_DB, 5, true, TootsieDatabase.MIGRATION_4_5).use { db ->
            assertEquals(1, count(db, "statuses"))
            assertEquals(0, count(db, "filters"))
        }
    }

    @Test
    fun migrate5To6_recreatesStatusCacheAndDropsRemoteKeys() {
        helper.createDatabase(TEST_DB, 5).use { db ->
            insertAccount(db, "acc-1", isActive = true)
            insertStatus(db, "old", ownerAccountId = "acc-1", schemaVersion = 5)
        }

        helper.runMigrationsAndValidate(TEST_DB, 6, true, TootsieDatabase.MIGRATION_5_6).use { db ->
            // Only the timeline cache is cleared
            assertEquals(0, count(db, "statuses"))
            assertEquals(1, count(db, "accounts"))
            db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'timeline_remote_keys'").use {
                assertEquals(0, it.count)
            }
        }
    }

    @Test
    fun migrateAll_opensWithCurrentSchema() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            insertAccount(db, "acc-active", isActive = true)
            insertDraftV2(db, id = 1, content = "draft")
        }

        // Opening through Room validates the migrated database against the current entities
        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            TootsieDatabase::class.java,
            TEST_DB
        )
            .addMigrations(*TootsieDatabase.ALL_MIGRATIONS)
            .build()
        try {
            val db = database.openHelper.writableDatabase
            assertEquals(1, count(db, "drafts"))
            assertEquals(1, count(db, "accounts"))
        } finally {
            database.close()
        }
    }

    private fun insertAccount(db: SupportSQLiteDatabase, id: String, isActive: Boolean) {
        db.execSQL(
            """
            INSERT INTO accounts (id, username, acct, displayName, note, url, avatar, header,
                locked, followersCount, followingCount, statusesCount, createdAt, bot,
                isActive, lastSyncedAt)
            VALUES (?, 'user', 'user@example.social', 'User', '', 'https://example.social/@user',
                '', '', 0, 0, 0, 0, '2025-01-01T00:00:00Z', 0, ?, 0)
            """.trimIndent(),
            arrayOf<Any>(id, if (isActive) 1 else 0)
        )
    }

    private fun insertDraftV2(db: SupportSQLiteDatabase, id: Long, content: String) {
        db.execSQL(
            """
            INSERT INTO drafts (id, content, visibility, sensitive, createdAt, updatedAt)
            VALUES (?, ?, 'public', 0, 1000, 2000)
            """.trimIndent(),
            arrayOf<Any>(id, content)
        )
    }

    private fun insertStatus(
        db: SupportSQLiteDatabase,
        id: String,
        ownerAccountId: String?,
        schemaVersion: Int
    ) {
        val ownerColumn = if (schemaVersion >= 3) ", ownerAccountId" else ""
        val ownerValue = if (schemaVersion >= 3) ", ?" else ""
        val args = mutableListOf<Any?>(id)
        if (schemaVersion >= 3) args += ownerAccountId
        db.execSQL(
            """
            INSERT INTO statuses (id, accountId, uri, content, createdAt, visibility, sensitive,
                spoilerText, reblogsCount, favouritesCount, repliesCount, reblogged, favourited,
                bookmarked, pinned, accountJson, lastSyncedAt$ownerColumn)
            VALUES (?, 'author', 'https://example.social/1', '<p>Hi</p>', '2025-01-01T00:00:00Z',
                'public', 0, '', 0, 0, 0, 0, 0, 0, 0, '{}', 0$ownerValue)
            """.trimIndent(),
            args.toTypedArray()
        )
    }

    private fun insertRemoteKeyV2(db: SupportSQLiteDatabase, statusId: String) {
        db.execSQL(
            "INSERT INTO timeline_remote_keys (statusId, timelineType, lastRefreshed) VALUES (?, 'HOME', 0)",
            arrayOf(statusId)
        )
    }

    private fun count(db: SupportSQLiteDatabase, table: String): Int =
        db.query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
