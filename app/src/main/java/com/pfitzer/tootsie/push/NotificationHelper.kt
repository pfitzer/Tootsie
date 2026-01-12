package com.pfitzer.tootsie.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.pfitzer.tootsie.MainActivity
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Notification
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper for creating and displaying push notifications.
 * Manages notification channels and notification content.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    /**
     * Create notification channels for different notification types.
     * Channels allow users to customize notification behavior per type.
     */
    private fun createNotificationChannels() {
        val mentionsChannel = NotificationChannel(
            CHANNEL_MENTIONS,
            "Mentions",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications when someone mentions you"
        }

        val followsChannel = NotificationChannel(
            CHANNEL_FOLLOWS,
            "Follows",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications when someone follows you"
        }

        val boostsChannel = NotificationChannel(
            CHANNEL_BOOSTS,
            "Boosts & Favorites",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifications when someone boosts or favorites your post"
        }

        notificationManager.createNotificationChannels(
            listOf(mentionsChannel, followsChannel, boostsChannel)
        )
    }

    /**
     * Show notification from UnifiedPush payload.
     *
     * @param payload Decrypted push payload from Mastodon
     * @param accountId Account that received the notification
     */
    fun showPushNotification(payload: PushPayload, accountId: String) {
        val channelId = when (payload.notification_type) {
            "mention" -> CHANNEL_MENTIONS
            "follow" -> CHANNEL_FOLLOWS
            "favourite", "reblog" -> CHANNEL_BOOSTS
            else -> CHANNEL_MENTIONS
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_id", payload.notification_id)
            putExtra("account_id", accountId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            payload.notification_id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(payload.title ?: "New notification")
            .setContentText(payload.body ?: "")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(payload.notification_id?.hashCode() ?: 0, notification)
    }

    /**
     * Show notification from Notification domain model (for polling fallback).
     *
     * @param notification Notification from Mastodon API
     * @param accountId Account that received the notification
     */
    fun showNotification(notification: Notification, accountId: String) {
        val channelId = when (notification.type.apiValue) {
            "mention" -> CHANNEL_MENTIONS
            "follow" -> CHANNEL_FOLLOWS
            "favourite", "reblog" -> CHANNEL_BOOSTS
            else -> CHANNEL_MENTIONS
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_id", notification.id)
            putExtra("account_id", accountId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Format notification content based on type
        val (title, body) = formatNotificationContent(notification)

        val notificationObject = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notification.id.hashCode(), notificationObject)
    }

    /**
     * Format notification content based on notification type.
     */
    private fun formatNotificationContent(notification: Notification): Pair<String, String> {
        return when (notification.type.apiValue) {
            "mention" -> {
                val title = "${notification.account.displayName} mentioned you"
                val body = notification.status?.content?.take(100) ?: ""
                Pair(title, body)
            }
            "follow" -> {
                val title = "${notification.account.displayName} followed you"
                val body = "@${notification.account.username}"
                Pair(title, body)
            }
            "favourite" -> {
                val title = "${notification.account.displayName} favorited your post"
                val body = notification.status?.content?.take(100) ?: ""
                Pair(title, body)
            }
            "reblog" -> {
                val title = "${notification.account.displayName} boosted your post"
                val body = notification.status?.content?.take(100) ?: ""
                Pair(title, body)
            }
            else -> {
                val title = "New notification"
                val body = notification.type.apiValue
                Pair(title, body)
            }
        }
    }

    companion object {
        private const val CHANNEL_MENTIONS = "mentions"
        private const val CHANNEL_FOLLOWS = "follows"
        private const val CHANNEL_BOOSTS = "boosts"
    }
}
