package com.kieronquinn.app.ambientmusicmod.ui.screens.setup.shizuku

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.navigation.SetupNavigation
import com.kieronquinn.app.ambientmusicmod.repositories.ShizukuServiceRepository
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isAppInstalled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class SetupShizukuViewModel: ViewModel() {

    abstract fun moveToNext()
    abstract fun onGetShizukuClicked()
    abstract fun refresh()

    abstract val state: StateFlow<State>

    enum class ShizukuFailedState(
        @StringRes val title: Int,
        @StringRes val message: Int,
        @StringRes val button: Int,
        @DrawableRes val buttonIcon: Int
    ) {
        NOT_INSTALLED(
            R.string.setup_shizuku_error_shizuku,
            R.string.setup_shizuku_error_shizuku_subtitle,
            R.string.setup_shizuku_get,
            R.drawable.ic_google_play
        ),
        NEEDS_START(
            R.string.setup_shizuku_error_shizuku,
            R.string.setup_shizuku_error_shizuku_subtitle,
            R.string.setup_shizuku_open,
            R.drawable.ic_open
        ),
        NEEDS_ROOT(
            R.string.setup_shizuku_needs_root,
            R.string.setup_shizuku_needs_root_subtitle,
            R.string.setup_shizuku_faq,
            R.drawable.ic_faq
        )
    }

    sealed class State {
        object Loading: State()
        object Success: State()
        data class Failed(val state: ShizukuFailedState): State()
    }

}

class SetupShizukuViewModelImpl(
    context: Context,
    private val navigation: SetupNavigation,
    shizukuServiceRepository: ShizukuServiceRepository
): SetupShizukuViewModel() {

    companion object {
        private const val PACKAGE_NAME_SHIZUKU = "moe.shizuku.privileged.api"
    }

    private val packageManager = context.packageManager
    private val refreshBus = MutableStateFlow(System.currentTimeMillis())

    override val state = refreshBus.map {
        if(shizukuServiceRepository.assertReady()) {
            val isCompatible = shizukuServiceRepository.runWithService { it.isCompatible }.unwrap()
            if(isCompatible != null){
                if(isCompatible){
                    State.Success
                }else{
                    State.Failed(ShizukuFailedState.NEEDS_ROOT)
                }
            }else State.Failed(ShizukuFailedState.NEEDS_START)
        } else {
            State.Failed(if(isShizukuInstalled()){
                ShizukuFailedState.NEEDS_START
            } else {
                ShizukuFailedState.NOT_INSTALLED
            })
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun moveToNext() {
        viewModelScope.launch {
            navigation.navigate(SetupShizukuFragmentDirections.actionSetupShizukuFragmentToSetupDataUsageFragment())
        }
    }

    override fun onGetShizukuClicked() {
        viewModelScope.launch {
            val state = (state.value as? State.Failed) ?: return@launch
            if(state.state == ShizukuFailedState.NEEDS_ROOT){
                navigation.navigate(R.id.action_global_setupFaqFragment)
            }else{
                showShizuku()
            }
        }
    }

    private suspend fun showShizuku() {
        val launchIntent = packageManager.getLaunchIntentForPackage(PACKAGE_NAME_SHIZUKU)
        if(launchIntent != null){
            navigation.navigate(launchIntent)
        }else {
            navigation.navigate(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$PACKAGE_NAME_SHIZUKU")
            })
        }
    }

    override fun refresh() {
        viewModelScope.launch {
            refreshBus.emit(System.currentTimeMillis())
        }
    }

    private fun isShizukuInstalled(): Boolean {
        return packageManager.isAppInstalled(PACKAGE_NAME_SHIZUKU)
    }

}