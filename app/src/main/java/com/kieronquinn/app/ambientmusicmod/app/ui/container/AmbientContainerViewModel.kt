package com.kieronquinn.app.ambientmusicmod.app.ui.container

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.main.MainSettingsFragmentDirections
import com.kieronquinn.app.ambientmusicmod.components.NavigationComponent
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.github.UpdateChecker
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isAppInstalled
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices.Companion.standardSettingsIntent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class AmbientContainerViewModel: ViewModel() {

    abstract val shouldShowMainAppLink: Flow<Boolean>
    abstract val currentPage: Flow<AmbientContainerViewModelImpl.Page?>

    abstract fun onBottomNavigationItemSelected(navigationTab: NavigationTab, navController: NavController)
    abstract fun getMainAppInstallState()
    abstract fun onMainAppLinkClicked()
    abstract fun onPageChanged(page: AmbientContainerViewModelImpl.Page)
    abstract fun showUpdateDialog(update: UpdateChecker.Update)
    abstract fun onOSSLicencesClicked()

    enum class NavigationTab(@IdRes val destinationId: Int, @IdRes val globalActionId: Int) {
        SETTINGS(R.id.settingsFragment, R.id.action_global_settingsFragment),
        INSTALLER(R.id.installerFragment, R.id.action_global_installerFragment)
    }

}

class AmbientContainerViewModelImpl(private val navigation: NavigationComponent, private val context: Context): AmbientContainerViewModel(){

    private val mainAppInstalled = MutableStateFlow(false)
    private val _currentPage: MutableStateFlow<Page?> = MutableStateFlow(null)
    override val currentPage: Flow<Page?> = _currentPage.asStateFlow()

    private val _shouldShowMainAppLink = combine(mainAppInstalled, _currentPage){ appInstalled: Boolean, page: Page? ->
        Log.d("AC", "shouldShowMainAppLink ${appInstalled && page?.id == R.id.settingsFragment}")
        appInstalled && page?.id == R.id.settingsFragment
    }

    override val shouldShowMainAppLink = MutableStateFlow(false).apply {
        viewModelScope.launch {
            _shouldShowMainAppLink.collect {
                emit(it)
            }
        }
    }

    override fun onBottomNavigationItemSelected(navigationTab: NavigationTab, navController: NavController) {
        viewModelScope.launch {
            if(navController.currentDestination?.id == navigationTab.destinationId) return@launch
            if(!navController.popBackStack(navigationTab.destinationId, false)){
                navigation.navigate(NavigationEvent.NavigateToDestination(navigationTab.globalActionId))
            }
        }
    }

    override fun getMainAppInstallState() {
        viewModelScope.launch {
            mainAppInstalled.emit(context.isAppInstalled(PixelAmbientServices.PIXEL_AMBIENT_SERVICES_PACKAGE_NAME))
        }
    }

    override fun onMainAppLinkClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateToActivityDestination(standardSettingsIntent))
        }
    }

    override fun onPageChanged(page: Page) {
        viewModelScope.launch {
            val previousPage = if(_currentPage.value != null){
                _currentPage.value?.apply {
                    previousPage = null
                }
            } else null
            _currentPage.emit(page.apply {
                this.previousPage = previousPage
            })
        }
    }

    override fun showUpdateDialog(update: UpdateChecker.Update) {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(MainSettingsFragmentDirections.actionGlobalUpdateBottomSheetFragment(update)))
        }
    }

    override fun onOSSLicencesClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateToActivityDestination(Intent(context, OssLicensesMenuActivity::class.java)))
        }
    }

    data class Page(@IdRes val id: Int, val isDialog: Boolean, var previousPage: Page? = null)

}