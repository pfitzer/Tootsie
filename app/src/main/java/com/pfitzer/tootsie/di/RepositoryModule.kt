package com.pfitzer.tootsie.di

import com.pfitzer.tootsie.data.repository.AccountRepositoryImpl
import com.pfitzer.tootsie.data.repository.AuthRepositoryImpl
import com.pfitzer.tootsie.data.repository.NotificationRepositoryImpl
import com.pfitzer.tootsie.data.repository.StatusRepositoryImpl
import com.pfitzer.tootsie.data.repository.TimelineRepositoryImpl
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.domain.repository.AuthRepository
import com.pfitzer.tootsie.domain.repository.NotificationRepository
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
}
