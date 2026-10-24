package com.pfitzer.tootsie.push

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.unifiedpush.android.connector.UnifiedPush
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for UnifiedPush registration and operations.
 * Handles registration with UnifiedPush distributors for decentralized push notifications.
 */
@Singleton
class UnifiedPushManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private companion object {
        // FEATURE_BYTES_MESSAGE allows receiving raw byte messages
        val FEATURES = arrayListOf(UnifiedPush.FEATURE_BYTES_MESSAGE)
    }

    /**
     * Register for push notifications with UnifiedPush.
     * Uses accountId as instance for multi-account support.
     *
     * @param accountId Account identifier to use as instance
     */
    fun registerForPush(accountId: String) {
        // Use accountId as instance for multi-account support
        // FEATURE_BYTES_MESSAGE allows receiving raw byte messages
        UnifiedPush.registerAppWithDialog(
            context,
            instance = accountId,
            features = FEATURES
        )
    }

    /**
     * Unregister from push notifications.
     *
     * @param accountId Account identifier used as instance
     */
    fun unregisterFromPush(accountId: String) {
        UnifiedPush.unregisterApp(context, instance = accountId)
    }

    /**
     * Check if a UnifiedPush distributor is available.
     * Returns true if any distributor (ntfy, NextPush, etc.) is installed.
     *
     * @return True if distributor is available
     */
    fun isDistributorAvailable(): Boolean {
        // Installed distributors, not the saved one: a distributor is only saved during the
        // first registration, so checking the saved one made enabling push impossible
        return UnifiedPush.getDistributors(context, FEATURES).isNotEmpty()
    }

    /**
     * Get the current UnifiedPush distributor package name.
     *
     * @return Distributor package name or empty string if none
     */
    fun getDistributor(): String {
        return UnifiedPush.getSavedDistributor(context) ?: ""
    }
}
