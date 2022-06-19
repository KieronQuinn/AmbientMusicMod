package com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore.restoreoptions

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.repositories.BackupRestoreRepository.RestoreOptions
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class BackupRestoreOptionsViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract fun onNextClicked(uri: Uri)

    abstract fun onRestoreFavouritesChanged(enabled: Boolean)
    abstract fun onClearFavouritesChanged(enabled: Boolean)
    abstract fun onRestoreHistoryChanged(enabled: Boolean)
    abstract fun onClearHistoryChanged(enabled: Boolean)
    abstract fun onRestoreLinearChanged(enabled: Boolean)
    abstract fun onClearLinearChanged(enabled: Boolean)
    abstract fun onRestoreSettingsChanged(enabled: Boolean)

    sealed class State {
        object Loading: State()
        data class Loaded(
            val restoreFavourites: Boolean,
            val clearFavourites: Boolean,
            val restoreHistory: Boolean,
            val clearHistory: Boolean,
            val restoreLinear: Boolean,
            val clearLinear: Boolean,
            val restoreSettings: Boolean
        ): State()
    }

}

class BackupRestoreOptionsViewModelImpl(
    private val navigation: ContainerNavigation
): BackupRestoreOptionsViewModel() {

    private val restoreFavourites = MutableStateFlow(true)
    private val clearFavourites = MutableStateFlow(false)
    private val restoreHistory = MutableStateFlow(true)
    private val clearHistory = MutableStateFlow(false)
    private val clearLinear = MutableStateFlow(false)
    private val restoreLinear = MutableStateFlow(true)
    private val restoreSettings = MutableStateFlow(true)

    private val favourites = combine(restoreFavourites, clearFavourites) { restore, clear ->
        Pair(restore, clear)
    }

    private val history = combine(restoreHistory, clearHistory) { restore, clear ->
        Pair(restore, clear)
    }

    private val linear = combine(restoreLinear, clearLinear) { restore, clear ->
        Pair(restore, clear)
    }

    override val state = combine(
        favourites,
        history,
        linear,
        restoreSettings
    ) { favourites, history, linear, settings ->
        State.Loaded(
            favourites.first,
            favourites.second,
            history.first,
            history.second,
            linear.first,
            linear.second,
            settings
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onRestoreFavouritesChanged(enabled: Boolean) {
        viewModelScope.launch {
            restoreFavourites.emit(enabled)
        }
    }

    override fun onClearFavouritesChanged(enabled: Boolean) {
        viewModelScope.launch {
            clearFavourites.emit(enabled)
        }
    }

    override fun onRestoreHistoryChanged(enabled: Boolean) {
        viewModelScope.launch {
            restoreHistory.emit(enabled)
        }
    }

    override fun onClearHistoryChanged(enabled: Boolean) {
        viewModelScope.launch {
            clearHistory.emit(enabled)
        }
    }

    override fun onRestoreLinearChanged(enabled: Boolean) {
        viewModelScope.launch {
            restoreLinear.emit(enabled)
        }
    }

    override fun onClearLinearChanged(enabled: Boolean) {
        viewModelScope.launch {
            clearLinear.emit(enabled)
        }
    }

    override fun onRestoreSettingsChanged(enabled: Boolean) {
        viewModelScope.launch {
            restoreSettings.emit(enabled)
        }
    }

    override fun onNextClicked(uri: Uri) {
        val options = (state.value as? State.Loaded)?.let {
            RestoreOptions(
                it.restoreHistory,
                it.clearHistory,
                it.clearLinear,
                it.restoreLinear,
                it.restoreFavourites,
                it.clearFavourites,
                it.restoreSettings
            )
        } ?: return
        viewModelScope.launch {
            navigation.navigate(BackupRestoreOptionsFragmentDirections.actionBackupRestoreOptionsFragmentToBackupRestoreRestoreFragment(uri, options))
        }
    }

}