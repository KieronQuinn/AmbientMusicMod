package com.kieronquinn.app.ambientmusicmod.model.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kieronquinn.app.ambientmusicmod.PACKAGE_NAME_PAM
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.navigation.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository.Companion.INTENT_ACTION_REQUEST_PERMISSIONS
import com.kieronquinn.app.ambientmusicmod.ui.screens.nowplaying.NowPlayingFragmentDirections
import com.kieronquinn.app.pixelambientmusic.model.BannerMessage as RemoteBannerMessage

enum class BannerAttentionLevel(
    @DrawableRes val icon: Int,
    @ColorRes val background: Int,
    @ColorRes val accent: Int
) {
    HIGH(
        R.drawable.ic_nowplaying_banner_attention_high,
        R.color.banner_background_attention_high,
        R.color.banner_accent_attention_high
    ),
    MEDIUM(
        R.drawable.ic_nowplaying_banner_attention_medium,
        R.color.banner_background_attention_medium,
        R.color.banner_accent_attention_medium
    ),
    LOW(
        R.drawable.ic_nowplaying_banner_attention_low,
        R.color.banner_background_attention_low,
        R.color.banner_accent_attention_low
    )
}

/**
 *  Convert a [RemoteBannerMessage] (from PAM) to a local [BannerMessage]. This method intentionally
 *  only converts those that can be sent from the remote app.
 */
fun RemoteBannerMessage.toLocalBannerMessage(): BannerMessage {
    return when(this){
        RemoteBannerMessage.DOWNLOADING -> BannerMessage.Downloading
        RemoteBannerMessage.SEARCH_BUTTON_BEING_SET_UP -> BannerMessage.SearchButtonBeingSetUp
        else -> throw RuntimeException("Unsupported RemoteBannerMessage")
    }
}

data class BannerButton(
    @StringRes val buttonText: Int,
    val onClick: NavigationEvent
)

sealed class BannerMessage(
    val attentionLevel: BannerAttentionLevel,
    @StringRes val title: Int,
    @StringRes val message: Int,
    val button: BannerButton? = null
) {

    companion object {
        private val CONNECTIVITY_INTENT by lazy {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Settings.Panel.ACTION_WIFI
            }else Settings.ACTION_WIFI_SETTINGS
        }
    }

    object PermissionsNeeded: BannerMessage(
        BannerAttentionLevel.HIGH,
        R.string.banner_message_permissions_needed_title,
        R.string.banner_message_permissions_needed_content,
        BannerButton(
            R.string.banner_message_permissions_needed_button,
            NavigationEvent.Intent(Intent(INTENT_ACTION_REQUEST_PERMISSIONS).apply {
                `package` = PACKAGE_NAME_PAM
            })
        )
    )

    object Downloading: BannerMessage(
        BannerAttentionLevel.HIGH,
        R.string.banner_message_downloading_title,
        R.string.banner_message_downloading_content
    )

    object NoInternet: BannerMessage(
        BannerAttentionLevel.HIGH,
        R.string.banner_message_no_internet_title,
        R.string.banner_message_no_internet_content,
        BannerButton(
            R.string.banner_message_settings_button,
            NavigationEvent.Intent(Intent(CONNECTIVITY_INTENT))
        )
    )

    object WaitingForUnmeteredInternet: BannerMessage(
        BannerAttentionLevel.HIGH,
        R.string.banner_message_waiting_for_unmetered_internet_title,
        R.string.banner_message_waiting_for_unmetered_internet_content,
        BannerButton(
            R.string.banner_message_settings_button,
            NavigationEvent.Intent(Intent(CONNECTIVITY_INTENT))
        )
    )

    object SearchButtonBeingSetUp: BannerMessage(
        BannerAttentionLevel.MEDIUM,
        R.string.banner_message_search_button_being_set_up_title,
        R.string.banner_message_search_button_being_set_up_content
    )

    object DoNotDisturbEnabled: BannerMessage(
        BannerAttentionLevel.LOW,
        R.string.banner_message_dnd_title,
        R.string.banner_message_dnd_content,
        BannerButton(
            R.string.banner_message_settings_button,
            NavigationEvent.Intent(Intent("android.settings.ZEN_MODE_SETTINGS"))
        )
    )

    object AppUsingDeviceAudio: BannerMessage(
        BannerAttentionLevel.MEDIUM,
        R.string.banner_message_app_using_device_audio_title,
        R.string.banner_message_app_using_device_audio_content
    )

    object AppRecordingAudio: BannerMessage(
        BannerAttentionLevel.MEDIUM,
        R.string.banner_message_app_recording_audio_title,
        R.string.banner_message_app_recording_audio_content
    )

    object GoogleAppInvalid: BannerMessage(
        BannerAttentionLevel.HIGH,
        R.string.banner_message_google_app_invalid_title,
        R.string.banner_message_google_app_invalid_content,
        BannerButton(
            R.string.faq_title_short,
            NavigationEvent.Id(R.id.action_global_faqFragment)
        )
    )

    object MicrophoneDisabled: BannerMessage(
        BannerAttentionLevel.MEDIUM,
        R.string.banner_message_microphone_disabled_title,
        R.string.banner_message_microphone_disabled_content,
        BannerButton(
            R.string.banner_message_settings_button,
            NavigationEvent.Intent(Intent(Settings.ACTION_PRIVACY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
        )
    )

    object BatteryOptimisationsNeedDisabling: BannerMessage(
        BannerAttentionLevel.HIGH,
        R.string.banner_message_battery_optimisations_title,
        R.string.banner_message_battery_optimisations_content,
        BannerButton(
            R.string.banner_message_battery_optimisations_button,
            NavigationEvent.Directions(NowPlayingFragmentDirections.actionNowPlayingFragmentToBatteryOptimisationFragment())
        )
    )

}