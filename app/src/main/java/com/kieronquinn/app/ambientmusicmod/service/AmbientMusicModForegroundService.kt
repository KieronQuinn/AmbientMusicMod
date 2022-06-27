package com.kieronquinn.app.ambientmusicmod.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.text.TextPaint
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.PACKAGE_NAME_PAM
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.notifications.NotificationChannel
import com.kieronquinn.app.ambientmusicmod.components.notifications.NotificationId
import com.kieronquinn.app.ambientmusicmod.components.notifications.createNotification
import com.kieronquinn.app.ambientmusicmod.model.lockscreenoverlay.OverlayState
import com.kieronquinn.app.ambientmusicmod.model.lockscreenoverlay.stateEquals
import com.kieronquinn.app.ambientmusicmod.model.recognition.Player
import com.kieronquinn.app.ambientmusicmod.model.settings.BannerMessage
import com.kieronquinn.app.ambientmusicmod.repositories.*
import com.kieronquinn.app.ambientmusicmod.repositories.RecognitionRepository.RecognitionState
import com.kieronquinn.app.ambientmusicmod.repositories.RecognitionRepository.RecognitionState.ErrorReason
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository.SettingsState
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository.LockscreenOnTrackClicked
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository.RecognitionPeriod
import com.kieronquinn.app.ambientmusicmod.ui.activities.MainActivity
import com.kieronquinn.app.ambientmusicmod.utils.alarm.AlarmTimeout
import com.kieronquinn.app.ambientmusicmod.utils.alarm.AlarmTimeout.Companion.MODE_RESCHEDULE_IF_SCHEDULED
import com.kieronquinn.app.ambientmusicmod.utils.extensions.*
import com.kieronquinn.app.pixelambientmusic.model.RecognitionMetadata
import com.kieronquinn.app.pixelambientmusic.model.RecognitionSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.koin.android.ext.android.inject
import java.time.Duration
import java.time.LocalDateTime

class AmbientMusicModForegroundService: LifecycleService() {

    private val recognition by inject<RecognitionRepository>()
    private val settings by inject<SettingsRepository>()
    private val deviceConfig by inject<DeviceConfigRepository>()
    private val bedtime by inject<BedtimeRepository>()
    private val remoteSettings by inject<RemoteSettingsRepository>()
    private val accessibility by inject<AccessibilityRepository>()
    private val shizuku by inject<ShizukuServiceRepository>()
    private val widgetRepository by inject<WidgetRepository>()
    private var overlayTimeoutJob: Job? = null

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        private const val TAG = "AMMFS"
        private const val ALARM_ID = "ambient_musid_mod"
        private const val ON_DEMAND_FAILED_TIMEOUT = 30_000L
        private const val ON_DEMAND_SEARCH_TIMEOUT = 20_000L
        private const val ACTION_RECOGNITION_RETRY =
            "${BuildConfig.APPLICATION_ID}.action.RECOGNITION_RETRY"
        private var MESSAGE_HANDLER: Handler? = null

        fun start(context: Context, restart: Boolean = false) {
            val intent = Intent(context, AmbientMusicModForegroundService::class.java)
            if (restart) {
                context.stopService(intent)
            }
            context.startForegroundService(intent)
        }

        fun sendManualRecognition(state: RecognitionState.Recognised){
            MESSAGE_HANDLER?.let {
                it.sendMessage(Message.obtain(it, MessageType.RECOGNITION.ordinal, state))
            }
        }

        fun sendImmediateTrigger() {
            MESSAGE_HANDLER?.let {
                it.sendMessage(Message.obtain(it, MessageType.TRIGGER_IMMEDIATE.ordinal))
            }
        }

