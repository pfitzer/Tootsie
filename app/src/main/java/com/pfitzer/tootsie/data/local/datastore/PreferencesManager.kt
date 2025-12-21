package com.pfitzer.tootsie.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property to create DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tootsie_preferences")

/**
 * Manager for app preferences using Jetpack DataStore.
 * Stores user settings, active account info, and other persistent data.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        // Authentication
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_ACTIVE_ACCOUNT_ID = stringPreferencesKey("active_account_id")
        private val KEY_INSTANCE_URL = stringPreferencesKey("instance_url")

        // App Settings
        private val KEY_THEME = stringPreferencesKey("theme") // "light", "dark", "system"
        private val KEY_LANGUAGE = stringPreferencesKey("language") // "en", "de", "system"
        private val KEY_DEFAULT_VISIBILITY = stringPreferencesKey("default_visibility")

        // Feature Flags
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    // ========== Authentication ==========

    /**
     * Save access token for the active account.
     */
    suspend fun saveAccessToken(token: String) {
        dataStore.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = token
        }
    }

    /**
     * Get access token as Flow.
     */
    val accessToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_ACCESS_TOKEN]
    }

    /**
     * Get access token (suspend function for one-time read).
     */
    suspend fun getAccessToken(): String? {
        return dataStore.data.map { it[KEY_ACCESS_TOKEN] }.first()
    }

    /**
     * Clear access token (logout).
     */
    suspend fun clearAccessToken() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_ACCESS_TOKEN)
        }
    }

    /**
     * Save active account ID.
     */
    suspend fun saveActiveAccountId(accountId: String) {
        dataStore.edit { preferences ->
            preferences[KEY_ACTIVE_ACCOUNT_ID] = accountId
        }
    }

    /**
     * Get active account ID as Flow.
     */
    val activeAccountId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_ACTIVE_ACCOUNT_ID]
    }

    /**
     * Clear active account ID.
     */
    suspend fun clearActiveAccountId() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_ACTIVE_ACCOUNT_ID)
        }
    }

    /**
     * Save instance URL for the active account.
     */
    suspend fun saveInstanceUrl(instanceUrl: String) {
        dataStore.edit { preferences ->
            preferences[KEY_INSTANCE_URL] = instanceUrl
        }
    }

    /**
     * Get instance URL as Flow.
     */
    val instanceUrl: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_INSTANCE_URL]
    }

    /**
     * Get instance URL (suspend function for one-time read).
     */
    suspend fun getInstanceUrl(): String? {
        return dataStore.data.map { it[KEY_INSTANCE_URL] }.first()
    }

    /**
     * Clear instance URL.
     */
    suspend fun clearInstanceUrl() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_INSTANCE_URL)
        }
    }

    // ========== App Settings ==========

    /**
     * Save theme preference.
     */
    suspend fun saveTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME] = theme
        }
    }

    /**
     * Get theme preference as Flow.
     * Returns "system" by default.
     */
    val theme: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_THEME] ?: "system"
    }

    /**
     * Save language preference.
     */
    suspend fun saveLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = language
        }
    }

    /**
     * Get language preference as Flow.
     * Returns "system" by default.
     */
    val language: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_LANGUAGE] ?: "system"
    }

    /**
     * Save default post visibility.
     */
    suspend fun saveDefaultVisibility(visibility: String) {
        dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_VISIBILITY] = visibility
        }
    }

    /**
     * Get default post visibility as Flow.
     * Returns "public" by default.
     */
    val defaultVisibility: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_DEFAULT_VISIBILITY] ?: "public"
    }

    // ========== Feature Flags ==========

    /**
     * Mark onboarding as completed.
     */
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    /**
     * Check if onboarding is completed.
     */
    val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }

    // ========== Utility ==========

    /**
     * Clear all preferences (full app reset).
     */
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Clear all authentication data (logout).
     */
    suspend fun clearAuthData() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_ACCESS_TOKEN)
            preferences.remove(KEY_ACTIVE_ACCOUNT_ID)
            preferences.remove(KEY_INSTANCE_URL)
        }
    }
}
