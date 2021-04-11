package com.kieronquinn.app.ambientmusicmod.app.ui.container

import android.util.Log
import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.installer.InstallerViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class AmbientContainerSharedViewModel: ViewModel() {

    abstract val mainAppLinkHeight: Flow<Float>
    abstract val fabClick: Flow<InstallerViewModel.CompatibilityStatus>
    abstract val shouldShowFab: Flow<FabVisibility>
    abstract fun setAppLinkHeight(height: Float)
    abstract fun setCompatibilityState(compatibilityState: InstallerViewModel.CompatibilityStatus)
    abstract fun setPage(@IdRes pageId: Int)
    abstract fun onFabClicked()

    enum class CompatibilityState {
        COMPATIBLE,
        NO_XPOSED,
        NOT_COMPATIBLE
    }

    sealed class FabVisibility {
        object Hidden: FabVisibility()
        data class Shown(val compatibilityState: InstallerViewModel.CompatibilityStatus): FabVisibility()
    }

}

class AmbientContainerSharedViewModelImpl: AmbientContainerSharedViewModel() {

    private val _mainAppLinkHeight = MutableSharedFlow<Float>()
    override val mainAppLinkHeight: Flow<Float> = _mainAppLinkHeight.asSharedFlow()
    private val pageId = MutableStateFlow(0)
    private val compatibilityState: MutableStateFlow<InstallerViewModel.CompatibilityStatus?> = MutableStateFlow(null)
    private val _fabClick = MutableSharedFlow<InstallerViewModel.CompatibilityStatus>()
    override val fabClick = _fabClick.asSharedFlow()

    override fun setAppLinkHeight(height: Float) {
        Log.d("AC", "setAppLinkHeight $height")
        viewModelScope.launch {
            _mainAppLinkHeight.emit(height)
        }
    }

    override fun setCompatibilityState(compatibilityStatus: InstallerViewModel.CompatibilityStatus) {
        viewModelScope.launch {
            this@AmbientContainerSharedViewModelImpl.compatibilityState.emit(compatibilityStatus)
        }
    }

    override val shouldShowFab = combine(pageId, compatibilityState){ page, compatibilityStatus ->
        if(page == R.id.installerFragment && compatibilityStatus != null){
            FabVisibility.Shown(compatibilityStatus)
        }else{
            FabVisibility.Hidden
        }
    }

    override fun onFabClicked() {
        viewModelScope.launch {
            Log.d("FabClick", "VM ${compatibilityState.value}")
            _fabClick.emit(compatibilityState.value ?: return@launch)
        }
    }

    override fun setPage(pageId: Int) {
        viewModelScope.launch {
            this@AmbientContainerSharedViewModelImpl.pageId.emit(pageId)
        }
    }

}