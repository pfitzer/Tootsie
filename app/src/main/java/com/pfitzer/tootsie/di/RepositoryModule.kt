package com.pfitzer.tootsie.di

import com.pfitzer.tootsie.data.repository.*
import com.pfitzer.tootsie.domain.repository.*
import com.pfitzer.tootsie.data.repository.ReportRepositoryImpl
import com.pfitzer.tootsie.domain.repository.ReportRepository
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

    /**
     * Binds HashtagTimelineRepositoryImpl to HashtagTimelineRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindHashtagTimelineRepository(
        hashtagTimelineRepositoryImpl: HashtagTimelineRepositoryImpl
    ): HashtagTimelineRepository

    /**
     * Binds TrendingRepositoryImpl to TrendingRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindTrendingRepository(
        trendingRepositoryImpl: TrendingRepositoryImpl
    ): TrendingRepository

    /**
     * Binds TrendingPostsRepositoryImpl to TrendingPostsRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindTrendingPostsRepository(
        trendingPostsRepositoryImpl: TrendingPostsRepositoryImpl
    ): TrendingPostsRepository

    /**
     * Binds TrendingLinksRepositoryImpl to TrendingLinksRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindTrendingLinksRepository(
        trendingLinksRepositoryImpl: TrendingLinksRepositoryImpl
    ): TrendingLinksRepository

    /**
     * Binds FollowedTagsRepositoryImpl to FollowedTagsRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindFollowedTagsRepository(
        followedTagsRepositoryImpl: FollowedTagsRepositoryImpl
    ): FollowedTagsRepository

    /**
     * Binds PushRepositoryImpl to PushRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindPushRepository(
        pushRepositoryImpl: PushRepositoryImpl
    ): PushRepository

    @Binds
    @Singleton
    abstract fun bindConversationRepository(
        conversationRepositoryImpl: ConversationRepositoryImpl
    ): ConversationRepository

    /**
     * Binds FilterRepositoryImpl to FilterRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindFilterRepository(
        filterRepositoryImpl: FilterRepositoryImpl
    ): FilterRepository

    /**
     * Binds FollowRequestsRepositoryImpl to FollowRequestsRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindFollowRequestsRepository(
        followRequestsRepositoryImpl: FollowRequestsRepositoryImpl
    ): FollowRequestsRepository

    /**
     * Binds AnnouncementRepositoryImpl to AnnouncementRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindAnnouncementRepository(
        announcementRepositoryImpl: AnnouncementRepositoryImpl
    ): AnnouncementRepository

    /**
     * Binds ReportRepositoryImpl to ReportRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindReportRepository(
        reportRepositoryImpl: ReportRepositoryImpl
    ): ReportRepository

    /**
     * Binds InstanceRepositoryImpl to InstanceRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindInstanceRepository(
        instanceRepositoryImpl: InstanceRepositoryImpl
    ): InstanceRepository

    /**
     * Binds DomainBlockRepositoryImpl to DomainBlockRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindDomainBlockRepository(
        domainBlockRepositoryImpl: DomainBlockRepositoryImpl
    ): DomainBlockRepository

    /**
     * Binds ScheduledStatusRepositoryImpl to ScheduledStatusRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindScheduledStatusRepository(
        scheduledStatusRepositoryImpl: ScheduledStatusRepositoryImpl
    ): ScheduledStatusRepository
}
