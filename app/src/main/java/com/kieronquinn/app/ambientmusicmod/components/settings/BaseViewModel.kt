package com.kieronquinn.app.ambientmusicmod.components.settings

import androidx.lifecycle.ViewModel
import com.kieronquinn.app.ambientmusicmod.components.AmbientSharedPreferences
import com.kieronquinn.app.ambientmusicmod.components.NavigationComponent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BaseViewModel: ViewModel(), KoinComponent {

    val navigation by inject<NavigationComponent>()
    val settings by inject<AmbientSharedPreferences>()

}