package com.kieronquinn.app.ambientmusicmod.components.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import com.jakewharton.processphoenix.ProcessPhoenix
import com.kieronquinn.app.ambientmusicmod.ui.activities.MainActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect

interface BaseNavigation {

    val navigationBus: Flow<NavigationEvent>

    suspend fun navigate(navDirections: NavDirections)
    suspend fun navigate(@IdRes id: Int, arguments: Bundle? = null)
    suspend fun navigate(intent: Intent)
    suspend fun navigate(event: NavigationEvent)
    suspend fun navigateUpTo(@IdRes id: Int, popInclusive: Boolean = false)
    suspend fun navigateBack()
    suspend fun navigateWithContext(method: (Context) -> Unit)
    suspend fun restartActivity()
    suspend fun finish()
    suspend fun phoenix()

}

sealed class NavigationEvent {
    data class Directions(val directions: NavDirections): NavigationEvent()
    data class Id(@IdRes val id: Int, val arguments: Bundle? = null): NavigationEvent()
    data class PopupTo(@IdRes val id: Int, val popInclusive: Boolean): NavigationEvent()
    data class Intent(val intent: android.content.Intent, val onActivityNotFound: (() -> Unit)? = null): NavigationEvent()
    data class ContextInjectedMethod(val method: (Context) -> Unit): NavigationEvent()
    object Back: NavigationEvent()
    object RestartActivity: NavigationEvent()
    object Finish: NavigationEvent()
    object Phoenix: NavigationEvent()
}

open class NavigationImpl: BaseNavigation {

    private val _navigationBus = MutableSharedFlow<NavigationEvent>()
    override val navigationBus = _navigationBus.asSharedFlow()

    override suspend fun navigate(id: Int, arguments: Bundle?) {
        _navigationBus.emit(NavigationEvent.Id(id, arguments))
    }

    override suspend fun navigate(navDirections: NavDirections) {
        _navigationBus.emit(NavigationEvent.Directions(navDirections))
    }

    override suspend fun navigateBack() {
        _navigationBus.emit(NavigationEvent.Back)
    }

    override suspend fun navigateUpTo(id: Int, popInclusive: Boolean) {
        _navigationBus.emit(NavigationEvent.PopupTo(id, popInclusive))
    }

    override suspend fun navigate(intent: Intent) {
        _navigationBus.emit(NavigationEvent.Intent(intent))
    }

    override suspend fun navigate(event: NavigationEvent) {
        _navigationBus.emit(event)
    }

    override suspend fun navigateWithContext(method: (Context) -> Unit) {
        _navigationBus.emit(NavigationEvent.ContextInjectedMethod(method))
    }

    override suspend fun restartActivity() {
        _navigationBus.emit(NavigationEvent.RestartActivity)
    }

    override suspend fun finish() {
        _navigationBus.emit(NavigationEvent.Finish)
    }

    override suspend fun phoenix() {
        _navigationBus.emit(NavigationEvent.Phoenix)
    }

}

//Root level navigation for full screen changes
interface RootNavigation: BaseNavigation
class RootNavigationImpl: NavigationImpl(), RootNavigation

//Container level navigation for inner changes
interface ContainerNavigation: BaseNavigation
class ContainerNavigationImpl: NavigationImpl(), ContainerNavigation

//Track List has its own full screen navigation
interface TracklistNavigation: BaseNavigation
class TracklistNavigationImpl: NavigationImpl(), TracklistNavigation

//Setup is a whole different flow
interface SetupNavigation: BaseNavigation
class SetupNavigationImpl: NavigationImpl(), SetupNavigation

suspend fun NavHostFragment.setupWithNavigation(navigation: BaseNavigation) {
    navigation.navigationBus.collect {
        when(it){
            is NavigationEvent.Directions -> navController.navigateSafely(it.directions)
            is NavigationEvent.Id -> navController.navigateSafely(it.id, it.arguments ?: Bundle.EMPTY)
            is NavigationEvent.Back -> navController.navigateUp()
            is NavigationEvent.PopupTo -> navController.popBackStack(it.id, it.popInclusive)
            is NavigationEvent.Intent -> try {
                startActivity(it.intent)
            }catch (e: ActivityNotFoundException){
                it.onActivityNotFound?.invoke()
            }
            is NavigationEvent.RestartActivity -> {
                requireActivity().let { activity ->
                    activity.finish()
                    activity.startActivity(activity.intent)
                }
            }
            is NavigationEvent.Finish -> {
                requireActivity().finish()
            }
            is NavigationEvent.Phoenix -> {
                val mainIntent = Intent(requireContext(), MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_SKIP_SPLASH, true)
                }
                ProcessPhoenix.triggerRebirth(requireContext(), mainIntent)
            }
            is NavigationEvent.ContextInjectedMethod -> {
                it.method.invoke(requireContext())
            }
        }
    }
}

private fun NavController.navigateSafely(directions: NavDirections){
    currentDestination?.getAction(directions.actionId)?.let {
        if(it.destinationId != currentDestination?.id) {
            navigate(directions)
        }
    }
}

private fun NavController.navigateSafely(@IdRes action: Int, arguments: Bundle){
    currentDestination?.getAction(action)?.let {
        if(it.destinationId != currentDestination?.id) {
            navigate(action, arguments)
        }
    }
}