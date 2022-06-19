package com.kieronquinn.app.ambientmusicmod.ui.screens.batteryoptimisation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.RootNavigation
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.repositories.BatteryOptimisationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class BatteryOptimisationViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun reload()

    abstract fun onBatteryOptimisationClicked()
    abstract fun onOemBatteryOptimisationClicked()
    abstract fun onLearnMoreClicked()

    sealed class State {
        object Loading: State()
        data class Loaded(
            val batteryOptimisationsDisabled: Boolean,
            val oemBatteryOptimisationAvailable: Boolean
        ): State() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
    }

    sealed class BatteryOptimisationSettingsItem(val type: ItemType): BaseSettingsItem(type) {

        data class Footer(val onLinkClicked: () -> Unit):
            BatteryOptimisationSettingsItem(ItemType.FOOTER)

        enum class ItemType: BaseSettingsItemType {
            FOOTER
        }
    }

}

class BatteryOptimisationViewModelImpl(
    context: Context,
    private val batteryOptimisationRepository: BatteryOptimisationRepository,
    private val navigation: RootNavigation
): BatteryOptimisationViewModel() {

    companion object {
        private const val DONT_KILL_ROOT = "https://dontkillmyapp.com/"
        private val DONT_KILL_MAPPING = mapOf(
            Pair("oneplus", "https://dontkillmyapp.com/oneplus"),
            Pair("huawei", "https://dontkillmyapp.com/huawei"),
            Pair("samsung", "https://dontkillmyapp.com/samsung"),
            Pair("xiaomi", "https://dontkillmyapp.com/xiaomi"),
            Pair("meizu", "https://dontkillmyapp.com/meizu"),
            Pair("asus", "https://dontkillmyapp.com/asus"),
            Pair("wiko", "https://dontkillmyapp.com/wiko"),
            Pair("lenovo", "https://dontkillmyapp.com/lenovo"),
            Pair("oppo", "https://dontkillmyapp.com/oppo"),
            Pair("vivo", "https://dontkillmyapp.com/vivo"),
            Pair("realme", "https://dontkillmyapp.com/realme"),
            Pair("blackview", "https://dontkillmyapp.com/blackview"),
            Pair("unihertz", "https://dontkillmyapp.com/unihertz"),
            Pair("nokia", "https://dontkillmyapp.com/hmd-global"),
            Pair("sony", "https://dontkillmyapp.com/sony")
        )
    }

    private val reloadBus = MutableStateFlow(System.currentTimeMillis())

    override val state = reloadBus.mapLatest {
        State.Loaded(
            batteryOptimisationRepository
                .getDisableBatteryOptimisationsIntent() == null,
            batteryOptimisationRepository.areOemOptimisationsAvailable(context)
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun reload() {
        viewModelScope.launch {
            reloadBus.emit(System.currentTimeMillis())
        }
    }

    override fun onBatteryOptimisationClicked() {
        viewModelScope.launch {
            val intent = batteryOptimisationRepository.getDisableBatteryOptimisationsIntent()
                ?: return@launch
            navigation.navigate(intent)
        }
    }

    override fun onOemBatteryOptimisationClicked() {
        viewModelScope.launch {
            navigation.navigateWithContext {
                batteryOptimisationRepository.startOemOptimisationSettings(it)
            }
        }
    }

    override fun onLearnMoreClicked() {
        viewModelScope.launch {
            val url = DONT_KILL_MAPPING[Build.MANUFACTURER.lowercase()] ?: DONT_KILL_ROOT
            navigation.navigate(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            })
        }
    }

}

