package com.kieronquinn.app.ambientmusicmod.repositories

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.AudioRecordingConfiguration
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.kieronquinn.app.ambientmusicmod.IMicrophoneDisabledStateCallback
import com.kieronquinn.app.ambientmusicmod.PACKAGE_NAME_GSB
import com.kieronquinn.app.ambientmusicmod.PACKAGE_NAME_PAM
import com.kieronquinn.app.ambientmusicmod.model.settings.BannerMessage
import com.kieronquinn.app.ambientmusicmod.model.settings.toLocalBannerMessage
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository.*
import com.kieronquinn.app.ambientmusicmod.utils.extensions.*
import com.kieronquinn.app.pixelambientmusic.model.LastRecognisedSong
import com.kieronquinn.app.pixelambientmusic.model.SettingsStateChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import com.kieronquinn.app.pixelambientmusic.model.SettingsState as RemoteSettingsState

interface RemoteSettingsRepository {

    companion object {
        const val INTENT_ACTION_REQUEST_PERMISSIONS =
            "com.kieronquinn.app.pixelambientmusic.REQUEST_PERMISSIONS"
    }

    data class NetworkCapability(val hasInternet: Boolean, val unmetered: Boolean)

    sealed class SettingsState {
        data class Available(
            val mainEnabled: Boolean,
            val onDemandEnabled: Boolean,
            val notificationsEnabled: Boolean,
            val bannerMessage: BannerMessage?,
            val lastRecognisedSong: LastRecognisedSong?,
            val onDemandCapable: Boolean
        ): SettingsState()
        object NoPAM: SettingsState()
        object NoShizuku: SettingsState()
        object NotSetup: SettingsState()
    }

    enum class GoogleAppState {
        /**
         *  The Google App & system are set up correctly to handle On Demand requests
         */
        SUPPORTED,

        /**
         *  The system is set up correctly, but the correct version of GSA needs installing
         */
        NEEDS_SPLIT,

        /**
         *  The system is not set up to handle On Demand, an overlay will be required
         */
        NEEDS_OVERLAY,

        /**
         *  The system cannot support On Demand
         */
        UNSUPPORTED
    }

    val permissionsGranted: Flow<Boolean>
    val googleAppSupported: Flow<GoogleAppState>

    fun getRemoteSettings(): Flow<SettingsState?>
    fun getRemoteSettings(lifecycleScope: CoroutineScope): StateFlow<SettingsState?>
    suspend fun commitChanges(settingsStateChange: SettingsStateChange)
    fun getOnDemandSupportedAndEnabled(): Flow<Boolean>

}

