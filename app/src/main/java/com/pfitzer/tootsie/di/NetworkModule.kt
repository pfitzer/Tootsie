package com.pfitzer.tootsie.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.data.remote.api.AuthApiService
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.data.remote.interceptors.AuthInterceptor
import com.pfitzer.tootsie.data.remote.interceptors.InstanceInterceptor
import com.pfitzer.tootsie.data.remote.interceptors.InstanceUrlProvider
import com.pfitzer.tootsie.data.remote.interceptors.TokenProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hilt module that binds provider interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindsModule {

    /**
     * Binds TokenProvider implementation.
     */
    @Binds
    @Singleton
    abstract fun bindTokenProvider(impl: TokenProviderImpl): TokenProvider

    /**
     * Binds InstanceUrlProvider implementation.
     */
    @Binds
    @Singleton
    abstract fun bindInstanceUrlProvider(impl: com.pfitzer.tootsie.data.remote.interceptors.InstanceUrlProviderImpl): InstanceUrlProvider
}

/**
 * Hilt module that provides network-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Provides JSON serializer configuration for Retrofit.
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true // Ignore fields not in our DTOs
        coerceInputValues = true // Use default values for null fields
        isLenient = true
    }

    /**
     * Provides HTTP logging interceptor for debugging.
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Log full request/response
        }
    }

    /**
     * Provides OkHttpClient with interceptors.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor,
        instanceInterceptor: InstanceInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(instanceInterceptor) // Must be first to set correct host
            .addInterceptor(authInterceptor) // Add auth headers
            .addInterceptor(loggingInterceptor) // Log requests/responses
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Provides Retrofit instance.
     * Note: Base URL is a placeholder; actual URL is set by InstanceInterceptor.
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl("https://mastodon.social/") // Placeholder base URL
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    /**
     * Provides MastodonApiService.
     */
    @Provides
    @Singleton
    fun provideMastodonApiService(retrofit: Retrofit): MastodonApiService {
        return retrofit.create(MastodonApiService::class.java)
    }

    /**
     * Provides AuthApiService.
     */
    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }
}

/**
 * Implementation of TokenProvider.
 */
class TokenProviderImpl @Inject constructor(
    private val preferencesManager: PreferencesManager
) : TokenProvider {
    override suspend fun getToken(): String? {
        return preferencesManager.getAccessToken()
    }
}

