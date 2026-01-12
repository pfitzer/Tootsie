package com.pfitzer.tootsie.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pfitzer.tootsie.domain.model.AccountPushConfig
import com.pfitzer.tootsie.domain.model.PushAlerts
import com.pfitzer.tootsie.domain.model.PushStatus
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

        // Timeline Filters
        private val KEY_TIMELINE_HIDE_BOOSTS = booleanPreferencesKey("timeline_hide_boosts")
        private val KEY_TIMELINE_HIDE_REPLIES = booleanPreferencesKey("timeline_hide_replies")
        private val KEY_TIMELINE_SHOW_ONLY_MEDIA = booleanPreferencesKey("timeline_show_only_media")
    }

    // Push Config Keys (account-scoped)
    private fun pushEnabledKey(accountId: String) = booleanPreferencesKey("push_enabled_$accountId")
    private fun pushEndpointKey(accountId: String) = stringPreferencesKey("push_endpoint_$accountId")
    private fun pushVapidPublicKey(accountId: String) = stringPreferencesKey("push_vapid_public_$accountId")
    private fun pushVapidPrivateKey(accountId: String) = stringPreferencesKey("push_vapid_private_$accountId")
    private fun pushAuthSecretKey(accountId: String) = stringPreferencesKey("push_auth_secret_$accountId")
    private fun pushStatusKey(accountId: String) = stringPreferencesKey("push_status_$accountId")

    // Alert preference keys (account-scoped)
    private fun pushAlertMentionKey(accountId: String) = booleanPreferencesKey("push_alert_mention_$accountId")
    private fun pushAlertFollowKey(accountId: String) = booleanPreferencesKey("push_alert_follow_$accountId")
    private fun pushAlertFavouriteKey(accountId: String) = booleanPreferencesKey("push_alert_favourite_$accountId")
    private fun pushAlertReblogKey(accountId: String) = booleanPreferencesKey("push_alert_reblog_$accountId")
    private fun pushAlertPollKey(accountId: String) = booleanPreferencesKey("push_alert_poll_$accountId")
    private fun pushAlertStatusKey(accountId: String) = booleanPreferencesKey("push_alert_status_$accountId")
    private fun pushAlertUpdateKey(accountId: String) = booleanPreferencesKey("push_alert_update_$accountId")
    private fun pushAlertFollowRequestKey(accountId: String) = booleanPreferencesKey("push_alert_follow_request_$accountId")

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

    // ========== Timeline Filters ==========

    /**
     * Save timeline filter preferences.
     */
    suspend fun saveTimelineFilter(hideBoosts: Boolean, hideReplies: Boolean, showOnlyMedia: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_TIMELINE_HIDE_BOOSTS] = hideBoosts
            preferences[KEY_TIMELINE_HIDE_REPLIES] = hideReplies
            preferences[KEY_TIMELINE_SHOW_ONLY_MEDIA] = showOnlyMedia
        }
    }

    /**
     * Get timeline filter as Flow.
     */
    val timelineFilter: Flow<com.pfitzer.tootsie.domain.model.TimelineFilter> = dataStore.data.map { preferences ->
        com.pfitzer.tootsie.domain.model.TimelineFilter(
            hideBoosts = preferences[KEY_TIMELINE_HIDE_BOOSTS] ?: false,
            hideReplies = preferences[KEY_TIMELINE_HIDE_REPLIES] ?: false,
            showOnlyMedia = preferences[KEY_TIMELINE_SHOW_ONLY_MEDIA] ?: false
        )
    }

    /**
     * Get timeline filter (suspend function for one-time read).
     */
    suspend fun getTimelineFilter(): com.pfitzer.tootsie.domain.model.TimelineFilter {
        return timelineFilter.first()
    }

    // ========== Push Notifications ==========

    /**
     * Save push configuration for a specific account.
     * IMPORTANT: Use .first() not .collect() when reading from DataStore.
     */
    suspend fun savePushConfig(config: AccountPushConfig) {
        dataStore.edit { preferences ->
            preferences[pushEnabledKey(config.accountId)] = config.enabled
            config.endpoint?.let { preferences[pushEndpointKey(config.accountId)] = it }
            config.vapidPublicKey?.let { preferences[pushVapidPublicKey(config.accountId)] = it }
            config.vapidPrivateKey?.let { preferences[pushVapidPrivateKey(config.accountId)] = it }
            config.authSecret?.let { preferences[pushAuthSecretKey(config.accountId)] = it }
            preferences[pushStatusKey(config.accountId)] = config.status.name

            // Save alerts
            preferences[pushAlertMentionKey(config.accountId)] = config.alerts.mention
            preferences[pushAlertFollowKey(config.accountId)] = config.alerts.follow
            preferences[pushAlertFavouriteKey(config.accountId)] = config.alerts.favourite
            preferences[pushAlertReblogKey(config.accountId)] = config.alerts.reblog
            preferences[pushAlertPollKey(config.accountId)] = config.alerts.poll
            preferences[pushAlertStatusKey(config.accountId)] = config.alerts.status
            preferences[pushAlertUpdateKey(config.accountId)] = config.alerts.update
            preferences[pushAlertFollowRequestKey(config.accountId)] = config.alerts.followRequest
        }
    }

    /**
     * Get push configuration for a specific account.
     * Returns default config if not found.
     */
    suspend fun getPushConfig(accountId: String): AccountPushConfig {
        return dataStore.data.map { preferences ->
            AccountPushConfig(
                accountId = accountId,
                enabled = preferences[pushEnabledKey(accountId)] ?: false,
                endpoint = preferences[pushEndpointKey(accountId)],
                vapidPublicKey = preferences[pushVapidPublicKey(accountId)],
                vapidPrivateKey = preferences[pushVapidPrivateKey(accountId)],
                authSecret = preferences[pushAuthSecretKey(accountId)],
                status = PushStatus.valueOf(
                    preferences[pushStatusKey(accountId)] ?: PushStatus.DISABLED.name
                ),
                alerts = PushAlerts(
                    mention = preferences[pushAlertMentionKey(accountId)] ?: true,
                    follow = preferences[pushAlertFollowKey(accountId)] ?: true,
                    favourite = preferences[pushAlertFavouriteKey(accountId)] ?: true,
                    reblog = preferences[pushAlertReblogKey(accountId)] ?: true,
                    poll = preferences[pushAlertPollKey(accountId)] ?: false,
                    status = preferences[pushAlertStatusKey(accountId)] ?: false,
                    update = preferences[pushAlertUpdateKey(accountId)] ?: false,
                    followRequest = preferences[pushAlertFollowRequestKey(accountId)] ?: false
                )
            )
        }.first()  // IMPORTANT: Use .first() not .collect()
    }

    /**
     * Delete push configuration for a specific account.
     * Removes all push-related preferences for the account.
     */
    suspend fun deletePushConfig(accountId: String) {
        dataStore.edit { preferences ->
            preferences.remove(pushEnabledKey(accountId))
            preferences.remove(pushEndpointKey(accountId))
            preferences.remove(pushVapidPublicKey(accountId))
            preferences.remove(pushVapidPrivateKey(accountId))
            preferences.remove(pushAuthSecretKey(accountId))
            preferences.remove(pushStatusKey(accountId))
            preferences.remove(pushAlertMentionKey(accountId))
            preferences.remove(pushAlertFollowKey(accountId))
            preferences.remove(pushAlertFavouriteKey(accountId))
            preferences.remove(pushAlertReblogKey(accountId))
            preferences.remove(pushAlertPollKey(accountId))
            preferences.remove(pushAlertStatusKey(accountId))
            preferences.remove(pushAlertUpdateKey(accountId))
            preferences.remove(pushAlertFollowRequestKey(accountId))
        }
    }

    /**
     * Get active account ID as one-time read (suspend function).
     */
    suspend fun getActiveAccountId(): String? {
        return dataStore.data.map { it[KEY_ACTIVE_ACCOUNT_ID] }.first()
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
