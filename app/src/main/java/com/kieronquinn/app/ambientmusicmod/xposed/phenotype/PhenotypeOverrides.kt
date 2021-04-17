package com.kieronquinn.app.ambientmusicmod.xposed.phenotype

import android.util.Log
import com.kieronquinn.app.ambientmusicmod.components.AmbientSharedPreferences
import com.kieronquinn.app.ambientmusicmod.xposed.debug.XLog

class PhenotypeOverrides(private val settings: AmbientSharedPreferences) {

    private val staticPhenotypeOverrides = mapOf(
        "ambient_music_apk_music_detector_min_score" to 0.0f,
        "ambient_music_run_apk_music_detector" to true,
        "ambient_music_use_dsp_audio_source" to true,
        "now_playing_allowed" to true,
        "ambient_music_enable_history" to true,
        "ambient_music_get_model_state_enabled" to true,
        //Play Store handling no longer works
        "handle_ambient_music_results_with_play_store" to false,
        //Always fallback to search (makes Assistant option the toggle as order is Assistant > Play > Search)
        "ambient_music_handle_results_with_search" to true
    )

    private var dynamicPhenotypeOverrides: Map<String, Any> = emptyMap()

    fun getOverridenPhenotype(key: String): Any? {
        return staticPhenotypeOverrides[key] ?: dynamicPhenotypeOverrides[key]
    }

    fun refreshDynamicPhenotypeOverrides(){
        dynamicPhenotypeOverrides = mapOf(
            "ambient_music_run_on_small_cores" to settings.runOnLittleCores,
            "handle_ambient_music_results_with_assistant" to settings.useAssistantForClick,
            "ambient_music_show_history_album_art" to settings.showAlbumArt,
            "phenotype_ambient_music_hide_old_results_when_recognition_fails" to settings.phenotype_hideResultsWhenRecognitionFails,
            "phenotype_ambient_music_smart_audio_playback_detection" to settings.phenotype_smartAudioPlaybackDetection,
            "phenotype_ambient_music_high_quality_resampling" to settings.phenotype_highQualityResampling,
            "phenotype_ambient_music_use_dsp_audio_source" to settings.phenotype_useDspAudioSource,
            "phenotype_ambient_music_suppress_recognition_during_audio_recording" to settings.phenotype_suppressDuringAudioRecording
        )
        XLog.d("Refreshed dynamic overrides ${dynamicPhenotypeOverrides.map { it.key + ": " + it.value }.joinToString(", ")}")
    }

}