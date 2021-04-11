package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.ActivityNavigator
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.settings.RootFragment

fun Intent.toActivityDestination(context: Context): ActivityNavigator.Destination {
    return ActivityNavigator(context).createDestination().apply {
        intent = this@toActivityDestination
    }
}

fun NavOptions.Builder.withStandardAnimations(): NavOptions.Builder {
    setEnterAnim(R.anim.slide_in_right)
    setExitAnim(R.anim.slide_out_left)
    setPopEnterAnim(R.anim.slide_in_left)
    setPopExitAnim(R.anim.slide_out_right)
    return this
}

fun NavController.handleNavigationEvent(context: Context, navigationEvent: NavigationEvent){
    when(navigationEvent){
        is NavigationEvent.NavigateUp -> {
            if(navigationEvent.popUpTo != null){
                popBackStack(navigationEvent.popUpTo, navigationEvent.popUpInclusive ?: false)
            }else{
                navigateUp()
            }
        }
        is NavigationEvent.NavigateToDestination -> {
            navigateSafe(navigationEvent.destination, navigationEvent.arguments)
        }
        is NavigationEvent.NavigateToActivityDestination -> {
            ActivityNavigator(context).navigate(navigationEvent.intent.toActivityDestination(context), null, navigationEvent.navOptions, null)
        }
        is NavigationEvent.NavigateByDirections -> {
            navigateSafe(navigationEvent.directions)
        }
    }
}

private fun NavController.navigateSafe(@IdRes destination: Int, arguments: Bundle?){
    try {
        navigate(destination, arguments)
    }catch (e: IllegalArgumentException){
        //Destination unknown
        e.printStackTrace()
    }
}

private fun NavController.navigateSafe(navDirections: NavDirections){
    try {
        navigate(navDirections)
    }catch (e: IllegalArgumentException){
        //Destination unknown
    }
}

val NavHostFragment.isRootFragment: Boolean
        get() = childFragmentManager.fragments.firstOrNull() is RootFragment

val NavHostFragment.isBottomSheet: Boolean
        get() = childFragmentManager.fragments.firstOrNull() is BottomSheetDialogFragment