        enum class MessageType {
            RECOGNITION, TRIGGER_IMMEDIATE
        }
    }

    private val delayTime = combine(
        settings.recognitionPeriod.asFlow(),
        settings.recognitionBuffer.asFlow()
    ) { time, buffer ->
        time.period + buffer.time
    }.stateIn(lifecycleScope, SharingStarted.Eagerly, null)

    private val remoteSettingsState = remoteSettings.getRemoteSettings(lifecycleScope)
        .filterNotNull()

    private val batterySaverGating = combine(
        batterySaverEnabled(),
        settings.runOnBatterySaver.asFlow()
    ) { saver, enabled ->
        saver && enabled
    }

    private val isBedtime = bedtime.isBedtime()
        .stateIn(lifecycleScope, SharingStarted.Eagerly, null)

    private val enabled = combine(
        remoteSettingsState,
        batterySaverGating,
        isBedtime.filterNotNull(),
        settings.hasSeenSetup.asFlow(), //Only start after setup
    ) { remote, batterySaver, bedtime, hasSeenSetup ->
        remote is SettingsState.Available && remote.mainEnabled && !batterySaver && !bedtime && hasSeenSetup
    }.distinctUntilChanged()

    private val recognitionState = MutableSharedFlow<RecognitionState?>()

    private val ownerInfoState = combine(
        settings.lockscreenOwnerInfo.asFlow(),
        settings.lockscreenOwnerInfoFallback.asFlow(),
        settings.lockscreenOwnerInfoShowNote.asFlow(),
        recognitionState.autoClearAfterBy {
            if(it is RecognitionState.Recognised) it.metadata.getDelayTime() else null
        }
    ) { enabled, fallback, showNote, state ->
        if(!enabled) return@combine null
        if(state !is RecognitionState.Recognised) return@combine fallback
        val textPaint = TextPaint()
        val width = resources.getDimension(R.dimen.overlay_text_max_width_half)
        val concatTitle = state.recognitionResult.trackName.ellipsizeToSize(textPaint, width)
        val concatArtist = state.recognitionResult.artist.ellipsizeToSize(textPaint, width)
        val text = resources.getString(R.string.lockscreen_overlay_song, concatTitle, concatArtist)
        if(showNote){
            resources.getString(R.string.lockscreen_owner_info_note, text)
        }else text
    }

    private val onDemandLockscreenEnabled = combine(
        remoteSettingsState,
        settings.onDemandLockscreenEnabled.asFlow()
    ) { remote, onDemandEnabled ->
        if(remote !is SettingsState.Available || !remote.onDemandEnabled || !remote.onDemandCapable)
            return@combine false
        onDemandEnabled
    }

    /**
     *  Triggered when GSA updates and On Demand is enabled, but no longer supported.
     *  Drops initial emission to only report changes.
     */
    private val onDemandWarningNotification = remoteSettings.getRemoteSettings(lifecycleScope)
        .mapNotNull {
            if(it !is SettingsState.Available) return@mapNotNull null
            it.onDemandEnabled && it.bannerMessage is BannerMessage.GoogleAppInvalid
        }.drop(1)

    private val retryActionClicked = broadcastReceiverAsFlow(ACTION_RECOGNITION_RETRY).map {
        it.verifySecurity()
    }

    private val screenOnTrigger = combine(
        broadcastReceiverAsFlow(Intent.ACTION_SCREEN_ON),
        settings.triggerWhenScreenOn.asFlow()
    ) { _, enabled ->
        if(enabled) Unit else null
    }.filterNotNull()

    private val overlayState = combine(
        recognitionState,
        accessibility.enabled,
        settings.lockscreenOverlayStyle.asFlow(),
        settings.lockscreenOverlayYPos.asFlow(),
        onDemandLockscreenEnabled
    ) { state, overlayEnabled, style, yPos, onDemandEnabled ->
        overlayTimeoutJob?.cancel()
        if(!overlayEnabled) return@combine OverlayState.Hidden
        val clearTo = if(onDemandEnabled){
            OverlayState.IconOnly(
                style,
                yPos,
                R.drawable.ic_nowplaying_ondemand,
                ::onOverlayOnDemandClicked
            )
        }else OverlayState.Hidden
        when(state){
            is RecognitionState.Recording -> {
                if(state.source == RecognitionSource.ON_DEMAND) {
                    OverlayState.Shown(
                        style,
                        yPos,
                        R.drawable.ic_nowplaying_ondemand,
                        getString(R.string.lockscreen_overlay_ondemand_searching),
                        null,
                        System.currentTimeMillis() + ON_DEMAND_SEARCH_TIMEOUT
                    ){}
                }else null //Don't clear during recordings for screen on's sake
            }
            is RecognitionState.Recognising -> {
                if(state.source == RecognitionSource.ON_DEMAND) {
                    OverlayState.Shown(
                        style,
                        yPos,
                        R.drawable.ic_nowplaying_ondemand,
                        getString(R.string.lockscreen_overlay_ondemand_searching),
                        null,
                        System.currentTimeMillis() + ON_DEMAND_SEARCH_TIMEOUT
                    ){}
                }else null //Don't clear during recognising for screen on's sake
            }
            is RecognitionState.Recognised -> {
                val recognitionTime = state.metadata?.recognitionTime ?: System.currentTimeMillis()
                val endTime = state.metadata.getDelayTime() + recognitionTime
                OverlayState.Shown(
                    style,
                    yPos,
                    style.icon,
                    state.recognitionResult.trackName,
                    state.recognitionResult.artist,
                    endTime
                ) { onOverlayTrackClicked(state) }
            }
            is RecognitionState.Failed -> {
                if(state.recognitionFailure.source == RecognitionSource.ON_DEMAND){
                    OverlayState.Shown(
                        style,
                        yPos,
                        R.drawable.audioanim_no_music,
                        getString(R.string.lockscreen_overlay_ondemand_no_result),
                        null,
                        System.currentTimeMillis() + ON_DEMAND_FAILED_TIMEOUT,
                        ::onOverlayOnDemandClicked
                    )
                }else clearTo
            }
            null -> null //Don't clear on null for screen on's sake
            else -> clearTo
        }.also {
            if(it is OverlayState.Shown) {
                setupTimeout(it.endTime, clearTo)
            }
        }
    }.filterNotNull().distinctUntilChanged { old, new -> old.stateEquals(new) }

    private val recognitionDelay = recognitionState.mapLatest {
        if(it == null){
            //Start immediate recognition when required (eg. on start), but skip if auto is disabled
            return@mapLatest calculateNextRecognitionTime(null, 0).also {
                log("Skipping check, using time $it")
            }
        }
        when(it){
            is RecognitionState.Recognised -> { calculateNextRecognitionTime(it.metadata) }
            is RecognitionState.Error -> { calculateNextRecognitionTime(null) }
            is RecognitionState.Failed -> { calculateNextRecognitionTime(null) }
            else -> null
        }
    }.filterNotNull().onEach {
        log("Recognition delay: $it, next trigger time ${LocalDateTime.now().plusNanos(Duration.ofMillis(it).toNanos())}")
    }

    private val bufferTime = settings.recognitionBuffer.asFlow()
        .stateIn(lifecycleScope, SharingStarted.Eagerly, null)

    private val loggingEnabled = deviceConfig.enableLogging.asFlow()
        .stateIn(lifecycleScope, SharingStarted.Eagerly, deviceConfig.enableLogging.getSync())

    private val minuteTicker by lazy {
        AlarmTimeout(alarmManager, alarmListener, ALARM_ID, handler)
    }

    private val tickerFlow = MutableSharedFlow<Unit>()

    private val alarmManager by lazy {
        getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val handler = Handler(Looper.getMainLooper())

    private val alarmListener = AlarmManager.OnAlarmListener {
        lifecycleScope.launchWhenCreated {
            tickerFlow.emit(Unit)
        }
    }

    private val messageHandler = object: Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            lifecycleScope.launchWhenCreated {
                when(MessageType.values().firstOrNull { it.ordinal == msg.what }) {
                    MessageType.RECOGNITION -> {
                        val state = msg.obj as RecognitionState.Recognised
                        recognitionState.emit(state)
                    }
                    MessageType.TRIGGER_IMMEDIATE -> {
                        log("Triggering, delay = ${settings.recognitionPeriod.get()}")
                        recognitionState.emit(null)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        MESSAGE_HANDLER = messageHandler
        startForeground(NotificationId.FOREGROUND_SERVICE.ordinal, showNotification())
        setupRecogniser()
        setupOverlay()
        setupScreenOn()
        setupToggle()
        setupBedtime()
        setupWidget()
        setupOwnerInfo()
        setupErrorNotification()
        setupErrorNotificationRetry()
        setupOnDemandNotification()
        setupAlarm()
        lifecycleScope.launchWhenCreated {
            //Start the recognition flow
            recognitionState.emit(null)
        }
    }

    override fun onDestroy() {
        MESSAGE_HANDLER = null
        super.onDestroy()
    }

    private fun setupRecogniser() = lifecycleScope.launchWhenCreated {
        tickerFlow.flatMapLatest {
            if(!enabled.firstNotNull()) return@flatMapLatest MutableStateFlow(null)
            recognition.requestRecognition()
        }.filterNotNull().collect {
            log("Recognition state: $it")
            recognitionState.emit(it)
        }
    }

    private fun showNotification(): Notification {
        val notificationIntent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, NotificationChannel.FOREGROUND_SERVICE.id)
        }
        val notification = createNotification(NotificationChannel.FOREGROUND_SERVICE) {
            it.setContentTitle(getString(R.string.notification_service_foreground_title))
            it.setContentText(getString(R.string.notification_service_foreground_subtitle))
            it.setSmallIcon(R.drawable.ic_notification)
            it.setOngoing(true)
            it.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    NotificationId.FOREGROUND_SERVICE.ordinal,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            it.setTicker(getString(R.string.notification_service_foreground_title))
        }
        notificationManager.notify(NotificationId.FOREGROUND_SERVICE.ordinal, notification)
        return notification
    }

    private fun setupTimeout(timeoutAt: Long, clearTo: OverlayState) {
        overlayTimeoutJob = lifecycleScope.launchWhenCreated {
            val now = System.currentTimeMillis()
            val delay = if(timeoutAt > now){
                timeoutAt - now
            }else 0
            delay(delay)
            LockscreenOverlayAccessibilityService.sendState(clearTo)
        }
    }

    private fun setupOverlay() = lifecycleScope.launchWhenCreated {
        overlayState.collect { state ->
            log(state.toString())
            LockscreenOverlayAccessibilityService.sendState(state)
        }
    }

    private fun setupScreenOn() = lifecycleScope.launchWhenCreated {
        screenOnTrigger.collect {
            recognitionState.emit(null)
        }
    }

    private fun setupToggle() = lifecycleScope.launchWhenCreated {
        enabled.drop(1).collect {
            if(it){
                //Trigger an immediate recognition
                recognitionState.emit(null)
            } //Disabled will be caught in the start recognition and not re-scheduled
        }
    }

    private fun setupWidget() = lifecycleScope.launchWhenCreated {
        recognitionState.collect {
            widgetRepository.notifyRecognitionState(it)
        }
    }

    private fun setupBedtime() = lifecycleScope.launchWhenCreated {
        bedtime.checkTimeAndSyncWorkers()
    }

    private fun setupOwnerInfo() = lifecycleScope.launchWhenCreated {
        ownerInfoState.filterNotNull().collectLatest { info ->
            shizuku.runWithService { it.setOwnerInfo(info) }
        }
    }

    private fun setupAlarm() = lifecycleScope.launchWhenCreated {
        recognitionDelay.collect {
            alarmManager.cancel(alarmListener)
            minuteTicker.schedule(it, MODE_RESCHEDULE_IF_SCHEDULED)
        }
    }

    private fun setupErrorNotification() = lifecycleScope.launchWhenCreated {
        recognitionState.collect {
            if(it is RecognitionState.Error){
                showErrorNotification(it)
            }else if(it is RecognitionState.Recognised || it is RecognitionState.Failed){
                //Clear error notifications that may exist already
                notificationManager.cancel(NotificationId.ERRORS.ordinal)
            }
        }
    }

    private fun showErrorNotification(error: RecognitionState.Error) {
        val launchIntent = Intent(this, MainActivity::class.java)
        val title = when(error.errorReason) {
            ErrorReason.SHIZUKU_ERROR -> R.string.notification_error_shizuku_title
            ErrorReason.TIMEOUT -> return
            ErrorReason.API_INCOMPATIBLE -> R.string.notification_error_api_title
            ErrorReason.NEEDS_ROOT -> R.string.notification_error_needs_root_title
            ErrorReason.DISABLED -> return
        }
        val subtitle = when(error.errorReason){
            ErrorReason.SHIZUKU_ERROR -> R.string.notification_error_shizuku_subtitle
            ErrorReason.TIMEOUT -> return
            ErrorReason.API_INCOMPATIBLE -> R.string.notification_error_api_subtitle
            ErrorReason.NEEDS_ROOT -> R.string.notification_error_needs_root_subtitle
            ErrorReason.DISABLED -> return
        }
        val retryIntent = PendingIntent.getBroadcast(
            this, 1002, Intent(ACTION_RECOGNITION_RETRY).apply {
                `package` = BuildConfig.APPLICATION_ID
                applySecurity(this@AmbientMusicModForegroundService)
            }, PendingIntent.FLAG_IMMUTABLE
        )
        val notification = createNotification(NotificationChannel.ERRORS) {
            it.setContentTitle(getString(title))
            it.setContentText(getString(subtitle))
            it.setSmallIcon(R.drawable.ic_notification)
            it.setOngoing(false)
            it.setAutoCancel(true)
            it.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    NotificationId.ERRORS.ordinal,
                    launchIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            it.setTicker(getString(title))
            it.addAction(0, getString(R.string.notification_error_button), retryIntent)
        }
        notificationManager.notify(NotificationId.ERRORS.ordinal, notification)
    }

    private fun setupErrorNotificationRetry() = lifecycleScope.launchWhenCreated {
        retryActionClicked.collect {
            notificationManager.cancel(NotificationId.ERRORS.ordinal)
            sendImmediateTrigger()
        }
    }

    private fun setupOnDemandNotification()  = lifecycleScope.launchWhenCreated {
        onDemandWarningNotification.collect {
            if(it) {
                showOnDemandNotification()
            }else{
                notificationManager.cancel(NotificationId.WARNINGS.ordinal)
            }
        }
    }

    private fun showOnDemandNotification() {
        val launchIntent = Intent(this, MainActivity::class.java)
        val notification = createNotification(NotificationChannel.WARNINGS) {
            it.setContentTitle(getString(R.string.notification_warning_on_demand_disabled_title))
            it.setContentText(getString(R.string.notification_warning_on_demand_disabled_subtitle))
            it.setSmallIcon(R.drawable.ic_notification)
            it.setOngoing(false)
            it.setAutoCancel(true)
            it.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    NotificationId.WARNINGS.ordinal,
                    launchIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            it.setTicker(getString(R.string.notification_warning_on_demand_disabled_title))
        }
        notificationManager.notify(NotificationId.WARNINGS.ordinal, notification)
    }

    private suspend fun RecognitionMetadata?.getDelayTime(): Long {
        return this?.remainingTime?.let {
            it + bufferTime.firstNotNull().time
        } ?: delayTime.firstNotNull()
    }

    private fun onOverlayTrackClicked(state: RecognitionState.Recognised) = lifecycleScope.launchWhenCreated {
        when(settings.lockscreenOverlayClicked.get()) {
            LockscreenOnTrackClicked.ASSISTANT -> {
                val intent = Player.Assistant(
                    state.recognitionResult.googleId ?: return@launchWhenCreated
                ).getIntent().apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
            LockscreenOnTrackClicked.HISTORY -> {
                val intent = Intent("com.google.intelligence.sense.NOW_PLAYING_HISTORY").apply {
                    `package` = PACKAGE_NAME_PAM
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                unlockAndRun(getString(R.string.lockscreen_unlock_message_history)) {
                    startActivity(intent)
                }
            }
            LockscreenOnTrackClicked.NOTHING -> {
                //No-op
            }
        }
    }

    private fun unlockAndRun(message: String, block: () -> Unit) = lifecycleScope.launchWhenCreated {
        shizuku.runWithService {
            it.dismissKeyguard(block, message)
        }
    }

    private fun onOverlayOnDemandClicked() = lifecycleScope.launchWhenCreated {
        recognition.requestOnDemandRecognition().collect {
            recognitionState.emit(it)
        }
    }

    private suspend fun calculateNextRecognitionTime(
        metadata: RecognitionMetadata?,
        timeOverride: Long? = null
    ): Long? {
        val period = settings.recognitionPeriod.get()
        if(period == RecognitionPeriod.NEVER) return null
        if(timeOverride != null) return timeOverride
        val buffer = settings.recognitionBuffer.get()
        val adaptive = settings.recognitionPeriodAdaptive.get()
        return when {
            !adaptive -> period.period + buffer.time
            metadata != null -> metadata.remainingTime + buffer.time
            else -> period.period + buffer.time
        }
    }

    private fun log(value: String) {
        if(!loggingEnabled.value) return
        Log.d(TAG, value)
    }

}