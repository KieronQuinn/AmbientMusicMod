package com.kieronquinn.app.ambientmusicmod.repositories

import android.content.Context
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.repositories.BaseSettingsRepository.AmbientMusicModSetting
import com.kieronquinn.app.ambientmusicmod.utils.extensions.createEncryptedSharedPrefDestructively
import com.kieronquinn.app.ambientmusicmod.utils.extensions.randomSecureString

interface EncryptedSettingsRepository {
    val externalAccessEnabled: AmbientMusicModSetting<Boolean>
    val externalAccessRequireToken: AmbientMusicModSetting<Boolean>
    val externalAccessToken: AmbientMusicModSetting<String>
    val externalAccessToggleEnabled: AmbientMusicModSetting<Boolean>
    val externalAccessRecognitionEnabled: AmbientMusicModSetting<Boolean>
}

class EncryptedSettingsRepositoryImpl(
    context: Context
): BaseSettingsRepositoryImpl(), EncryptedSettingsRepository {

    companion object {
        const val EXTERNAL_ACCESS_TOKEN_LENGTH = 16

        private const val EXTERNAL_ACCESS_ENABLED = "external_access_enabled"
        private const val DEFAULT_EXTERNAL_ACCESS_ENABLED = false

        private const val EXTERNAL_ACCESS_REQUIRE_TOKEN = "external_access_require_token"
        private const val DEFAULT_EXTERNAL_ACCESS_REQUIRE_TOKEN = true

        private const val EXTERNAL_ACCESS_TOKEN = "external_access_token"
        private val DEFAULT_EXTERNAL_ACCESS_TOKEN = randomSecureString(EXTERNAL_ACCESS_TOKEN_LENGTH)

        private const val EXTERNAL_ACCESS_TOGGLE_ENABLED = "external_access_toggle_enabled"
        private const val DEFAULT_EXTERNAL_ACCESS_TOGGLE_ENABLED = true

        private const val EXTERNAL_ACCESS_RECOGNITION_ENABLED = "external_access_recognition_enabled"
        private const val DEFAULT_ACCESS_EXTERNAL_RECOGNITION_ENABLED = true
    }

    override val sharedPreferences by lazy {
        context.createEncryptedSharedPrefDestructively(
            "${BuildConfig.APPLICATION_ID}_encrypted_prefs"
        )
    }

    override val externalAccessEnabled =
        boolean(EXTERNAL_ACCESS_ENABLED, DEFAULT_EXTERNAL_ACCESS_ENABLED)

    override val externalAccessRequireToken =
        boolean(EXTERNAL_ACCESS_REQUIRE_TOKEN, DEFAULT_EXTERNAL_ACCESS_REQUIRE_TOKEN)

    override val externalAccessToken =
        string(EXTERNAL_ACCESS_TOKEN, DEFAULT_EXTERNAL_ACCESS_TOKEN)

    override val externalAccessToggleEnabled =
        boolean(EXTERNAL_ACCESS_TOGGLE_ENABLED, DEFAULT_EXTERNAL_ACCESS_TOGGLE_ENABLED)

    override val externalAccessRecognitionEnabled =
        boolean(EXTERNAL_ACCESS_RECOGNITION_ENABLED, DEFAULT_ACCESS_EXTERNAL_RECOGNITION_ENABLED)

}