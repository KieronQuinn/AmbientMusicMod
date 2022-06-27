package com.kieronquinn.app.ambientmusicmod.repositories

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.model.lockscreenoverlay.LockscreenOverlayStyle
import com.kieronquinn.app.ambientmusicmod.repositories.BaseSettingsRepository.AmbientMusicModSetting
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository.*
import java.time.Duration

interface SettingsRepository {

    val hasSeenSetup: AmbientMusicModSetting<Boolean>

    val recognitionPeriod: AmbientMusicModSetting<RecognitionPeriod>
    val recognitionPeriodAdaptive: AmbientMusicModSetting<Boolean>
    val recognitionBuffer: AmbientMusicModSetting<RecognitionBuffer>

    val lockscreenOverlayEnhanced: AmbientMusicModSetting<Boolean>
    val lockscreenOverlayStyle: AmbientMusicModSetting<LockscreenOverlayStyle>
    val lockscreenOverlayYPos: AmbientMusicModSetting<Int>
    val lockscreenOverlayClicked: AmbientMusicModSetting<LockscreenOnTrackClicked>
    val lockscreenOverlayColour: AmbientMusicModSetting<OverlayTextColour>
    val lockscreenOverlayCustomColour: AmbientMusicModSetting<Int>

    val lockscreenOwnerInfo: AmbientMusicModSetting<Boolean>
    val lockscreenOwnerInfoShowNote: AmbientMusicModSetting<Boolean>
    val lockscreenOwnerInfoFallback: AmbientMusicModSetting<String>

    val onDemandLockscreenEnabled: AmbientMusicModSetting<Boolean>

    val triggerWhenScreenOn: AmbientMusicModSetting<Boolean>
    val runOnBatterySaver: AmbientMusicModSetting<Boolean>
    val bedtimeModeEnabled: AmbientMusicModSetting<Boolean>
    val bedtimeModeStart: AmbientMusicModSetting<Long>
    val bedtimeModeEnd: AmbientMusicModSetting<Long>

    val automaticMusicDatabaseUpdates: AmbientMusicModSetting<Boolean>

    //The wallpaper colour to use (< Android 12)
    val monetColor: AmbientMusicModSetting<Int>

    enum class LockscreenOnTrackClicked(@StringRes val title: Int, @StringRes val content: Int) {
        ASSISTANT(
            R.string.lockscreen_on_track_clicked_assistant_title,
            R.string.lockscreen_on_track_clicked_assistant_content
        ),
        HISTORY(
            R.string.lockscreen_on_track_clicked_history_title,
            R.string.lockscreen_on_track_clicked_history_content
        ),
        NOTHING(
            R.string.lockscreen_on_track_clicked_nothing_title,
            R.string.lockscreen_on_track_clicked_nothing_content
        )
    }

    enum class RecognitionPeriod(
        val period: Long,
        @StringRes val title: Int,
        @StringRes val content: Int? = null
    ) {
        NEVER(
            0L,
            R.string.settings_recognition_period_never,
            R.string.settings_recognition_period_never_content
        ),
        SECONDS_30(
            30_000L,
            R.string.settings_recognition_period_30_seconds,
            R.string.settings_recognition_period_30_seconds_content
        ),
        MINUTES_1(
            60_000L,
            R.string.settings_recognition_period_1_minute
        ),
        MINUTES_2(
            120_000L,
            R.string.settings_recognition_period_2_minutes
        ),
        MINUTES_3(
            180_000L,
            R.string.settings_recognition_period_3_minutes
        ),
        MINUTES_5(
            240_000L,
            R.string.settings_recognition_period_5_minutes,
            R.string.settings_recognition_period_5_minutes_content
        )
    }

    enum class RecognitionBuffer(val time: Long, @StringRes val title: Int) {
        SECONDS_0(0L, R.string.settings_recognition_buffer_0_seconds),
        SECONDS_5(5_000L, R.string.settings_recognition_buffer_5_seconds),
        SECONDS_10(10_000L, R.string.settings_recognition_buffer_10_seconds),
        SECONDS_20(20_000L, R.string.settings_recognition_buffer_20_seconds),
        SECONDS_30(30_000L, R.string.settings_recognition_buffer_30_seconds)
    }

