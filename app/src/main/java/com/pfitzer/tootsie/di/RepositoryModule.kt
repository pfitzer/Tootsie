package com.pfitzer.tootsie.di

import com.pfitzer.tootsie.data.repository.AccountRepositoryImpl
import com.pfitzer.tootsie.data.repository.AuthRepositoryImpl
import com.pfitzer.tootsie.data.repository.BookmarksRepositoryImpl
import com.pfitzer.tootsie.data.repository.DraftRepositoryImpl
import com.pfitzer.tootsie.data.repository.EmojiRepositoryImpl
import com.pfitzer.tootsie.data.repository.FavoritesRepositoryImpl
import com.pfitzer.tootsie.data.repository.ListsRepositoryImpl
import com.pfitzer.tootsie.data.repository.ListTimelineRepositoryImpl
import com.pfitzer.tootsie.data.repository.NotificationRepositoryImpl
import com.pfitzer.tootsie.data.repository.SearchRepositoryImpl
import com.pfitzer.tootsie.data.repository.StatusRepositoryImpl
import com.pfitzer.tootsie.data.repository.TimelineRepositoryImpl
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.domain.repository.AuthRepository
import com.pfitzer.tootsie.domain.repository.BookmarksRepository
import com.pfitzer.tootsie.domain.repository.DraftRepository
import com.pfitzer.tootsie.domain.repository.EmojiRepository
import com.pfitzer.tootsie.domain.repository.FavoritesRepository
import com.pfitzer.tootsie.domain.repository.ListsRepository
import com.pfitzer.tootsie.domain.repository.ListTimelineRepository
import com.pfitzer.tootsie.domain.repository.NotificationRepository
import com.pfitzer.tootsie.domain.repository.SearchRepository
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.domain.repository.TimelineRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides repository implementations.
 * Uses @Binds to bind implementations to their interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds AuthRepositoryImpl to AuthRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    /**
     * Binds TimelineRepositoryImpl to TimelineRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindTimelineRepository(
        timelineRepositoryImpl: TimelineRepositoryImpl
    ): TimelineRepository

    /**
     * Binds StatusRepositoryImpl to StatusRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindStatusRepository(
        statusRepositoryImpl: StatusRepositoryImpl
    ): StatusRepository

    /**
     * Binds AccountRepositoryImpl to AccountRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        accountRepositoryImpl: AccountRepositoryImpl
    ): AccountRepository

    /**
     * Binds NotificationRepositoryImpl to NotificationRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: NotificationRepositoryImpl
    ): NotificationRepository

    /**
     * Binds SearchRepositoryImpl to SearchRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindSearchRepository(
        searchRepositoryImpl: SearchRepositoryImpl
    ): SearchRepository

    /**
     * Binds DraftRepositoryImpl to DraftRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindDraftRepository(
        draftRepositoryImpl: DraftRepositoryImpl
    ): DraftRepository

    /**
     * Binds BookmarksRepositoryImpl to BookmarksRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindBookmarksRepository(
        bookmarksRepositoryImpl: BookmarksRepositoryImpl
    ): BookmarksRepository

    /**
     * Binds FavoritesRepositoryImpl to FavoritesRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(
        favoritesRepositoryImpl: FavoritesRepositoryImpl
    ): FavoritesRepository

    /**
     * Binds ListsRepositoryImpl to ListsRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindListsRepository(
        listsRepositoryImpl: ListsRepositoryImpl
    ): ListsRepository

    /**
     * Binds EmojiRepositoryImpl to EmojiRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindEmojiRepository(
        emojiRepositoryImpl: EmojiRepositoryImpl
    ): EmojiRepository

    /**
     * Binds ListTimelineRepositoryImpl to ListTimelineRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindListTimelineRepository(
        listTimelineRepositoryImpl: ListTimelineRepositoryImpl
    ): ListTimelineRepository
}
