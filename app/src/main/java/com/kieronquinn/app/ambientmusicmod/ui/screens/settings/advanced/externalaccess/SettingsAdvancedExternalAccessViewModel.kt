package com.kieronquinn.app.ambientmusicmod.ui.screens.settings.advanced.externalaccess

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.EncryptedSettingsRepositoryImpl.Companion.EXTERNAL_ACCESS_TOKEN_LENGTH
import com.kieronquinn.app.ambientmusicmod.utils.extensions.randomSecureString
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SettingsAdvancedExternalAccessViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onEnabledChanged(enabled: Boolean)
    abstract fun onToggleChanged(enabled: Boolean)
    abstract fun onRecognitionChanged(enabled: Boolean)
    abstract fun onRequireTokenChanged(enabled: Boolean)
    abstract fun onTokenClicked(context: Context)
    abstract fun onTokenLongClicked()
    abstract fun onWikiClicked()
    abstract fun navigateBack()

    sealed class ExternalAccessSettingsItem(val type: ItemType): BaseSettingsItem(type) {
        data class Footer(val onLinkClicked: () -> Unit):
            ExternalAccessSettingsItem(ItemType.FOOTER)

        enum class ItemType: BaseSettingsItemType {
            FOOTER
        }
    }

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val enabled: Boolean,
            val toggleEnabled: Boolean,
            val recognitionEnabled: Boolean,
            val requireToken: Boolean,
            val token: String
        ): State()
    }

}

class SettingsAdvancedExternalAccessViewModelImpl(
    private val navigation: ContainerNavigation,
    settings: EncryptedSettingsRepository
): SettingsAdvancedExternalAccessViewModel() {

    private val enabled = settings.externalAccessEnabled
    private val toggleEnabled = settings.externalAccessToggleEnabled
    private val recognitionEnabled = settings.externalAccessRecognitionEnabled
    private val requireTokenEnabled = settings.externalAccessRequireToken
    private val accessToken = settings.externalAccessToken

    override val state = combine(
        enabled.asFlow(),
        toggleEnabled.asFlow(),
        recognitionEnabled.asFlow(),
        requireTokenEnabled.asFlow(),
        accessToken.asFlow()
    ) { enabled, toggle, recognition, requireToken, token ->
        State.Loaded(
            enabled,
            toggle,
            recognition,
            requireToken,
            token
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            this@SettingsAdvancedExternalAccessViewModelImpl.enabled.set(enabled)
            //Set token to itself so it's persisted
            accessToken.set(accessToken.get())
        }
    }

    override fun onToggleChanged(enabled: Boolean) {
        viewModelScope.launch {
            toggleEnabled.set(enabled)
        }
    }

    override fun onRecognitionChanged(enabled: Boolean) {
        viewModelScope.launch {
            recognitionEnabled.set(enabled)
        }
    }

    override fun onRequireTokenChanged(enabled: Boolean) {
        viewModelScope.launch {
            requireTokenEnabled.set(enabled)
        }
    }

    override fun onTokenClicked(context: Context) {
        viewModelScope.launch {
            val token = accessToken.get()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val label = context.getString(R.string.settings_external_access_token_title)
            clipboard.setPrimaryClip(ClipData.newPlainText(label, token))
            Toast.makeText(
                context, R.string.settings_external_access_token_copied, Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onTokenLongClicked() {
        viewModelScope.launch {
            accessToken.set(randomSecureString(EXTERNAL_ACCESS_TOKEN_LENGTH))
        }
    }

    override fun onWikiClicked() {
        viewModelScope.launch {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(
                    "https://github.com/KieronQuinn/AmbientMusicMod/wiki/External-Access"
                )
            }
            navigation.navigate(intent)
        }
    }

    override fun navigateBack() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

}