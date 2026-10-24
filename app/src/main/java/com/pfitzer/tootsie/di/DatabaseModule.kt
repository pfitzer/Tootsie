package com.pfitzer.tootsie.di

import android.content.Context
import androidx.room.Room
import com.pfitzer.tootsie.data.local.TootsieDatabase
import com.pfitzer.tootsie.data.local.dao.AccountDao
import com.pfitzer.tootsie.data.local.dao.DraftDao
import com.pfitzer.tootsie.data.local.dao.FilterDao
import com.pfitzer.tootsie.data.local.dao.StatusDao
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
            .addMigrations(*TootsieDatabase.ALL_MIGRATIONS)
            // Schema v1 only existed in early development builds and was never released
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)
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
     * Provides DraftDao from the database.
     */
    @Provides
    @Singleton
    fun provideDraftDao(database: TootsieDatabase): DraftDao {
        return database.draftDao()
    }

    /**
     * Provides FilterDao from the database.
     */
    @Provides
    @Singleton
    fun provideFilterDao(database: TootsieDatabase): FilterDao {
        return database.filterDao()
    }
}
