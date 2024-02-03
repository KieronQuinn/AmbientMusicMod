package com.kieronquinn.app.ambientmusicmod.work

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.documentfile.provider.DocumentFile
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest.Companion.MIN_BACKOFF_MILLIS
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.notifications.NotificationChannel
import com.kieronquinn.app.ambientmusicmod.components.notifications.NotificationId
import com.kieronquinn.app.ambientmusicmod.components.notifications.createNotification
import com.kieronquinn.app.ambientmusicmod.repositories.BackupRestoreRepository
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository.LastBackup
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository.PeriodicBackupInterval
import com.kieronquinn.app.ambientmusicmod.ui.activities.MainActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class PeriodicBackupWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams), KoinComponent {

    companion object {
        private val TAG = PeriodicBackupWorker::class.java.simpleName
        private val URI_DEEP_LINK = Uri.parse("amm://backuprestore")
        private const val PERIODIC_BACKUP_NAME = "amm_backup_periodic.ammbkp"
        private const val PERIODIC_BACKUP_NAME_TMP = "amm_backup_periodic.ammbkp.tmp"

        fun enqueueOrCancelWorker(
            workManager: WorkManager,
            enabled: Boolean,
            interval: PeriodicBackupInterval
        ) {
            if(enabled){
                queueNextWorker(workManager, interval)
            }else{
                cancelNextWorker(workManager)
            }
        }

        fun getNextTime(interval: PeriodicBackupInterval): ZonedDateTime {
            val now = ZonedDateTime.now()
            return when(interval) {
                PeriodicBackupInterval.DAILY -> {
                    now.atThreeAm()
                }
                PeriodicBackupInterval.WEEKLY -> {
                    now.atThreeAm().atStartOfWeek()
                }
                PeriodicBackupInterval.MONTHLY -> {
                    now.atThreeAm().atStartOfMonth()
                }
            }
        }

        private fun queueNextWorker(workManager: WorkManager, interval: PeriodicBackupInterval) {
            val now = ZonedDateTime.now()
            val next = getNextTime(interval)
            val durationUntilNext = Duration.between(now, next)
            cancelNextWorker(workManager)
            val work = OneTimeWorkRequestBuilder<PeriodicBackupWorker>()
                .setInitialDelay(durationUntilNext)
                .addTag(TAG)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL, MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS
                )
                .build()
            workManager.enqueue(work)
        }

        private fun cancelNextWorker(workManager: WorkManager) {
            workManager.cancelAllWorkByTag(TAG)
        }

        private fun ZonedDateTime.atThreeAm(): ZonedDateTime {
            return withHour(3)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .plusDays(1)
        }

        private fun ZonedDateTime.atStartOfWeek(): ZonedDateTime {
            return minusDays(dayOfWeek.ordinal.toLong()).plusDays(7)
        }

        private fun ZonedDateTime.atStartOfMonth(): ZonedDateTime {
            return withDayOfMonth(1).plusMonths(1)
        }
    }

    private val backupRepository by inject<BackupRestoreRepository>()
    private val settingsRepository by inject<SettingsRepository>()

    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val launchIntent by lazy {
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = URI_DEEP_LINK
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    override fun doWork(): Result {
        context.handleBackup()
        val workManager = WorkManager.getInstance(context)
        enqueueOrCancelWorker(
            workManager,
            settingsRepository.periodicBackupEnabled.getSync(),
            settingsRepository.periodicBackupInterval.getSync()
        )
        return Result.success()
    }

    private fun Context.handleBackup() = GlobalScope.launch {
        if(!settingsRepository.periodicBackupEnabled.get()) return@launch
        createBackup().collect {
            when(it){
                is BackupRestoreRepository.BackupState.BackupComplete -> {
                    if (it.result == BackupRestoreRepository.BackupResult.SUCCESS) {
                        notificationManager.cancel(NotificationId.BACKUP.ordinal)
                        deleteTempFileIfExists()
                    } else {
                        restoreTempFileIfExists()
                        showErrorNotification(it.result)
                    }
                    settingsRepository.periodicBackupLastBackup.set(
                        LastBackup(System.currentTimeMillis(), it.result)
                    )
                }
                else -> {
                    showProgressNotification(it.title)
                }
            }
        }
    }

    private fun createBackup() = flow {
        val uri = getBackupUri()
        if(uri != null) {
            backupRepository.createBackup(uri).collect {
                emit(it)
            }
        }else{
            emit(BackupRestoreRepository.BackupState.BackupComplete(BackupRestoreRepository.BackupResult.FAILED_TO_WRITE))
        }
    }

    private suspend fun getBackupFolder(): DocumentFile? {
        val uri = settingsRepository.periodicBackupUri.get().takeIf { it.isNotBlank() }
            ?: return null
        return try {
            val folder = DocumentFile.fromTreeUri(context, Uri.parse(uri)) ?: return null
            folder.createRenamingIfExists("application/ammbkp", PERIODIC_BACKUP_NAME)
        }catch (e: Exception){
            null
        }
    }

    private suspend fun getBackupUri(): Uri? {
        return getBackupFolder()?.uri
    }

    private fun DocumentFile.createRenamingIfExists(
        mimeType: String, fileName: String
    ): DocumentFile? {
        findFile(fileName)?.renameTo(PERIODIC_BACKUP_NAME_TMP)
        return createFile(mimeType, fileName)
    }

    private suspend fun deleteTempFileIfExists() {
        getBackupFolder()?.findFile(PERIODIC_BACKUP_NAME)?.delete()
    }

    private suspend fun restoreTempFileIfExists() {
        getBackupFolder()?.findFile(PERIODIC_BACKUP_NAME_TMP)?.renameTo(PERIODIC_BACKUP_NAME)
    }

    private fun Context.showErrorNotification(
        backupResult: BackupRestoreRepository.BackupResult
    ) = createNotification(NotificationChannel.BACKUP) {
        it.setContentTitle(getString(backupResult.title))
        it.setContentText(getString(backupResult.content))
        it.setSmallIcon(R.drawable.ic_error_circle)
        it.setAutoCancel(true)
        it.setContentIntent(
            PendingIntent.getActivity(
                this,
                NotificationId.BACKUP.ordinal,
                launchIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        it.setTicker(getString(backupResult.title))
    }.let {
        notificationManager.notify(NotificationId.BACKUP.ordinal, it)
    }

    private fun Context.showProgressNotification(
        @StringRes
        subtitle: Int = R.string.backup_subtitle_generic
    ) = createNotification(NotificationChannel.BACKUP) {
        it.setContentTitle(getString(R.string.backup_title))
        it.setContentText(getString(subtitle))
        it.setSmallIcon(R.drawable.ic_notification)
        it.setOngoing(true)
        it.setProgress(0, 0, true)
        it.setContentIntent(
            PendingIntent.getActivity(
                this,
                NotificationId.BACKUP.ordinal,
                launchIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        it.setTicker(getString(R.string.backup_title))
    }.let {
        notificationManager.notify(NotificationId.BACKUP.ordinal, it)
    }


}