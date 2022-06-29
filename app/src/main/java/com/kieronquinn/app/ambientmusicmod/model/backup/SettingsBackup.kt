package com.kieronquinn.app.ambientmusicmod.model.backup

import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.ambientmusicmod.model.lockscreenoverlay.LockscreenOverlayStyle
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository.*

data class SettingsBackup(
    //Remote Settings
    @SerializedName("main_enabled")
    val mainEnabled: Boolean,
    @SerializedName("on_demand_enabled")
    val onDemandEnabled: Boolean,
    //Local Settings
    @SerializedName("recognition_period")
    val recognitionPeriod: RecognitionPeriod?,
    @SerializedName("recognition_period_adaptive")
    val recognitionPeriodAdaptive: Boolean?,
    @SerializedName("recognition_buffer")
    val recognitionBuffer: RecognitionBuffer?,
    @SerializedName("lockscreen_overlay_enhanced")
    val lockscreenOverlayEnhanced: Boolean?,
    @SerializedName("lockscreen_overlay_style")
    val lockscreenOverlayStyle: LockscreenOverlayStyle?,
    @SerializedName("lockscreen_overlay_y_pos")
    val lockscreenOverlayYPos: Int?,
    @SerializedName("lockscreen_overlay_on_track_clicked")
    val lockscreenOnTrackClicked: LockscreenOnTrackClicked?,
    @SerializedName("lockscreen_owner_info")
    val lockscreenOwnerInfo: Boolean?,
    @SerializedName("lockscreen_owner_info_show_note")
    val lockscreenOwnerInfoShowNote: Boolean?,
    @SerializedName("lockscreen_owner_info_fallback")
    val lockscreenOwnerInfoFallback: String?,
    @SerializedName("on_demand_lockscreen_enabled")
    val onDemandLockscreenEnabled: Boolean?,
    @SerializedName("trigger_when_screen_on")
    val triggerWhenScreenOn: Boolean?,
    @SerializedName("run_on_battery_saver")
    val runOnBatterySaver: Boolean?,
    @SerializedName("bedtime_mode_enabled")
    val bedtimeModeEnabled: Boolean?,
    @SerializedName("bedtime_mode_start")
    val bedtimeModeStart: Long?,
    @SerializedName("bedtime_mode_end")
    val bedtimeModeEnd: Long?,
    @SerializedName("automatic_database_updates")
    val automaticDatabaseUpdates: Boolean?,
    //Device Config
    @SerializedName("cache_shard_enabled")
    val cacheShardEnabled: Boolean?,
    @SerializedName("run_on_small_cores")
    val runOnSmallCores: Boolean?,
    @SerializedName("nnfp_v3_enabled")
    val nnfpv3Enabled: Boolean?,
    @SerializedName("on_demand_vibrate_enabled")
    val onDemandVibrateEnabled: Boolean?,
    @SerializedName("device_country")
    val deviceCountry: String?,
    @SerializedName("superpacks_require_charging")
    val superpacksRequireCharging: Boolean?,
    @SerializedName("superpacks_require_wifi")
    val superpacksRequireWiFi: Boolean?,
    @SerializedName("recording_gain")
    val recordingGain: Float?,
    @SerializedName("show_album_art")
    val showAlbumArt: Boolean?,
    @SerializedName("overlay_text_colour")
    val overlayTextColour: OverlayTextColour?,
    @SerializedName("overlay_custom_text_colour")
    val overlayCustomTextColour: Int?,
    @SerializedName("alternative_encoding")
    val alternativeEncoding: Boolean?
)