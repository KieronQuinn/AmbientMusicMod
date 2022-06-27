package com.kieronquinn.app.ambientmusicmod.work

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.notifications.NotificationChannel
import com.kieronquinn.app.ambientmusicmod.components.notifications.NotificationId
import com.kieronquinn.app.ambientmusicmod.components.notifications.createNotification
import com.kieronquinn.app.ambientmusicmod.repositories.DeviceConfigRepository
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.UpdatesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import java.time.LocalDateTime

class UpdateWorker(
    private val context: Context, workerParams: WorkerParameters
) : Worker(context, workerParams), KoinComponent {

    companion object {
        private const val TAG = "update_check"

        fun queueWorker(context: Context){
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag(TAG)
            val now = LocalDateTime.now()
            val updateTime = if(now.hour >= 12) {
                now.plusDays(1).withHour(12).withMinute(0).withSecond(0)
            }else{
                now.withHour(12).withMinute(0).withSecond(0)
            }
            val delay = Duration.between(now, updateTime)
            workManager.enqueue(OneTimeWorkRequest.Builder(UpdateWorker::class.java).apply {
                addTag(TAG)
                setInitialDelay(delay)
            }.build())
        }
    }

    private val updatesRepository by inject<UpdatesRepository>()
    private val shardsRepository by inject<ShardsRepository>()
    private val settingsRepository by inject<SettingsRepository>()
    private val deviceConfigRepository by inject<DeviceConfigRepository>()

    override fun doWork(): Result {
        GlobalScope.launch {
            checkForUpdates()
        }
        return Result.success()
    }

    private suspend fun checkForUpdates() = withContext(Dispatchers.IO) {
        val appUpdatesAvailable = updatesRepository.isAnyUpdateAvailable()
        val shardsUpdateAvailable = shardsRepository.isUpdateAvailable(true)
        if(appUpdatesAvailable || shardsUpdateAvailable){
            val shards = shardsRepository.getShardsState(true).first()
            if(shards.updateAvailable && shards.remote != null
                && settingsRepository.automaticMusicDatabaseUpdates.get()){
                //Automatically apply the shards update
                deviceConfigRepository.indexManifest.set(shards.remote.url)
                //Still show notification if others are available
                if(appUpdatesAvailable){
                    context.showUpdateNotification()
                }
            }else{
                context.showUpdateNotification()
            }
        }
        queueWorker(context)
    }

    private fun Context.showUpdateNotification() {
        val notificationIntent = Intent(Intent.ACTION_VIEW).apply {
            `package` = BuildConfig.APPLICATION_ID
            data = Uri.parse("amm://updates")
        }
        val notification = createNotification(NotificationChannel.UPDATES) {
            it.setContentTitle(getString(R.string.notification_update_title))
            it.setContentText(getString(R.string.notification_update_subtitle))
            it.setSmallIcon(R.drawable.ic_notification)
            it.setOngoing(false)
            it.setAutoCancel(true)
            it.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    NotificationId.UPDATES.ordinal,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            it.setTicker(getString(R.string.notification_update_title))
        }
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NotificationId.UPDATES.ordinal, notification)
    }

}