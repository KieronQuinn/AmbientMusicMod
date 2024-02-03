package com.kieronquinn.app.ambientmusicmod.repositories

import android.content.Intent
import com.kieronquinn.app.ambientmusicmod.repositories.RecognitionRepository.RecognitionState
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository.SettingsState
import com.kieronquinn.app.ambientmusicmod.service.AmbientMusicModForegroundService
import com.kieronquinn.app.ambientmusicmod.utils.extensions.firstNotNull
import com.kieronquinn.app.pixelambientmusic.model.SettingsStateChange
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

interface ExternalAccessRepository {

    fun onEnable(intent: Intent)
    fun onDisable(intent: Intent)
    fun onToggle(intent: Intent)
    fun onRecognise(intent: Intent, online: Boolean)

}

class ExternalAccessRepositoryImpl(
    private val settings: RemoteSettingsRepository,
    private val encryptedSettings: EncryptedSettingsRepository,
    private val recognitionRepository: RecognitionRepository
): ExternalAccessRepository {

    companion object {
        private const val EXTRA_TOKEN = "token"
    }

    private val scope = MainScope()

    override fun onEnable(intent: Intent) {
        scope.launch {
            if(!verifyTokenIfRequired(intent)) return@launch
            if(!toggleEnabled()) return@launch
            settings.commitChanges(SettingsStateChange(mainEnabled = true))
        }
    }

    override fun onDisable(intent: Intent) {
        scope.launch {
            if(!verifyTokenIfRequired(intent)) return@launch
            if(!toggleEnabled()) return@launch
            settings.commitChanges(SettingsStateChange(mainEnabled = false))
        }
    }

    override fun onToggle(intent: Intent) {
        scope.launch {
            if(!verifyTokenIfRequired(intent)) return@launch
            if(!toggleEnabled()) return@launch
            val current = isEnabled() ?: return@launch
            settings.commitChanges(SettingsStateChange(mainEnabled = !current))
        }
    }

    override fun onRecognise(intent: Intent, online: Boolean) {
        scope.launch {
            if(!verifyTokenIfRequired(intent)) return@launch
            if(!recognitionEnabled()) return@launch
            if(online){
                if(isOnDemandEnabled() != true) return@launch
                recognitionRepository.requestOnDemandRecognition()
            }else{
                recognitionRepository.requestRecognition()
            }.collect {
                if(it is RecognitionState.Recognised){
                    AmbientMusicModForegroundService.sendManualRecognition(it)
                }
            }
        }
    }

    private suspend fun isEnabled(): Boolean? {
        return withTimeout(5000L) {
            val remote = settings.getRemoteSettings().firstNotNull()
            if(remote !is SettingsState.Available) return@withTimeout null
            remote.mainEnabled
        }
    }

    private suspend fun isOnDemandEnabled(): Boolean? {
        return withTimeout(5000L) {
            val remote = settings.getRemoteSettings().firstNotNull()
            if(remote !is SettingsState.Available) return@withTimeout null
            remote.onDemandCapable && remote.onDemandEnabled
        }
    }

    private suspend fun verifyTokenIfRequired(intent: Intent): Boolean {
        if(!encryptedSettings.externalAccessRequireToken.get()) return true
        val token = intent.getStringExtra(EXTRA_TOKEN) ?: return false
        return encryptedSettings.externalAccessToken.get() == token
    }

    private suspend fun toggleEnabled(): Boolean {
        return encryptedSettings.externalAccessEnabled.get()
                && encryptedSettings.externalAccessToggleEnabled.get()
    }

    private suspend fun recognitionEnabled(): Boolean {
        return encryptedSettings.externalAccessEnabled.get()
                && encryptedSettings.externalAccessRecognitionEnabled.get()
    }

}