class RemoteSettingsRepositoryImpl(
    private val ambientServiceRepository: AmbientServiceRepository,
    private val shizukuServiceRepository: ShizukuServiceRepository,
    private val deviceConfigRepository: DeviceConfigRepository,
    private val batteryOptimisationRepository: BatteryOptimisationRepository,
    settingsRepository: SettingsRepository,
    recognitionRepository: RecognitionRepository,
    context: Context
): RemoteSettingsRepository {

    companion object {
        private const val SETTINGS_AUTHORITY = "com.google.android.as.pam.ambientmusic.settings"
        private val SETTINGS_URI = Uri.Builder().apply {
            scheme("content")
            authority(SETTINGS_AUTHORITY)
        }.build()
    }

    private val gson = Gson()

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val packageManager = context.packageManager

    private val latestRecognition = recognitionRepository.getLatestRecognition()

    private val isOnDemandConfigValueSet = context.isOnDemandConfigValueSet()

    private val hasSeenSetup = settingsRepository.hasSeenSetup

    override val permissionsGranted = flow {
        val requiredPermissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE
        )
        emit(context.packageManager.isPermissionGranted(PACKAGE_NAME_PAM, *requiredPermissions))
    }

    private val microphoneDisabled = callbackFlow {
        val callback = object: IMicrophoneDisabledStateCallback.Stub() {
            override fun onMicrophoneDisabledStateChanged(disabled: Boolean) {
                trySend(disabled)
            }
        }
        val callbackId = shizukuServiceRepository.runWithService {
            it.addMicrophoneDisabledListener(callback)
        }.unwrap()
        val isMicrophoneDisabled = shizukuServiceRepository.runWithService {
            it.isMicrophoneDisabled
        }.unwrap() ?: false
        trySend(isMicrophoneDisabled)
        awaitClose {
            callbackId?.let { id ->
                shizukuServiceRepository.runWithServiceIfAvailable {
                    it.removeMicrophoneDisabledListener(id)
                }
            }
        }
    }

    private val dndEnabled = callbackFlow {
        val disabledStates = arrayOf(
            NotificationManager.INTERRUPTION_FILTER_ALL,
            NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        )
        val isEnabled = {
            !disabledStates.contains(notificationManager.currentInterruptionFilter)
        }
        val receiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(isEnabled())
            }
        }
        trySend(isEnabled())
        context.registerReceiver(
            receiver, IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        )
        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    private val audioPlaying = callbackFlow {
        val callback = object: AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
                trySend(configs.isNotEmpty())
            }
        }
        trySend(audioManager.activePlaybackConfigurations.isNotEmpty())
        audioManager.registerAudioPlaybackCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose {
            audioManager.unregisterAudioPlaybackCallback(callback)
        }
    }

    private val audioRecording = callbackFlow {
        val callback = object: AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>) {
                trySend(configs.isNotEmpty())
            }
        }
        trySend(audioManager.activeRecordingConfigurations.isNotEmpty())
        audioManager.registerAudioRecordingCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose {
            audioManager.unregisterAudioRecordingCallback(callback)
        }
    }

    override suspend fun commitChanges(
        settingsStateChange: SettingsStateChange
    ) = withContext(Dispatchers.IO) {
        val service = ambientServiceRepository.getService()
        service?.updateSettingsState(settingsStateChange)
        Unit
    }

    private val ambientSettings = context.contentResolverAsTFlow(SETTINGS_URI) {
        val resolver = context.contentResolver
        resolver.safeQuery(
            SETTINGS_URI, null, null, null, null
        )?.map {
            gson.fromJson(it.getString(0), RemoteSettingsState::class.java)
        }?.firstOrNull()
    }.flowOn(Dispatchers.IO)

    private suspend fun isGoogleAppSupported(): GoogleAppState {
        //Requires Android 12+
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return GoogleAppState.UNSUPPORTED
        //Requires ARMv8
        if(isArmv7) return GoogleAppState.UNSUPPORTED
        //Requires the config value to point to it
        val isRoot = shizukuServiceRepository.runWithService { it.isRoot }.unwrap() ?: false
        if(!isOnDemandConfigValueSet && !isRoot) return GoogleAppState.NEEDS_OVERLAY
        val splits = packageManager.getSplits(PACKAGE_NAME_GSB)
        //The QSB app needs Sound Search to be installed, this has to be done manually
        if(!splits.contains("sound_search_fingerprinter_split")) return GoogleAppState.NEEDS_SPLIT
        return GoogleAppState.SUPPORTED
    }

    override val googleAppSupported = context.onPackageChanged(PACKAGE_NAME_GSB).map {
        isGoogleAppSupported()
    }

    private val secondarySettings = combine(
        dndEnabled,
        audioPlaying,
        audioRecording,
        microphoneDisabled
    ) { dnd, playback, recording, privacy ->
        when {
            //Privacy microphone disabling should be most visible
            privacy -> BannerMessage.MicrophoneDisabled
            //Recording & playback may confuse user so they come next
            recording -> BannerMessage.AppRecordingAudio
            playback -> BannerMessage.AppUsingDeviceAudio
            //DND doesn't prevent it working but may suppress the notification so it comes last
            dnd -> BannerMessage.DoNotDisturbEnabled
            //No blockers
            else -> null
        }
    }

    private val combinedSettings = combine(
        ambientSettings,
        secondarySettings,
        context.getNetworkCapabilities(),
        permissionsGranted,
        googleAppSupported
    ) { ambient, secondary, capabilities, granted, googleApp ->
        if(ambient == null) return@combine SettingsState.NoPAM
        var banner = ambient.bannerMessage?.toLocalBannerMessage() ?: secondary
        val onDemandCapable = googleApp == GoogleAppState.SUPPORTED
                && ambient.onDemandEnabled && capabilities.hasInternet
        //Battery Optimisation takes high priority
        if(batteryOptimisationRepository.getDisableBatteryOptimisationsIntent() != null){
            banner = BannerMessage.BatteryOptimisationsNeedDisabling
        }
        //Permissions take top priority
        when {
            !granted -> {
                banner = BannerMessage.PermissionsNeeded
            }
            banner == BannerMessage.Downloading -> {
                //If the remote is reporting that there's superpacks to download, make sure we can do
                if(!capabilities.hasInternet) {
                    //No internet, can't download
                    banner = BannerMessage.NoInternet
                }
                if(!capabilities.unmetered && deviceConfigRepository.superpacksRequireWiFi.get()){
                    //Metered connections won't be used to download
                    banner = BannerMessage.WaitingForUnmeteredInternet
                }
            }
            googleApp == GoogleAppState.NEEDS_SPLIT && ambient.onDemandEnabled -> {
                //The user has on demand enabled but the wrong GSB version, prompt to fix
                banner = BannerMessage.GoogleAppInvalid
            }
        }
        SettingsState.Available(
            ambient.mainEnabled,
            ambient.onDemandEnabled,
            ambient.notificationsEnabled,
            banner,
            ambient.lastRecognisedSong,
            onDemandCapable
        )
    }

    private val combinedSettingsAndRecognition = combine(
        combinedSettings,
        latestRecognition
    ) { settings, latest ->
        if(settings !is SettingsState.Available) return@combine settings
        shizukuServiceRepository.assertReady()
        SettingsState.Available(
            settings.mainEnabled,
            settings.onDemandEnabled,
            settings.notificationsEnabled,
            settings.bannerMessage,
            latest,
            settings.onDemandCapable
        )
    }

    override fun getRemoteSettings(): Flow<SettingsState?> {
        if(!hasSeenSetup.getSync()){
            return MutableStateFlow(SettingsState.NotSetup)
        }
        return shizukuServiceRepository.isReady.flatMapLatest {
            if(!it) MutableStateFlow(SettingsState.NoShizuku)
            else combinedSettingsAndRecognition
        }
    }

    override fun getRemoteSettings(lifecycleScope: CoroutineScope): StateFlow<SettingsState?> {
        return getRemoteSettings().stateIn(lifecycleScope, SharingStarted.Eagerly, null)
    }

    override fun getOnDemandSupportedAndEnabled() = combine(
        googleAppSupported,
        ambientSettings
    ) { supported, settings ->
        supported == GoogleAppState.SUPPORTED && settings?.onDemandEnabled == true
    }



}