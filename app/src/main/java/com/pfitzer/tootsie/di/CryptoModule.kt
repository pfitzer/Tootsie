package com.pfitzer.tootsie.di

import com.pfitzer.tootsie.data.local.crypto.CredentialCipher
import com.pfitzer.tootsie.data.local.crypto.EncryptedSharedPreferencesCleanup
import com.pfitzer.tootsie.data.local.crypto.KeystoreCredentialCipher
import com.pfitzer.tootsie.data.local.crypto.LegacySecureStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds credential encryption dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CryptoModule {

    /**
     * Binds the Android Keystore backed cipher.
     */
    @Binds
    @Singleton
    abstract fun bindCredentialCipher(impl: KeystoreCredentialCipher): CredentialCipher

    /**
     * Binds the cleanup of the former EncryptedSharedPreferences storage.
     */
    @Binds
    @Singleton
    abstract fun bindLegacySecureStorage(impl: EncryptedSharedPreferencesCleanup): LegacySecureStorage
}
