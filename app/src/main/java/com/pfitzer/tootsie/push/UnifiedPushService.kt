package com.pfitzer.tootsie.push

import android.content.Context
import org.unifiedpush.android.connector.MessagingReceiver

/**
 * UnifiedPush MessagingReceiver for handling push notification events.
 * Receives push messages and registration events from UnifiedPush distributors.
 */
class UnifiedPushService : MessagingReceiver() {

    /**
     * Called when a new endpoint is registered.
     * This happens after successful registration with a UnifiedPush distributor.
     *
     * @param context Application context
     * @param endpoint Push endpoint URL from the distributor
     * @param instance Instance identifier (we use accountId for multi-account support)
     */
    override fun onNewEndpoint(context: Context, endpoint: String, instance: String) {
        // Instance = accountId for multi-account support
        val handler = context.applicationContext as? UnifiedPushHandler
        handler?.handleNewEndpoint(endpoint, instance)
    }

    /**
     * Called when a push message is received.
     *
     * @param context Application context
     * @param message Raw message bytes (may be encrypted)
     * @param instance Instance identifier (accountId)
     */
    override fun onMessage(context: Context, message: ByteArray, instance: String) {
        // Decrypt and display notification
        val handler = context.applicationContext as? UnifiedPushHandler
        handler?.handleMessage(message, instance)
    }

    /**
     * Called when registration with UnifiedPush fails.
     * This triggers fallback to WorkManager polling.
     *
     * @param context Application context
     * @param instance Instance identifier (accountId)
     */
    override fun onRegistrationFailed(context: Context, instance: String) {
        // Fall back to WorkManager polling
        val handler = context.applicationContext as? UnifiedPushHandler
        handler?.handleRegistrationFailed(instance)
    }

    /**
     * Called when the app is unregistered from UnifiedPush.
     *
     * @param context Application context
     * @param instance Instance identifier (accountId)
     */
    override fun onUnregistered(context: Context, instance: String) {
        // Clean up subscription
        val handler = context.applicationContext as? UnifiedPushHandler
        handler?.handleUnregistered(instance)
    }
}

/**
 * Interface for handling UnifiedPush events.
 * Should be implemented by TootsieApplication.
 */
interface UnifiedPushHandler {
    /**
     * Handle new endpoint registration.
     * Should subscribe to Mastodon push API with the endpoint.
     */
    fun handleNewEndpoint(endpoint: String, accountId: String)

    /**
     * Handle incoming push message.
     * Should decrypt and display notification.
     */
    fun handleMessage(message: ByteArray, accountId: String)

    /**
     * Handle registration failure.
     * Should fall back to WorkManager polling.
     */
    fun handleRegistrationFailed(accountId: String)

    /**
     * Handle unregistration.
     * Should clean up push subscription.
     */
    fun handleUnregistered(accountId: String)
}
