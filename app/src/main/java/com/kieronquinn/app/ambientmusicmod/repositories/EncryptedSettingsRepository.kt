package com.kieronquinn.app.ambientmusicmod.repositories

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.repositories.BaseSettingsRepository.AmbientMusicModSetting
import com.kieronquinn.app.ambientmusicmod.utils.extensions.randomSecureString
import java.security.KeyStore

interface EncryptedSettingsRepository {

    val encryptionAvailable: Boolean

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

        private fun tryLoadSharedPreferences(context: Context): SharedPreferences? {
            //Regular load, should work 99% of the time
            getSharedPreferences(context)?.let {
                return it
            }
            //If failed, delete the key and start again
            deleteMasterKeyEntry()
            //If it still fails, nothing we can do
            return getSharedPreferences(context)
        }

        private fun getSharedPreferences(context: Context): SharedPreferences? {
            return try {
                val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
                val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
                EncryptedSharedPreferences.create(
                    "${BuildConfig.APPLICATION_ID}_encrypted_prefs",
                    mainKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }catch (e: Exception) {
                //Failed to load shared prefs
                null
            }
        }

        private fun deleteMasterKeyEntry() {
            try {
                KeyStore.getInstance("AndroidKeyStore").apply {
                    load(null)
                    deleteEntry("_androidx_security_master_key_")
                }
            }catch (e: Exception){
                //Failed to delete key
            }
        }
    }

    private val encryptedSharedPreferences by lazy {
        tryLoadSharedPreferences(context)
    }

    override val sharedPreferences
        get() = encryptedSharedPreferences ?: throw RuntimeException("Encrypted prefs failed to load")

    override val encryptionAvailable = encryptedSharedPreferences != null

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