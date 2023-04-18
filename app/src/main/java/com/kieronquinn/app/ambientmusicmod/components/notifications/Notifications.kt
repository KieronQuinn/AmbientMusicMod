package com.kieronquinn.app.ambientmusicmod.components.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.kieronquinn.app.ambientmusicmod.R
import android.app.NotificationChannel as AndroidNotificationChannel

fun Context.createNotification(
    channel: NotificationChannel,
    builder: (NotificationCompat.Builder) -> Unit
): Notification {
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notificationChannel =
        AndroidNotificationChannel(
            channel.id,
            getString(channel.titleRes),
            channel.importance
        ).apply {
            description = getString(channel.descRes)
        }
    notificationManager.createNotificationChannel(notificationChannel)
    return NotificationCompat.Builder(this, channel.id).apply(builder).build()
}

enum class NotificationChannel(
    val id: String,
    val importance: Int,
    val titleRes: Int,
    val descRes: Int
) {
    FOREGROUND_SERVICE (
        "foreground_service",
        NotificationManager.IMPORTANCE_LOW,
        R.string.notification_channel_service_foreground_title,
        R.string.notification_channel_service_foreground_subtitle
    ),
    UPDATES (
        "updates",
        NotificationManager.IMPORTANCE_HIGH,
        R.string.notification_channel_updates_title,
        R.string.notification_channel_updates_subtitle
    ),
    ERRORS (
        "errors",
        NotificationManager.IMPORTANCE_HIGH,
        R.string.notification_channel_errors_title,
        R.string.notification_channel_errors_subtitle
    ),
    WARNINGS (
        "warnings",
        NotificationManager.IMPORTANCE_HIGH,
        R.string.notification_channel_warnings_title,
        R.string.notification_channel_warnings_subtitle
    ),
    BACKUP (
        "backup",
        NotificationManager.IMPORTANCE_DEFAULT,
        R.string.notification_channel_backups_title,
        R.string.notification_channel_backups_subtitle
    )
}

enum class NotificationId {
    BUFFER, //Foreground ID cannot be 0
    FOREGROUND_SERVICE,
    UPDATES,
    ERRORS,
    WARNINGS,
    BACKUP
}