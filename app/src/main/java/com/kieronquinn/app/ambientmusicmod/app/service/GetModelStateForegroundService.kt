package com.kieronquinn.app.ambientmusicmod.app.service

import android.app.*
import android.content.*
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.AmbientSharedPreferences
import com.kieronquinn.app.ambientmusicmod.model.recognition.RecognitionResponse
import com.kieronquinn.app.ambientmusicmod.model.recognition.RecognitionResult
import com.kieronquinn.app.ambientmusicmod.utils.AlarmTimeout
import com.kieronquinn.app.ambientmusicmod.utils.extensions.SecureBroadcastReceiver
import com.kieronquinn.app.ambientmusicmod.utils.extensions.sendSecureBroadcast
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class GetModelStateForegroundService: LifecycleService(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private const val NOTIFICATION_CHANNEL_FOREGROUND = "foreground_service"
        private const val ALARM_ID = "get_model_state"
        private const val NOTIFICATION_ID_FOREGROUND = 1001

        //Default automatic time to 60 seconds
        private const val DEFAULT_AUTOMATIC_TIME = 1000L * 60L

        //How long to give for the recognition response broadcast to come through
        private const val RECOGNITION_RESULT_DELAY = 10000L
    }

    private val settings by inject<AmbientSharedPreferences>()

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val alarmManager by lazy {
        getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val handler = Handler(Looper.getMainLooper())

    private val minuteTicker by lazy {
        AlarmTimeout(alarmManager, alarmListener, ALARM_ID, handler)
    }

    private val alarmListener = AlarmManager.OnAlarmListener {
        createTicker()
        sendBroadcast()
    }

    private val recognitionListener = SecureBroadcastReceiver { _, intent ->
        val recognitionResult = intent?.getParcelableExtra<RecognitionResult>(PixelAmbientServices.INTENT_RECOGNITION_RESULT_EXTRA_RESULT) ?: return@SecureBroadcastReceiver
        if(recognitionResult.retryTime != 0L){
            automaticTime = recognitionResult.retryTime
        }
    }

    private var jobTime = settings.jobTime

    private var automaticTime: Long? = null

    private val dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID_FOREGROUND, createNotification())
        if(jobTime == 0){
            stopSelf()
            return
        }
        settings.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        registerReceiver(recognitionListener, IntentFilter(PixelAmbientServices.INTENT_ACTION_RECOGNITION_RESULT))
        createTicker()
    }

    override fun onDestroy() {
        super.onDestroy()
        settings.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        try {
            unregisterReceiver(recognitionListener)
        }catch (e: IllegalArgumentException){
            //Not registered
        }
        stopTicker()
    }

    private fun createNotificationChannel() {
        notificationManager.createNotificationChannel(NotificationChannel(NOTIFICATION_CHANNEL_FOREGROUND, getString(R.string.foreground_service_notification_channel_title), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = getString(R.string.foreground_service_notification_channel_content)
        })
    }

    private fun createNotification(): Notification = Notification.Builder(this, NOTIFICATION_CHANNEL_FOREGROUND).apply {
        setContentTitle(getString(R.string.foreground_service_notification_title))
        setContentText(getString(R.string.foreground_service_notification_content))
        style = Notification.BigTextStyle().bigText(getString(R.string.foreground_service_notification_content))
        setSmallIcon(R.drawable.ic_music)
        setContentIntent(getClickIntent())
    }.build()

    private fun getClickIntent(): PendingIntent {
        val launchIntent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, NOTIFICATION_CHANNEL_FOREGROUND)
        }
        return PendingIntent.getActivity(this, 1, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createTicker() {
        lifecycleScope.launch {
            delay(RECOGNITION_RESULT_DELAY)
            if(!minuteTicker.isScheduled && jobTime != 0){
                val timeToAdd: Long = if(jobTime == -1){
                    //Automatic mode selected
                    automaticTime ?: DEFAULT_AUTOMATIC_TIME
                }else{
                    //Job time (in minutes) * 60 seconds - 10 seconds
                    jobTime * 1000L * 60L - 10000L
                }
                val currentTimeMillis = System.currentTimeMillis()
                val roundToNextTicker = currentTimeMillis + timeToAdd - System.currentTimeMillis()
                Log.d("GetModelState","Scheduled next tick for ${dateTimeFormatter.format(Instant.ofEpochMilli(roundToNextTicker + System.currentTimeMillis()).atZone(OffsetDateTime.now().offset))}")
                minuteTicker.schedule(roundToNextTicker, AlarmTimeout.MODE_IGNORE_IF_SCHEDULED)
                //Reset automatic time so it goes back to default if not updated
                automaticTime = null
            }
        }
    }

    private fun stopTicker() {
        if(minuteTicker.isScheduled){
            minuteTicker.cancel()
        }
    }

    private fun sendBroadcast() {
        sendSecureBroadcast(Intent(PixelAmbientServices.INTENT_ACTION_GET_MODEL_STATE).apply {
            `package` = PixelAmbientServices.PIXEL_AMBIENT_SERVICES_PACKAGE_NAME
        })
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if(key == AmbientSharedPreferences.KEY_JOB_TIME){
            // Time changed, we'll need to re-schedule the alarm
            jobTime = settings.jobTime
            stopTicker()
            if(jobTime == 0){
                //User has disabled automatic listening
                stopSelf()
                return
            }
            createTicker()
        }
    }

}