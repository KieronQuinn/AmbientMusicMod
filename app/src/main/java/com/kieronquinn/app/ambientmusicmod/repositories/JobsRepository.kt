package com.kieronquinn.app.ambientmusicmod.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import com.kieronquinn.app.ambientmusicmod.utils.extensions.contentReceiverAsFlow
import com.kieronquinn.app.ambientmusicmod.utils.extensions.map
import com.kieronquinn.app.ambientmusicmod.utils.extensions.safeQuery
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

interface JobsRepository {

    fun setShouldExpediteJobs(shouldExpedite: Boolean)
    fun forceExpediteJobs()

}

class JobsRepositoryImpl(
    private val shizukuServiceRepository: ShizukuServiceRepository,
    private val deviceConfigRepository: DeviceConfigRepository,
    context: Context
): JobsRepository {

    companion object {
        private const val TAG = "AmbientJobs"
        private const val AUTHORITY = "com.google.android.as.pam.ambientmusic.leveldbprovider"
        private const val PATH_JOB_SCHEDULED = "jobscheduled"
        private const val PATH_JOBS = "jobs"

        private val URI_JOB_SCHEDULED = Uri.Builder().apply {
            scheme("content")
            authority(AUTHORITY)
            path(PATH_JOB_SCHEDULED)
        }.build()

        private val URI_JOBS = Uri.Builder().apply {
            scheme("content")
            authority(AUTHORITY)
            path(PATH_JOBS)
        }.build()
    }

    private val scope = MainScope()
    private var shouldExpedite = false
    private val contentResolver = context.contentResolver

    private val jobAdded = context.contentReceiverAsFlow(URI_JOB_SCHEDULED)

    override fun setShouldExpediteJobs(shouldExpedite: Boolean) {
        this.shouldExpedite = shouldExpedite
        if(shouldExpedite){
            expediteJobsNow()
        }
    }

    override fun forceExpediteJobs() {
        expediteJobsNow(true)
    }

    private fun expediteJobsNow(force: Boolean = false) = scope.launch {
        val jobs = getScheduledJobs()
        if(deviceConfigRepository.enableLogging.get()){
            Log.d(TAG, "Got ${jobs.size} jobs to expedite")
        }
        if(jobs.isEmpty()) return@launch
        shizukuServiceRepository.runWithService {
            it.expediteJobs(jobs.toIntArray(), force)
        }
    }

    private fun setupJobAddedListener() = scope.launch {
        jobAdded.filter { shouldExpedite }.debounce(1000L).collect {
            expediteJobsNow()
        }
    }

    private fun getScheduledJobs(): List<Int> {
        val cursor = contentResolver.safeQuery(
            URI_JOBS, null, null, null, null
        ) ?: return emptyList()
        return cursor.map {
            it.getInt(0)
        }.also {
            cursor.close()
        }
    }

    init {
        setupJobAddedListener()
    }

}