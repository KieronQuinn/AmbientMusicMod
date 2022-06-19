package com.kieronquinn.app.ambientmusicmod.ui.screens.container

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.navigation.ContainerNavigation
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.UpdatesRepository
import com.kieronquinn.app.ambientmusicmod.ui.base.BaseContainerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class ContainerViewModel: ViewModel(), BaseContainerViewModel {

    abstract val updateAvailable: StateFlow<Boolean>

}

class ContainerViewModelImpl(
    private val navigation: ContainerNavigation,
    updatesRepository: UpdatesRepository,
    shardsRepository: ShardsRepository
): ContainerViewModel() {

    override val updateAvailable = updatesRepository.containerCheckUpdatesBus.mapLatest {
        updatesRepository.isAnyUpdateAvailable() || shardsRepository.isUpdateAvailable()
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override fun onBackPressed() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    //No parent
    override fun onParentBackPressed(): Boolean = false

}