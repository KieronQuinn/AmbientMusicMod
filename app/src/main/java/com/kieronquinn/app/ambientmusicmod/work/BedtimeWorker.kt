package com.kieronquinn.app.ambientmusicmod.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kieronquinn.app.ambientmusicmod.repositories.BedtimeRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BedtimeWorker(
    context: Context, workerParams: WorkerParameters
) : Worker(context, workerParams), KoinComponent {

    private val bedtimeRepository by inject<BedtimeRepository>()

    override fun doWork(): Result {
        GlobalScope.launch {
            bedtimeRepository.checkTimeAndSyncWorkers()
        }
        return Result.success()
    }

}