package com.pfitzer.tootsie.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pfitzer.tootsie.data.local.dao.AccountDao
import com.pfitzer.tootsie.data.local.dao.DraftDao
import com.pfitzer.tootsie.data.local.dao.FilterDao
import com.pfitzer.tootsie.data.local.dao.StatusDao
import com.pfitzer.tootsie.data.local.dao.TimelineRemoteKeysDao
import com.pfitzer.tootsie.data.local.entities.AccountEntity
import com.pfitzer.tootsie.data.local.entities.DraftEntity
import com.pfitzer.tootsie.data.local.entities.FilterEntity
import com.pfitzer.tootsie.data.local.entities.StatusEntity
import com.pfitzer.tootsie.data.local.entities.TimelineRemoteKeys

/**
 * Room database for the Tootsie app.
 * Stores cached Mastodon data for offline access and performance.
 */
@Database(
    entities = [
        AccountEntity::class,
        StatusEntity::class,
        TimelineRemoteKeys::class,
        DraftEntity::class,
        FilterEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class TootsieDatabase : RoomDatabase() {

    /**
     * Provides access to Account operations.
     */
    abstract fun accountDao(): AccountDao

    /**
     * Provides access to Status operations.
     */
    abstract fun statusDao(): StatusDao

    /**
     * Provides access to Timeline Remote Keys operations.
     */
    abstract fun timelineRemoteKeysDao(): TimelineRemoteKeysDao

    /**
     * Provides access to Draft operations.
     */
    abstract fun draftDao(): DraftDao

    /**
     * Provides access to Filter operations.
     */
    abstract fun filterDao(): FilterDao

    companion object {
        const val DATABASE_NAME = "tootsie_database"

        /**
         * Migration from database version 2 to 3.
         * Adds multi-account support by:
         * 1. Adding ownerAccountId to statuses table
         * 2. Adding accountId to drafts table (assigns existing drafts to active account)
         * 3. Recreating timeline_remote_keys with composite primary key
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Add ownerAccountId column to statuses table (nullable)
                database.execSQL("ALTER TABLE statuses ADD COLUMN ownerAccountId TEXT")

                // 2. Handle drafts table - need to add non-nullable accountId
                // First, get the active account ID (or first account if no active account)
                val cursor = database.query(
                    "SELECT id FROM accounts WHERE isActive = 1 LIMIT 1"
                )
                val activeAccountId = if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    // No active account, get first account
                    val fallbackCursor = database.query("SELECT id FROM accounts LIMIT 1")
                    if (fallbackCursor.moveToFirst()) {
                        fallbackCursor.getString(0)
                    } else {
                        null
                    }.also { fallbackCursor.close() }
                }.also { cursor.close() }

                if (activeAccountId != null) {
                    // Create new drafts table with accountId
                    database.execSQL("""
                        CREATE TABLE drafts_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            accountId TEXT NOT NULL,
                            content TEXT NOT NULL,
                            mediaIds TEXT,
                            visibility TEXT NOT NULL DEFAULT 'public',
                            sensitive INTEGER NOT NULL DEFAULT 0,
                            spoilerText TEXT,
                            inReplyToId TEXT,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL
                        )
                    """.trimIndent())

                    // Copy existing drafts with active account ID
                    database.execSQL("""
                        INSERT INTO drafts_new (id, accountId, content, mediaIds, visibility, sensitive, spoilerText, inReplyToId, createdAt, updatedAt)
                        SELECT id, '$activeAccountId', content, mediaIds, visibility, sensitive, spoilerText, inReplyToId, createdAt, updatedAt
                        FROM drafts
                    """.trimIndent())

                    // Drop old table and rename new one
                    database.execSQL("DROP TABLE drafts")
                    database.execSQL("ALTER TABLE drafts_new RENAME TO drafts")
                } else {
                    // No accounts exist, just recreate empty drafts table
                    database.execSQL("DROP TABLE drafts")
                    database.execSQL("""
                        CREATE TABLE drafts (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            accountId TEXT NOT NULL,
                            content TEXT NOT NULL,
                            mediaIds TEXT,
                            visibility TEXT NOT NULL DEFAULT 'public',
                            sensitive INTEGER NOT NULL DEFAULT 0,
                            spoilerText TEXT,
                            inReplyToId TEXT,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL
                        )
                    """.trimIndent())
                }

                // 3. Recreate timeline_remote_keys with composite primary key
                database.execSQL("DROP TABLE timeline_remote_keys")
                database.execSQL("""
                    CREATE TABLE timeline_remote_keys (
                        statusId TEXT NOT NULL,
                        timelineType TEXT NOT NULL,
                        ownerAccountId TEXT NOT NULL,
                        prevKey TEXT,
                        nextKey TEXT,
                        lastRefreshed INTEGER NOT NULL,
                        PRIMARY KEY(statusId, timelineType, ownerAccountId)
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration from database version 3 to 4.
         * Fixes issue where existing cached statuses had NULL ownerAccountId.
         * Solution: Clear all old cached statuses (they will be re-fetched from API).
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Delete all cached statuses with NULL ownerAccountId
                // These are orphaned from the v2->v3 migration
                database.execSQL("DELETE FROM statuses WHERE ownerAccountId IS NULL")
            }
        }

        /**
         * Migration from database version 4 to 5.
         * Adds the filters table for keyword and context filtering.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create filters table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS filters (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        contextJson TEXT NOT NULL,
                        expiresAt TEXT,
                        filterAction TEXT NOT NULL,
                        keywordsJson TEXT NOT NULL,
                        ownerAccountId TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
