package com.pfitzer.tootsie.data.local.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.pfitzer.tootsie.domain.model.AccountPushConfig
import com.pfitzer.tootsie.domain.model.PushAlerts
import com.pfitzer.tootsie.domain.model.PushStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

// Extension property to create DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tootsie_preferences")

/**
 * Manager for app preferences using Jetpack DataStore.
 * Sensitive credentials (OAuth token, instance URL) are stored in EncryptedSharedPreferences.
 * Non-sensitive settings (theme, filters, etc.) remain in plaintext DataStore.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        // Legacy DataStore keys — used only for one-time migration to encrypted storage
        private val KEY_ACCESS_TOKEN_LEGACY = stringPreferencesKey("access_token")
        private val KEY_INSTANCE_URL_LEGACY = stringPreferencesKey("instance_url")

        // Non-sensitive DataStore keys
        private val KEY_ACTIVE_ACCOUNT_ID = stringPreferencesKey("active_account_id")
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_DEFAULT_VISIBILITY = stringPreferencesKey("default_visibility")
        private val KEY_TEXT_SIZE = stringPreferencesKey("text_size")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_TIMELINE_HIDE_BOOSTS = booleanPreferencesKey("timeline_hide_boosts")
        private val KEY_TIMELINE_HIDE_REPLIES = booleanPreferencesKey("timeline_hide_replies")
        private val KEY_TIMELINE_SHOW_ONLY_MEDIA = booleanPreferencesKey("timeline_show_only_media")
        private val KEY_DATABASE_RETENTION_HOURS = stringPreferencesKey("database_retention_hours")

        // Keys inside EncryptedSharedPreferences
        private const val SECURE_PREFS_FILE = "tootsie_secure_prefs"
        private const val SECURE_KEY_ACCESS_TOKEN = "access_token"
        private const val SECURE_KEY_INSTANCE_URL = "instance_url"
    }

    // Initialized lazily on first IO-thread access; MasterKey.Builder uses Android Keystore
    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
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
    private fun lastNotificationIdKey(accountId: String) = stringPreferencesKey("last_notification_id_$accountId")

    // ========== Authentication (EncryptedSharedPreferences) ==========

    /**
     * Save access token encrypted. Also removes any legacy plaintext entry.
     */
    suspend fun saveAccessToken(token: String) {
        encryptedPrefs.edit().putString(SECURE_KEY_ACCESS_TOKEN, token).apply()
        dataStore.edit { it.remove(KEY_ACCESS_TOKEN_LEGACY) }
    }

    /**
     * Observe access token as Flow (backed by EncryptedSharedPreferences).
     * flowOn(IO) ensures the Keystore-backed lazy init runs off the main thread.
     */
    val accessToken: Flow<String?> = callbackFlow {
        trySend(encryptedPrefs.getString(SECURE_KEY_ACCESS_TOKEN, null))
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SECURE_KEY_ACCESS_TOKEN) {
                trySend(encryptedPrefs.getString(SECURE_KEY_ACCESS_TOKEN, null))
            }
        }
        encryptedPrefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { encryptedPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.flowOn(Dispatchers.IO)

    /**
     * Get access token (one-time read). Migrates from legacy plaintext DataStore on first call.
     */
    suspend fun getAccessToken(): String? {
        val secureToken = encryptedPrefs.getString(SECURE_KEY_ACCESS_TOKEN, null)
        if (secureToken != null) return secureToken

        // One-time migration: read plaintext token, move to encrypted storage, erase original
        val legacyToken = dataStore.data.map { it[KEY_ACCESS_TOKEN_LEGACY] }.first()
        if (legacyToken != null) {
            encryptedPrefs.edit().putString(SECURE_KEY_ACCESS_TOKEN, legacyToken).apply()
            dataStore.edit { it.remove(KEY_ACCESS_TOKEN_LEGACY) }
        }
        return legacyToken
    }

    /**
     * Clear access token (logout).
     */
    suspend fun clearAccessToken() {
        encryptedPrefs.edit().remove(SECURE_KEY_ACCESS_TOKEN).apply()
        dataStore.edit { it.remove(KEY_ACCESS_TOKEN_LEGACY) }
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
     * Save instance URL encrypted. Also removes any legacy plaintext entry.
     */
    suspend fun saveInstanceUrl(instanceUrl: String) {
        encryptedPrefs.edit().putString(SECURE_KEY_INSTANCE_URL, instanceUrl).apply()
        dataStore.edit { it.remove(KEY_INSTANCE_URL_LEGACY) }
    }

    /**
     * Observe instance URL as Flow (backed by EncryptedSharedPreferences).
     */
    val instanceUrl: Flow<String?> = callbackFlow {
        trySend(encryptedPrefs.getString(SECURE_KEY_INSTANCE_URL, null))
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SECURE_KEY_INSTANCE_URL) {
                trySend(encryptedPrefs.getString(SECURE_KEY_INSTANCE_URL, null))
            }
        }
        encryptedPrefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { encryptedPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.flowOn(Dispatchers.IO)

    /**
     * Get instance URL (one-time read). Migrates from legacy plaintext DataStore on first call.
     */
    suspend fun getInstanceUrl(): String? {
        val secureUrl = encryptedPrefs.getString(SECURE_KEY_INSTANCE_URL, null)
        if (secureUrl != null) return secureUrl

        // One-time migration: read plaintext URL, move to encrypted storage, erase original
        val legacyUrl = dataStore.data.map { it[KEY_INSTANCE_URL_LEGACY] }.first()
        if (legacyUrl != null) {
            encryptedPrefs.edit().putString(SECURE_KEY_INSTANCE_URL, legacyUrl).apply()
            dataStore.edit { it.remove(KEY_INSTANCE_URL_LEGACY) }
        }
        return legacyUrl
    }

    /**
     * Clear instance URL.
     */
    suspend fun clearInstanceUrl() {
        encryptedPrefs.edit().remove(SECURE_KEY_INSTANCE_URL).apply()
        dataStore.edit { it.remove(KEY_INSTANCE_URL_LEGACY) }
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

    // ========== Display Settings ==========

    /**
     * Save text size preference.
     */
    suspend fun saveTextSize(textSize: String) {
        dataStore.edit { preferences ->
            preferences[KEY_TEXT_SIZE] = textSize
        }
    }

    /**
     * Get text size preference as Flow.
     * Returns "MEDIUM" by default.
     */
    val textSize: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_TEXT_SIZE] ?: "MEDIUM"
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

    // ========== Database Cleanup ==========

    /**
     * Save database retention period in hours.
     */
    suspend fun saveDatabaseRetentionHours(hours: String) {
        dataStore.edit { preferences ->
            preferences[KEY_DATABASE_RETENTION_HOURS] = hours
        }
    }

    /**
     * Get database retention period in hours as Flow.
     * Returns "24" by default.
     */
    val databaseRetentionHours: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_DATABASE_RETENTION_HOURS] ?: "24"
    }

    /**
     * Get database retention period in hours (suspend function for one-time read).
     */
    suspend fun getDatabaseRetentionHours(): String {
        return databaseRetentionHours.first()
    }

    // ========== Push Notifications ==========

    /**
     * Save push configuration for a specific account.
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
        }.first()
    }

    /**
     * Delete push configuration for a specific account.
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
     * Clear all preferences and encrypted credentials (full app reset).
     */
    suspend fun clearAll() {
        encryptedPrefs.edit().clear().apply()
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Clear all authentication data (logout).
     */
    suspend fun clearAuthData() {
        encryptedPrefs.edit()
            .remove(SECURE_KEY_ACCESS_TOKEN)
            .remove(SECURE_KEY_INSTANCE_URL)
            .apply()
        dataStore.edit { preferences ->
            preferences.remove(KEY_ACCESS_TOKEN_LEGACY)
            preferences.remove(KEY_ACTIVE_ACCOUNT_ID)
            preferences.remove(KEY_INSTANCE_URL_LEGACY)
        }
    }

    /**
     * Save the ID of the last notification shown to the user.
     */
    suspend fun saveLastNotificationId(accountId: String, notificationId: String) {
        dataStore.edit { preferences ->
            preferences[lastNotificationIdKey(accountId)] = notificationId
        }
    }

    /**
     * Get the ID of the last notification shown to the user.
     */
    suspend fun getLastNotificationId(accountId: String): String? {
        return dataStore.data.first()[lastNotificationIdKey(accountId)]
    }
}
