package com.pfitzer.tootsie.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pfitzer.tootsie.data.local.dao.AccountDao
import com.pfitzer.tootsie.data.local.dao.DraftDao
import com.pfitzer.tootsie.data.local.dao.FilterDao
import com.pfitzer.tootsie.data.local.dao.StatusDao
import com.pfitzer.tootsie.data.local.entities.AccountEntity
import com.pfitzer.tootsie.data.local.entities.DraftEntity
import com.pfitzer.tootsie.data.local.entities.FilterEntity
import com.pfitzer.tootsie.data.local.entities.StatusEntity

/**
 * Room database for the Tootsie app.
 * Stores cached Mastodon data for offline access and performance.
 */
@Database(
    entities = [
        AccountEntity::class,
        StatusEntity::class,
        DraftEntity::class,
        FilterEntity::class
    ],
    version = 6,
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
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add ownerAccountId column to statuses table (nullable)
                db.execSQL("ALTER TABLE statuses ADD COLUMN ownerAccountId TEXT")

                // 2. Handle drafts table - need to add non-nullable accountId
                // First, get the active account ID (or first account if no active account)
                val cursor = db.query(
                    "SELECT id FROM accounts WHERE isActive = 1 LIMIT 1"
                )
                val activeAccountId = if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    // No active account, get first account
                    val fallbackCursor = db.query("SELECT id FROM accounts LIMIT 1")
                    if (fallbackCursor.moveToFirst()) {
                        fallbackCursor.getString(0)
                    } else {
                        null
                    }.also { fallbackCursor.close() }
                }.also { cursor.close() }

                if (activeAccountId != null) {
                    // Create new drafts table with accountId
                    db.execSQL("""
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
                    db.execSQL("""
                        INSERT INTO drafts_new (id, accountId, content, mediaIds, visibility, sensitive, spoilerText, inReplyToId, createdAt, updatedAt)
                        SELECT id, '$activeAccountId', content, mediaIds, visibility, sensitive, spoilerText, inReplyToId, createdAt, updatedAt
                        FROM drafts
                    """.trimIndent())

                    // Drop old table and rename new one
                    db.execSQL("DROP TABLE drafts")
                    db.execSQL("ALTER TABLE drafts_new RENAME TO drafts")
                } else {
                    // No accounts exist, just recreate empty drafts table
                    db.execSQL("DROP TABLE drafts")
                    db.execSQL("""
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
                db.execSQL("DROP TABLE timeline_remote_keys")
                db.execSQL("""
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
            override fun migrate(db: SupportSQLiteDatabase) {
                // Delete all cached statuses with NULL ownerAccountId
                // These are orphaned from the v2->v3 migration
                db.execSQL("DELETE FROM statuses WHERE ownerAccountId IS NULL")
            }
        }

        /**
         * Migration from database version 4 to 5.
         * Adds the filters table for keyword and context filtering.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create filters table
                db.execSQL("""
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

        /**
         * Migration from database version 5 to 6.
         * The statuses table becomes the single source of truth for timelines
         * (RemoteMediator): one row per status, timeline and owner account, with the full
         * status as JSON. The table only holds a cache, so it is recreated instead of
         * converted; the unused timeline_remote_keys table is dropped.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS statuses")
                db.execSQL("DROP TABLE IF EXISTS timeline_remote_keys")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `statuses` (" +
                        "`id` TEXT NOT NULL, `timelineType` TEXT NOT NULL, `ownerAccountId` TEXT NOT NULL, " +
                        "`contentId` TEXT NOT NULL, `statusJson` TEXT NOT NULL, " +
                        "`favourited` INTEGER NOT NULL, `favouritesCount` INTEGER NOT NULL, " +
                        "`reblogged` INTEGER NOT NULL, `reblogsCount` INTEGER NOT NULL, " +
                        "`bookmarked` INTEGER NOT NULL, `pinned` INTEGER NOT NULL, " +
                        "`lastSyncedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`, `timelineType`, `ownerAccountId`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_statuses_ownerAccountId_timelineType` " +
                        "ON `statuses` (`ownerAccountId`, `timelineType`)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_statuses_contentId` ON `statuses` (`contentId`)")
            }
        }

        /**
         * All manual migrations, in order. Schema version 1 was never released and is
         * handled by a destructive fallback instead (see DatabaseModule).
         */
        val ALL_MIGRATIONS = arrayOf(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
    }
}
