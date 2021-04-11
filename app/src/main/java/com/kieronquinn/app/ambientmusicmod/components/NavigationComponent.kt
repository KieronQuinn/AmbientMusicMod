package com.kieronquinn.app.ambientmusicmod.components

import android.content.Intent
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import com.kieronquinn.app.ambientmusicmod.utils.extensions.withStandardAnimations
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

interface NavigationComponent {
    val navigationBus: Flow<NavigationEvent>
    suspend fun navigate(navigationEvent: NavigationEvent)
}

class NavigationComponentImpl: NavigationComponent {

    private val _navigationBus = MutableSharedFlow<NavigationEvent>()
    override val navigationBus: SharedFlow<NavigationEvent> = _navigationBus

    override suspend fun navigate(navigationEvent: NavigationEvent) {
        _navigationBus.emit(navigationEvent)
    }

}

sealed class NavigationEvent {
    data class NavigateUp(@IdRes val popUpTo: Int? = null, val popUpInclusive: Boolean? = null): NavigationEvent()
    data class NavigateToDestination(@IdRes val destination: Int, val arguments: Bundle? = null): NavigationEvent()
    data class NavigateToActivityDestination(val intent: Intent, val navOptions: NavOptions = NavOptions.Builder().withStandardAnimations().build()): NavigationEvent()
    data class NavigateByDirections(val directions: NavDirections): NavigationEvent()
}