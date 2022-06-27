package com.kieronquinn.app.ambientmusicmod.repositories

import android.content.Context
import android.content.SharedPreferences
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.repositories.BaseSettingsRepository.AmbientMusicModSetting
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

interface DeviceConfigRepository {

    val cacheShardEnabled: AmbientMusicModSetting<Boolean>
    val indexManifest: AmbientMusicModSetting<String>
    val runOnSmallCores: AmbientMusicModSetting<Boolean>
    val nnfpv3Enabled: AmbientMusicModSetting<Boolean>
    val onDemandVibrateEnabled: AmbientMusicModSetting<Boolean>
    val deviceCountry: AmbientMusicModSetting<String>
    val superpacksRequireCharging: AmbientMusicModSetting<Boolean>
    val superpacksRequireWiFi: AmbientMusicModSetting<Boolean>

    val recordingGain: AmbientMusicModSetting<Float>
    val showAlbumArt: AmbientMusicModSetting<Boolean>
    val enableLogging: AmbientMusicModSetting<Boolean>
    val alternativeEncoding: AmbientMusicModSetting<Boolean>

    fun getAllDeviceConfigValues(): List<Pair<String, String>>
    suspend fun sendValues()

}

/**
 *  Device Config store for values to override the values in ASI. Contains both static values and
 *  dynamic ones, which are stored in [sharedPreferences]. Values are retrieved from ASI, via
 *  SettingsProvider.
 *
 *  Call onConfigChanged(keys) in IRecognitionService to tell ASI that values with the given keys
 *  have changed.
 */