    enum class OverlayTextColour(
        @StringRes val title: Int,
        @StringRes val content: Int
    ) {
        AUTOMATIC(
            R.string.lockscreen_overlay_text_colour_automatic_title,
            R.string.lockscreen_overlay_text_colour_automatic_content
        ),
        BLACK(
            R.string.lockscreen_overlay_text_colour_black_title,
            R.string.lockscreen_overlay_text_colour_black_content
        ),
        WHITE(
            R.string.lockscreen_overlay_text_colour_white_title,
            R.string.lockscreen_overlay_text_colour_white_content
        ),
        CUSTOM(
            R.string.lockscreen_overlay_text_colour_custom_title,
            R.string.lockscreen_overlay_text_colour_custom_content
        )
    }

}

class SettingsRepositoryImpl(
    private val context: Context
): BaseSettingsRepositoryImpl(), SettingsRepository {

    companion object {
        private const val HAS_SEEN_SETUP = "has_seen_setup"
        private const val DEFAULT_HAS_SEEN_SETUP = false

        private const val RECOGNITION_PERIOD = "recognition_period"
        private val DEFAULT_RECOGNITION_PERIOD = RecognitionPeriod.MINUTES_1

        private const val RECOGNITION_PERIOD_ADAPTIVE = "recognition_period_adaptive"
        private const val DEFAULT_RECOGNITION_PERIOD_ADAPTIVE = true

        private const val RECOGNITION_BUFFER = "recognition_buffer"
        private val DEFAULT_RECOGNITION_BUFFER = RecognitionBuffer.SECONDS_10

        private const val LOCK_SCREEN_OVERLAY_ENHANCED = "lock_screen_overlay_enhanced"
        private const val DEFAULT_LOCK_SCREEN_OVERLAY_ENHANCED = false

        private const val LOCK_SCREEN_OVERLAY_STYLE = "lock_screen_overlay_style"
        private val DEFAULT_LOCK_SCREEN_OVERLAY_STYLE = LockscreenOverlayStyle.NEW

        private const val ON_DEMAND_LOCK_SCREEN_ENABLED = "on_demand_lock_screen_enabled"
        private const val DEFAULT_ON_DEMAND_LOCK_SCREEN_ENABLED = false

        private const val LOCK_SCREEN_OVERLAY_ON_TRACK_CLICKED = "lock_screen_overlay_on_track_clicked"
        private val DEFAULT_LOCK_SCREEN_OVERLAY_ON_TRACK_CLICKED = LockscreenOnTrackClicked.ASSISTANT

        private const val LOCK_SCREEN_OVERLAY_Y_POS = "lock_screen_overlay_y_pos"
        private const val DEFAULT_LOCK_SCREEN_Y_POS = 0

        private const val LOCK_SCREEN_OVERLAY_TEXT_COLOUR = "lock_screen_overlay_text_colour"
        private val DEFAULT_LOCK_SCREEN_OVERLAY_TEXT_COLOUR = OverlayTextColour.AUTOMATIC

        private const val LOCK_SCREEN_OVERLAY_CUSTOM_TEXT_COLOUR = "lock_screen_overlay_custom_text_colour"

        private const val LOCK_SCREEN_OWNER_INFO = "lock_screen_owner_info"
        private const val DEFAULT_LOCK_SCREEN_OWNER_INFO = false

        private const val LOCK_SCREEN_OWNER_INFO_SHOW_NOTE = "lock_screen_owner_info_show_note"
        private const val DEFAULT_LOCK_SCREEN_OWNER_INFO_SHOW_NOTE = true

        private const val LOCK_SCREEN_OWNER_INFO_FALLBACK = "lock_screen_owner_info_fallback"
        private const val DEFAULT_LOCK_SCREEN_OWNER_INFO_FALLBACK = ""

        private const val TRIGGER_WHEN_SCREEN_ON = "trigger_when_screen_on"
        private const val DEFAULT_TRIGGER_WHEN_SCREEN_ON = false

        private const val RUN_ON_BATTERY_SAVER = "run_on_battery_saver"
        private const val DEFAULT_RUN_ON_BATTERY_SAVER = false

        private const val BEDTIME_MODE_ENABLED = "bedtime_mode"
        private const val DEFAULT_BEDTIME_MODE_ENABLED = false

        private const val BEDTIME_MODE_START = "bedtime_mode_start"
        private val DEFAULT_BEDTIME_MODE_START = Duration.ofHours(23).toMinutes()

        private const val BEDTIME_MODE_END = "bedtime_mode_end"
        private val DEFAULT_BEDTIME_MODE_END = Duration.ofHours(7).toMinutes()

        private const val AUTOMATIC_MUSIC_DATABASE_UPDATES = "automatic_music_database_updates"
        private const val DEFAULT_AUTOMATIC_MUSIC_DATABASE_UPDATES = false

        private const val KEY_MONET_COLOR = "monet_color"
    }

    override val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("${BuildConfig.APPLICATION_ID}_prefs", Context.MODE_PRIVATE)
    }

    override val hasSeenSetup = boolean(HAS_SEEN_SETUP, DEFAULT_HAS_SEEN_SETUP)

    override val recognitionPeriod = enum(RECOGNITION_PERIOD, DEFAULT_RECOGNITION_PERIOD)

    override val recognitionPeriodAdaptive = boolean(
        RECOGNITION_PERIOD_ADAPTIVE, DEFAULT_RECOGNITION_PERIOD_ADAPTIVE
    )

    override val recognitionBuffer = enum(RECOGNITION_BUFFER, DEFAULT_RECOGNITION_BUFFER)

    override val lockscreenOverlayEnhanced = boolean(
        LOCK_SCREEN_OVERLAY_ENHANCED, DEFAULT_LOCK_SCREEN_OVERLAY_ENHANCED
    )

    override val lockscreenOverlayStyle = enum(
        LOCK_SCREEN_OVERLAY_STYLE, DEFAULT_LOCK_SCREEN_OVERLAY_STYLE
    )

    override val lockscreenOverlayYPos = int(LOCK_SCREEN_OVERLAY_Y_POS, DEFAULT_LOCK_SCREEN_Y_POS)

    override val lockscreenOverlayClicked = enum(
        LOCK_SCREEN_OVERLAY_ON_TRACK_CLICKED, DEFAULT_LOCK_SCREEN_OVERLAY_ON_TRACK_CLICKED
    )

    override val lockscreenOverlayColour = enum(
        LOCK_SCREEN_OVERLAY_TEXT_COLOUR, DEFAULT_LOCK_SCREEN_OVERLAY_TEXT_COLOUR
    )

    override val lockscreenOverlayCustomColour = color(
        LOCK_SCREEN_OVERLAY_CUSTOM_TEXT_COLOUR, Integer.MAX_VALUE
    )

    override val lockscreenOwnerInfo = boolean(
        LOCK_SCREEN_OWNER_INFO, DEFAULT_LOCK_SCREEN_OWNER_INFO
    )

    override val lockscreenOwnerInfoShowNote = boolean(
        LOCK_SCREEN_OWNER_INFO_SHOW_NOTE, DEFAULT_LOCK_SCREEN_OWNER_INFO_SHOW_NOTE
    )

    override val lockscreenOwnerInfoFallback = string(
        LOCK_SCREEN_OWNER_INFO_FALLBACK, DEFAULT_LOCK_SCREEN_OWNER_INFO_FALLBACK
    )

    override val onDemandLockscreenEnabled = boolean(
        ON_DEMAND_LOCK_SCREEN_ENABLED, DEFAULT_ON_DEMAND_LOCK_SCREEN_ENABLED
    )

    override val triggerWhenScreenOn = boolean(
        TRIGGER_WHEN_SCREEN_ON, DEFAULT_TRIGGER_WHEN_SCREEN_ON
    )

    override val runOnBatterySaver = boolean(RUN_ON_BATTERY_SAVER, DEFAULT_RUN_ON_BATTERY_SAVER)

    override val bedtimeModeEnabled = boolean(BEDTIME_MODE_ENABLED, DEFAULT_BEDTIME_MODE_ENABLED)
    override val bedtimeModeStart = long(BEDTIME_MODE_START, DEFAULT_BEDTIME_MODE_START)
    override val bedtimeModeEnd = long(BEDTIME_MODE_END, DEFAULT_BEDTIME_MODE_END)

    override val automaticMusicDatabaseUpdates = boolean(
        AUTOMATIC_MUSIC_DATABASE_UPDATES, DEFAULT_AUTOMATIC_MUSIC_DATABASE_UPDATES
    )

    override val monetColor = color(
        KEY_MONET_COLOR, Integer.MAX_VALUE
    )

}