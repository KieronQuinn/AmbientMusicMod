package com.kieronquinn.app.ambientmusicmod.components

import com.kieronquinn.app.ambientmusicmod.xposed.wrappers.SoundTriggerManager

abstract class AmbientSharedPreferences: BaseSharedPreferences() {

    companion object {

        const val INTENT_ACTION_SETTINGS_CHANGED = "com.google.intelligence.sense.ACTION_SETTINGS_CHANGED"

        val KEY_ENABLED = "enabled"
        val DEFAULT_ENABLED = true

        val KEY_RECORD_GAIN = "record_gain"
        val DEFAULT_RECORD_GAIN = 50f

        val KEY_JOB_TIME = "job_time"
        val DEFAULT_JOB_TIME = 0

        val KEY_RUN_WHEN_WOKEN = "run_when_woken"
        val DEFAULT_RUN_WHEN_WOKEN = true

        val KEY_DEVELOPER_MODE_ENABLED = "developer_mode_enabled"
        val DEFAULT_DEVELOPER_MODE_ENABLED = false

        val KEY_RUN_ON_LITTLE_CORES = "run_on_little_cores"
        val DEFAULT_RUN_ON_LITTLE_CORES = false

        val KEY_SHOW_ALBUM_ART = "show_album_art"
        val DEFAULT_SHOW_ALBUM_ART = true

        val KEY_USE_ASSISTANT_FOR_CLICK = "use_assistant_for_click"
        val DEFAULT_USE_ASSISTANT_FOR_CLICK = true

        val KEY_LOCK_SCREEN_OVERLAY_ENABLED = "lock_screen_overlay_enabled"
        val DEFAULT_LOCK_SCREEN_OVERLAY_ENABLED = false

        val KEY_LOCK_SCREEN_OVERLAY_LAUNCH_CLICK_ENABLED = "lock_screen_overlay_launch_click_enabled"
        val DEFAULT_LOCK_SCREEN_OVERLAY_LAUNCH_CLICK_ENABLED = true

        val KEY_DEVELOPER_ENABLE_LOGGING = "developer_enable_logging"
        val DEFAULT_DEVELOPER_ENABLE_LOGGING = false

        val KEY_OVERLAY_POSITION_X = "overlay_position_x"
        val DEFAULT_OVERLAY_POSITION_X = -1f

        val KEY_OVERLAY_POSITION_Y = "overlay_position_y"
        val DEFAULT_OVERLAY_POSITION_Y = -1f

        val KEY_SOUND_TRIGGER_GET_MODEL_SUPPORTED = "soundtrigger_get_model_supported"
        val DEFAULT_SOUND_TRIGGER_GET_MODEL_SUPPORTED = GetModelSupported.UNKNOWN

        val KEY_SOUND_TRIGGER_GET_MODEL_LAST_RESULT = "soundtrigger_get_model_last_result"
        val DEFAULT_SOUND_TRIGGER_GET_MODEL_LAST_RESULT = SoundTriggerManager.STATUS_NOT_RUN

    }

    var enabled by this.shared(KEY_ENABLED, DEFAULT_ENABLED)
    var recordGain by this.shared(KEY_RECORD_GAIN, DEFAULT_RECORD_GAIN)
    var jobTime by this.shared(KEY_JOB_TIME, DEFAULT_JOB_TIME)
    var runWhenWoken by this.shared(KEY_RUN_WHEN_WOKEN, DEFAULT_RUN_WHEN_WOKEN)
    var developerModeEnabled by this.shared(KEY_DEVELOPER_MODE_ENABLED, DEFAULT_DEVELOPER_MODE_ENABLED)
    var runOnLittleCores by this.shared(KEY_RUN_ON_LITTLE_CORES, DEFAULT_RUN_ON_LITTLE_CORES)
    var showAlbumArt by this.shared(KEY_SHOW_ALBUM_ART, DEFAULT_SHOW_ALBUM_ART)
    var useAssistantForClick by this.shared(KEY_USE_ASSISTANT_FOR_CLICK, DEFAULT_USE_ASSISTANT_FOR_CLICK)
    var lockScreenOverlayEnabled by this.shared(KEY_LOCK_SCREEN_OVERLAY_ENABLED, DEFAULT_LOCK_SCREEN_OVERLAY_ENABLED)
    var lockScreenOverlayLaunchClick by this.shared(KEY_LOCK_SCREEN_OVERLAY_LAUNCH_CLICK_ENABLED, DEFAULT_LOCK_SCREEN_OVERLAY_LAUNCH_CLICK_ENABLED)
    var overlayPositionX by this.shared(KEY_OVERLAY_POSITION_X, DEFAULT_OVERLAY_POSITION_X)
    var overlayPositionY by this.shared(KEY_OVERLAY_POSITION_Y, DEFAULT_OVERLAY_POSITION_Y)

    //Whether getModelState is supported
    var getModelSupported by this.shared(KEY_SOUND_TRIGGER_GET_MODEL_SUPPORTED, DEFAULT_SOUND_TRIGGER_GET_MODEL_SUPPORTED)
    var getModelLastResult by this.shared(KEY_SOUND_TRIGGER_GET_MODEL_LAST_RESULT, DEFAULT_SOUND_TRIGGER_GET_MODEL_LAST_RESULT)

    //Developer options
    var developerEnableLogging by this.shared(KEY_DEVELOPER_ENABLE_LOGGING, DEFAULT_DEVELOPER_ENABLE_LOGGING)

    //Phenotypes
    var phenotype_hideResultsWhenRecognitionFails by this.shared("phenotype_ambient_music_hide_old_results_when_recognition_fails", true)
    var phenotype_smartAudioPlaybackDetection by this.shared("phenotype_ambient_music_smart_audio_playback_detection", true)
    var phenotype_highQualityResampling by this.shared("phenotype_ambient_music_high_quality_resampling", true)
    var phenotype_useDspAudioSource by this.shared("phenotype_ambient_music_use_dsp_audio_source", true)
    var phenotype_suppressDuringAudioRecording by this.shared("phenotype_ambient_music_suppress_recognition_during_audio_recording", true)

    enum class GetModelSupported {
        UNKNOWN,
        SUPPORTED,
        UNSUPPORTED
    }

}