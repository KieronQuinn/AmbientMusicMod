package com.kieronquinn.app.ambientmusicmod.repositories

import android.content.Context
import android.text.format.DateFormat
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.kieronquinn.app.ambientmusicmod.work.BedtimeWorker
import kotlinx.coroutines.flow.*
import java.time.*
import java.util.*

interface BedtimeRepository {

    /**
     *  Update the time to check against - should be triggered on service start & from WorkManager
     *  workers that are set for the start and end times
     */
    suspend fun checkTimeAndSyncWorkers()

    fun isBedtime(): Flow<Boolean>

    fun isEnabled(): Flow<Boolean>
    fun getStartTime(): Flow<LocalTime>
    fun getEndTime(): Flow<LocalTime>

    fun getFormattedTime(time: LocalTime): String

    suspend fun setEnabled(enabled: Boolean)
    suspend fun setStartTime(time: Long)
    suspend fun setEndTime(time: Long)

}

class BedtimeRepositoryImpl(
    settingsRepository: SettingsRepository,
    private val context: Context
): BedtimeRepository {

    companion object {
        private const val WORKER_TAG_BEDTIME_START = "bedtime_start"
        private const val WORKER_TAG_BEDTIME_END = "bedtime_end"
    }

    private val time = MutableStateFlow(LocalTime.now())
    private val workManager = WorkManager.getInstance(context)

    private val bedtimeEnabled = settingsRepository.bedtimeModeEnabled
    private val bedtimeStart = settingsRepository.bedtimeModeStart
    private val bedtimeEnd = settingsRepository.bedtimeModeEnd

    private val dateFormat
        get() = DateFormat.getTimeFormat(context)

    override fun isEnabled() = bedtimeEnabled.asFlow()

    override fun getStartTime() = bedtimeStart.asFlow().map {
        LocalTime.MIN.plusMinutes(it)
    }

    override fun getEndTime() = bedtimeEnd.asFlow().map {
        LocalTime.MIN.plusMinutes(it)
    }

    override suspend fun setEnabled(enabled: Boolean) {
        bedtimeEnabled.set(enabled)
    }

    override suspend fun setStartTime(time: Long) {
        bedtimeStart.set(time)
    }

    override suspend fun setEndTime(time: Long) {
        bedtimeEnd.set(time)
    }

    override fun getFormattedTime(time: LocalTime): String {
        return dateFormat.format(time.toDate())
    }

    override fun isBedtime() = combine(
        time,
        isEnabled(),
        bedtimeStart.asFlow(),
        bedtimeEnd.asFlow()
    ) { time, enabled, start, end ->
        if(!enabled) return@combine false
        val currentMinute = time.toMinutes()
        if(start > end) {
            //Bedtime is over midnight, so check for later than start or earlier than end
            currentMinute >= start || currentMinute < end
        }else{
            //Bedtime is not over midnight, so check for any time between start and end
            currentMinute in start until end
        }
    }.onStart {
        checkTime()
    }

    private suspend fun checkTime() {
        time.emit(LocalTime.now())
    }

    override suspend fun checkTimeAndSyncWorkers() {
        checkTime()
        workManager.cancelAllWorkByTag(WORKER_TAG_BEDTIME_START)
        workManager.cancelAllWorkByTag(WORKER_TAG_BEDTIME_END)
        if(!isEnabled().first()) return
        val now = LocalTime.now()
        val startDelay = now.getTimeUntil(getStartTime().first())
        val endDelay = now.getTimeUntil(getEndTime().first())
        workManager.enqueue(OneTimeWorkRequest.Builder(BedtimeWorker::class.java).apply {
            addTag(WORKER_TAG_BEDTIME_START)
            setInitialDelay(startDelay)
        }.build())
        workManager.enqueue(OneTimeWorkRequest.Builder(BedtimeWorker::class.java).apply {
            addTag(WORKER_TAG_BEDTIME_END)
            setInitialDelay(endDelay)
        }.build())
    }

    private fun LocalTime.getTimeUntil(time: LocalTime): Duration {
        val date = if(isBefore(time)){
            time.atDate(LocalDate.now())
        }else{
            time.atDate(LocalDate.now().plusDays(1))
        }
        return Duration.between(LocalDateTime.now(), date)
    }

    private fun LocalTime.toDate(): Date {
        return Date.from(atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant())
    }

    private fun LocalTime.toMinutes(): Long {
        return Duration.ofHours(hour.toLong()).plusMinutes(minute.toLong()).toMinutes()
    }

}