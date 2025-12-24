package com.pfitzer.tootsie.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pfitzer.tootsie.data.local.dao.AccountDao
import com.pfitzer.tootsie.data.local.dao.DraftDao
import com.pfitzer.tootsie.data.local.dao.StatusDao
import com.pfitzer.tootsie.data.local.dao.TimelineRemoteKeysDao
import com.pfitzer.tootsie.data.local.entities.AccountEntity
import com.pfitzer.tootsie.data.local.entities.DraftEntity
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
        DraftEntity::class
    ],
    version = 2,
    exportSchema = false
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

    companion object {
        const val DATABASE_NAME = "tootsie_database"
    }
}
