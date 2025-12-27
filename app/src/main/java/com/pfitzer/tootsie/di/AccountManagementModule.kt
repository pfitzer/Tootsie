package com.pfitzer.tootsie.di

import com.pfitzer.tootsie.data.repository.AccountManagementRepositoryImpl
import com.pfitzer.tootsie.domain.repository.AccountManagementRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for account management dependencies.
 * Binds the AccountManagementRepository interface to its implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AccountManagementModule {

    /**
     * Binds AccountManagementRepositoryImpl to AccountManagementRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindAccountManagementRepository(
        impl: AccountManagementRepositoryImpl
    ): AccountManagementRepository
}