class DeviceConfigRepositoryImpl(
    private val context: Context,
    private val serviceRepository: AmbientServiceRepository
): BaseSettingsRepositoryImpl(), DeviceConfigRepository {

    companion object {
        private const val CACHE_SHARD_ENABLED = "NowPlaying__cache_shard_enabled"
        private const val DEFAULT_CACHE_SHARD_ENABLED = true

        private const val INDEX_MANIFEST = "NowPlaying__ambient_music_index_manifest_17_09_02"
        private const val DEFAULT_INDEX_MANIFEST = BuildConfig.DEFAULT_MANIFEST

        private const val RUN_ON_SMALL_CORES = "NowPlaying__ambient_music_run_on_small_cores"
        private const val DEFAULT_RUN_ON_SMALL_CORES = false

        private const val NNFP_V3_MODEL_ENABLED = "NowPlaying__nnfp_v3_model_enabled"
        private const val DEFAULT_NNFP_V3_MODEL_ENABLED = false

        private const val ON_DEMAND_VIBRATE_ENABLED = "NowPlaying__on_demand_vibration_enabled"
        private const val DEFAULT_ON_DEMAND_VIBRATE_ENABLED = true

        private const val SUPERPACKS_REQUIRE_CHARGING = "Superpacks__require_charging_by_default"
        private const val DEFAULT_SUPERPACKS_REQUIRE_CHARGING = false

        private const val SUPERPACKS_REQUIRE_WIFI = "Superpacks__require_wifi_by_default"
        private const val DEFAULT_SUPERPACKS_REQUIRE_WIFI = true

        private const val DEVICE_COUNTRY = "NowPlaying__device_country"
        private const val DEFAULT_DEVICE_COUNTRY = "" //Empty = automatic

        //This is custom but allows for easy cached access from ASI
        private const val RECORDING_GAIN = "NowPlaying__recording_gain"
        const val DEFAULT_RECORDING_GAIN = 1.0f

        //Also custom, the built in album art setting has side effects
        private const val SHOW_ALBUM_ART = "NowPlaying__show_album_art"
        private const val SHOW_ALBUM_ART_DEFAULT = true

        //Custom to allow enabling/disabling verbose logging
        private const val ENABLE_LOGGING = "NowPlaying__enable_logging"
        private const val ENABLE_LOGGING_DEFAULT = false

        //Custom, enables BIG_ENDIAN encoding to fix crackle on some devices
        private const val ALTERNATIVE_ENCODING = "NowPlaying__alternative_audio_encoding"
        private const val ALTERNATIVE_ENCODING_DEFAULT = false

        private val KEY_MAP = mapOf(
            CACHE_SHARD_ENABLED to DeviceConfigRepositoryImpl::cacheShardEnabled,
            INDEX_MANIFEST to DeviceConfigRepositoryImpl::indexManifest,
            RUN_ON_SMALL_CORES to DeviceConfigRepositoryImpl::runOnSmallCores,
            NNFP_V3_MODEL_ENABLED to DeviceConfigRepositoryImpl::nnfpv3Enabled,
            ON_DEMAND_VIBRATE_ENABLED to DeviceConfigRepositoryImpl::onDemandVibrateEnabled,
            DEVICE_COUNTRY to DeviceConfigRepositoryImpl::deviceCountry,
            SUPERPACKS_REQUIRE_CHARGING to DeviceConfigRepositoryImpl::superpacksRequireCharging,
            SUPERPACKS_REQUIRE_WIFI to DeviceConfigRepositoryImpl::superpacksRequireWiFi,
            RECORDING_GAIN to DeviceConfigRepositoryImpl::recordingGain,
            SHOW_ALBUM_ART to DeviceConfigRepositoryImpl::showAlbumArt,
            ENABLE_LOGGING to DeviceConfigRepositoryImpl::enableLogging,
            ALTERNATIVE_ENCODING to DeviceConfigRepositoryImpl::alternativeEncoding
        )

        /**
         *  Device Config values that should always be set, and are not updatable from the UI
         */
        private val STATIC_DEVICE_CONFIG = mapOf(
            "NowPlaying__ambient_music_on_demand_enabled" to "true",
            "NowPlaying__ambient_music_on_demand_music_confidence" to "0.48",
            "NowPlaying__cloud_api_allowed" to "true",
            "NowPlaying__enable_usage_fa" to "false", //FA is disabled w/o .oss
            "NowPlaying__favorites_enabled" to "true",
            "NowPlaying__feature_users_count_enabled" to "true",
            "NowPlaying__federated_analytics_allowed" to "false", // Disabled without .oss
            "NowPlaying__handle_ambient_music_results_with_history" to "true",
            "NowPlaying__min_training_interval_millis" to "86400000",
            "NowPlaying__on_demand_enable_eager_prompt" to "false",
            "NowPlaying__on_demand_fingerprinter_being_setup_warning" to "true",
            "NowPlaying__on_demand_hide_if_fingerprinter_install_not_confirmed" to "true",
            "NowPlaying__on_demand_min_supported_aga_version" to "12.35.17",
            "NowPlaying__on_demand_retry_fingerprinter_install" to "true",
            "NowPlaying__youtube_export_enabled" to "true",
            "NowPlaying__ambient_music_hide_old_results_when_recognition_fails" to "false",
            "StatsLog__enabled" to "false" //Disable StatsLog as it's useless to us and crashes A10
        )
    }

    private val onChange = MutableSharedFlow<String>()

    override val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("${BuildConfig.APPLICATION_ID}_device_config", Context.MODE_PRIVATE)
    }

    override val cacheShardEnabled = boolean(
        CACHE_SHARD_ENABLED, DEFAULT_CACHE_SHARD_ENABLED, onChange
    )

    override val indexManifest = string(
        INDEX_MANIFEST, DEFAULT_INDEX_MANIFEST, onChange
    )

    override val runOnSmallCores = boolean(
        RUN_ON_SMALL_CORES, DEFAULT_RUN_ON_SMALL_CORES, onChange
    )

    override val nnfpv3Enabled = boolean(
        NNFP_V3_MODEL_ENABLED, DEFAULT_NNFP_V3_MODEL_ENABLED, onChange
    )

    override val onDemandVibrateEnabled = boolean(
        ON_DEMAND_VIBRATE_ENABLED, DEFAULT_ON_DEMAND_VIBRATE_ENABLED, onChange
    )

    override val deviceCountry = string(
        DEVICE_COUNTRY, DEFAULT_DEVICE_COUNTRY, onChange
    )

    override val superpacksRequireCharging = boolean(
        SUPERPACKS_REQUIRE_CHARGING, DEFAULT_SUPERPACKS_REQUIRE_CHARGING, onChange
    )

    override val superpacksRequireWiFi = boolean(
        SUPERPACKS_REQUIRE_WIFI, DEFAULT_SUPERPACKS_REQUIRE_WIFI, onChange
    )

    override val recordingGain = float(
        RECORDING_GAIN, DEFAULT_RECORDING_GAIN, onChange
    )

    override val showAlbumArt = boolean(
        SHOW_ALBUM_ART, SHOW_ALBUM_ART_DEFAULT, onChange
    )

    override val enableLogging = boolean(
        ENABLE_LOGGING, ENABLE_LOGGING_DEFAULT, onChange
    )

    override val alternativeEncoding = boolean(
        ALTERNATIVE_ENCODING, ALTERNATIVE_ENCODING_DEFAULT, onChange
    )

    override fun getAllDeviceConfigValues(): List<Pair<String, String>> {
        val staticValues = STATIC_DEVICE_CONFIG.map {
            Pair(it.key, it.value)
        }
        val dynamicValues = KEY_MAP.map {
            Pair(it.key, it.value.get(this).getSync().toString())
        }
        return staticValues + dynamicValues
    }

    private fun sendInitial() = GlobalScope.launch {
        sendValues()
    }

    private fun setupChange() = GlobalScope.launch {
        onChange.collect {
            serviceRepository.getService()?.onConfigChanged(listOf(it))
        }
    }

    override suspend fun sendValues() {
        serviceRepository.getService()?.onConfigChanged(KEY_MAP.keys.toList())
    }

    init {
        sendInitial()
        setupChange()
    }

}