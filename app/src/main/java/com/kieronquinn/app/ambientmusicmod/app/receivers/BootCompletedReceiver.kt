package com.kieronquinn.app.ambientmusicmod.app.receivers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.service.GetModelStateForegroundService
import com.kieronquinn.app.ambientmusicmod.app.ui.activities.BootHelperLaunchActivity

class BootCompletedReceiver: BroadcastReceiver() {

    companion object {
        private const val NOTIFICATION_CHANNEL_BOOT_HELPER = "boot_helper"
        const val NOTIFICATION_ID_HELPER = 1002
    }

    override fun onReceive(context: Context, intent: Intent?) {
        context.startForegroundService(Intent(context, GetModelStateForegroundService::class.java))
        showHelperNotification(context)
    }

    private fun showHelperNotification(context: Context) = with(context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager, context)
        Notification.Builder(context, NOTIFICATION_CHANNEL_BOOT_HELPER).apply {
            setContentTitle(getString(R.string.foreground_service_notification_boot_title))
            setContentText(getString(R.string.foreground_service_notification_boot_content))
            style = Notification.BigTextStyle().bigText(getString(R.string.foreground_service_notification_boot_content))
            setSmallIcon(R.drawable.ic_music)
            setContentIntent(getClickIntent(context))
        }.build().also {
            notificationManager.notify(NOTIFICATION_ID_HELPER, it)
        }
    }

    private fun getClickIntent(context: Context): PendingIntent {
        return PendingIntent.getActivity(context, 1, Intent(context, BootHelperLaunchActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannel(notificationManager: NotificationManager, context: Context) = with(context) {
        notificationManager.createNotificationChannel(
            NotificationChannel(NOTIFICATION_CHANNEL_BOOT_HELPER, getString(R.string.foreground_service_notification_boot_channel_title), NotificationManager.IMPORTANCE_HIGH).apply {
            description = getString(R.string.foreground_service_notification_boot_channel_content)
        })
    }

}