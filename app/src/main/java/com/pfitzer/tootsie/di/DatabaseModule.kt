package com.pfitzer.tootsie.di

import android.content.Context
import androidx.room.Room
import com.pfitzer.tootsie.data.local.TootsieDatabase
import com.pfitzer.tootsie.data.local.dao.AccountDao
import com.pfitzer.tootsie.data.local.dao.StatusDao
import com.pfitzer.tootsie.data.local.dao.TimelineRemoteKeysDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the Room database instance.
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): TootsieDatabase {
        return Room.databaseBuilder(
            context,
            TootsieDatabase::class.java,
            TootsieDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development; use migrations in production
            .build()
    }

    /**
     * Provides AccountDao from the database.
     */
    @Provides
    @Singleton
    fun provideAccountDao(database: TootsieDatabase): AccountDao {
        return database.accountDao()
    }

    /**
     * Provides StatusDao from the database.
     */
    @Provides
    @Singleton
    fun provideStatusDao(database: TootsieDatabase): StatusDao {
        return database.statusDao()
    }

    /**
     * Provides TimelineRemoteKeysDao from the database.
     */
    @Provides
    @Singleton
    fun provideTimelineRemoteKeysDao(database: TootsieDatabase): TimelineRemoteKeysDao {
        return database.timelineRemoteKeysDao()
    }
}
