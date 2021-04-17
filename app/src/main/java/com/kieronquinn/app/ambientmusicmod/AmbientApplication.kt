package com.kieronquinn.app.ambientmusicmod

import android.annotation.SuppressLint
import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.os.BuildCompat
import com.kieronquinn.app.ambientmusicmod.app.service.GetModelStateForegroundService
import com.kieronquinn.app.ambientmusicmod.app.ui.container.AmbientContainerSharedViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.container.AmbientContainerSharedViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.container.AmbientContainerViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.container.AmbientContainerViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.database.DatabaseSharedViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.database.DatabaseSharedViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.database.copy.DatabaseCopyViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.database.copy.DatabaseCopyViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.database.copywarning.DatabaseCopyWarningViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.database.copywarning.DatabaseCopyWarningViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.DatabaseViewerSharedViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.DatabaseViewerSharedViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.DatabaseViewerViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.DatabaseViewerViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.artists.DatabaseViewerArtistsViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.artists.DatabaseViewerArtistsViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.artists.tracks.DatabaseViewerArtistTracksViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.artists.tracks.DatabaseViewerArtistTracksViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.tracks.DatabaseViewerTracksViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.database.viewer.tracks.DatabaseViewerTracksViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.installer.InstallerViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.installer.InstallerViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.installer.build.InstallerBuildViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.installer.build.InstallerBuildViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.installer.modelstate.InstallerModelStateCheckBottomSheetViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.installer.modelstate.InstallerModelStateCheckBottomSheetViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.installer.outputpicker.InstallerOutputPickerViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.installer.outputpicker.InstallerOutputPickerViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.installer.xposed.InstallerXposedWarningBottomSheetViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.installer.xposed.InstallerXposedWarningBottomSheetViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.advanced.SettingsAdvancedViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.advanced.SettingsAdvancedViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.advanced.customamplification.SettingsAdvancedCustomAmplificationBottomSheetViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.advanced.customamplification.SettingsAdvancedCustomAmplificationBottomSheetViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.amplification.SettingsAmplificationBottomSheetViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.amplification.SettingsAmplificationBottomSheetViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.batteryoptimisation.SettingsBatteryOptimisationViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.batteryoptimisation.SettingsBatteryOptimisationViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.developer.SettingsDeveloperOptionsViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.developer.SettingsDeveloperOptionsViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.developer.log.SettingsDeveloperOptionsLogViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.developer.log.SettingsDeveloperOptionsLogViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.developer.log.dump.SettingsDeveloperOptionsDumpLogViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.developer.log.dump.SettingsDeveloperOptionsDumpLogViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.developer.phenotypes.SettingsDeveloperOptionsPhenotypesViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.developer.phenotypes.SettingsDeveloperOptionsPhenotypesViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.listenperiod.SettingsListenPeriodBottomSheetViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.listenperiod.SettingsListenPeriodBottomSheetViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.lockscreenoverlay.SettingsLockscreenOverlayViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.lockscreenoverlay.SettingsLockscreenOverlayViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.lockscreenoverlay.position.SettingsLockscreenOverlayPositionViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.lockscreenoverlay.position.SettingsLockscreenOverlayPositionViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.main.MainSettingsViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.main.MainSettingsViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.manualtrigger.SettingsManualTriggerBottomSheetViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.manualtrigger.SettingsManualTriggerBottomSheetViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.manualtrigger.playback.SettingsManualTriggerPlaybackBottomSheetViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.manualtrigger.playback.SettingsManualTriggerPlaybackBottomSheetViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.splash.SplashViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.splash.SplashViewModelImpl
import com.kieronquinn.app.ambientmusicmod.app.ui.update.download.UpdateDownloadBottomSheetViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.update.download.UpdateDownloadBottomSheetViewModelImpl
import com.kieronquinn.app.ambientmusicmod.components.*
import com.kieronquinn.app.ambientmusicmod.components.github.UpdateChecker
import com.kieronquinn.app.ambientmusicmod.components.superpacks.getSuperpacksFileUri
import com.kieronquinn.app.ambientmusicmod.utils.blur.BlurUtils
import com.kieronquinn.app.ambientmusicmod.utils.blur.BlurUtilsAndroid26
import com.kieronquinn.app.ambientmusicmod.utils.blur.BlurUtilsAndroid30
import com.kieronquinn.app.ambientmusicmod.utils.blur.BlurUtilsAndroid31
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getTempInputFileUri
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class AmbientApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AmbientApplication)
            modules(coreModule)
            modules(viewModelModule)
        }
        grantUriPerms()
        startForegroundService(Intent(this, GetModelStateForegroundService::class.java))
    }

    private val coreModule = module {
        single<NavigationComponent> { NavigationComponentImpl() }
        single<AmbientSharedPreferences> { AppSharedPreferences(get()) }
        single { getBlurUtils() }
        single { OffsetProvider(get()) }
        single { UpdateChecker() }
    }

    private val viewModelModule = module {
        viewModel<SplashViewModel> { SplashViewModelImpl() }

        viewModel<AmbientContainerViewModel> { AmbientContainerViewModelImpl(get(), get()) }
        viewModel<AmbientContainerSharedViewModel> { AmbientContainerSharedViewModelImpl() }
        viewModel<MainSettingsViewModel> { MainSettingsViewModelImpl(get()) }
        viewModel<SettingsAdvancedViewModel> { SettingsAdvancedViewModelImpl() }
        viewModel<SettingsBatteryOptimisationViewModel> { SettingsBatteryOptimisationViewModelImpl(get()) }
        viewModel<SettingsDeveloperOptionsViewModel> { SettingsDeveloperOptionsViewModelImpl() }
        viewModel<SettingsDeveloperOptionsPhenotypesViewModel> { SettingsDeveloperOptionsPhenotypesViewModelImpl() }

        //Settings bottom sheets
        viewModel<SettingsAmplificationBottomSheetViewModel> { SettingsAmplificationBottomSheetViewModelImpl() }
        viewModel<SettingsListenPeriodBottomSheetViewModel> { SettingsListenPeriodBottomSheetViewModelImpl() }
        viewModel<SettingsManualTriggerBottomSheetViewModel> { SettingsManualTriggerBottomSheetViewModelImpl(get()) }
        viewModel<SettingsManualTriggerPlaybackBottomSheetViewModel> { SettingsManualTriggerPlaybackBottomSheetViewModelImpl(get()) }
        viewModel<SettingsAdvancedCustomAmplificationBottomSheetViewModel> { SettingsAdvancedCustomAmplificationBottomSheetViewModelImpl() }

        //Overlay settings
        viewModel<SettingsLockscreenOverlayViewModel> { SettingsLockscreenOverlayViewModelImpl(get()) }
        viewModel<SettingsLockscreenOverlayPositionViewModel> { SettingsLockscreenOverlayPositionViewModelImpl(get()) }

        //Installer
        viewModel<InstallerViewModel> { InstallerViewModelImpl(get()) }
        viewModel<InstallerXposedWarningBottomSheetViewModel> { InstallerXposedWarningBottomSheetViewModelImpl() }
        viewModel<InstallerOutputPickerViewModel> { InstallerOutputPickerViewModelImpl() }
        viewModel<InstallerBuildViewModel> { InstallerBuildViewModelImpl(get()) }
        viewModel<InstallerModelStateCheckBottomSheetViewModel> { InstallerModelStateCheckBottomSheetViewModelImpl(get()) }

        //Database viewer
        viewModel<DatabaseSharedViewModel> { DatabaseSharedViewModelImpl(get()) }
        viewModel<DatabaseCopyWarningViewModel> { DatabaseCopyWarningViewModelImpl() }
        viewModel<DatabaseCopyViewModel> { DatabaseCopyViewModelImpl(get()) }
        viewModel<DatabaseViewerViewModel> { DatabaseViewerViewModelImpl(get()) }
        viewModel<DatabaseViewerSharedViewModel> { DatabaseViewerSharedViewModelImpl() }
        viewModel<DatabaseViewerTracksViewModel> { DatabaseViewerTracksViewModelImpl() }
        viewModel<DatabaseViewerArtistsViewModel> { DatabaseViewerArtistsViewModelImpl() }
        viewModel<DatabaseViewerArtistTracksViewModel> { DatabaseViewerArtistTracksViewModelImpl() }

        //Log dump
        viewModel<SettingsDeveloperOptionsLogViewModel> { SettingsDeveloperOptionsLogViewModelImpl() }
        viewModel<SettingsDeveloperOptionsDumpLogViewModel> { SettingsDeveloperOptionsDumpLogViewModelImpl(get()) }

        //Updater
        viewModel<UpdateDownloadBottomSheetViewModel> { UpdateDownloadBottomSheetViewModelImpl(getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager) }
    }

    /**
     *  Give Pixel Ambient Services permission to write to the temp input file for manual trigger playback
     */
    private fun grantUriPerms(){
        grantUriPermission(PixelAmbientServices.PIXEL_AMBIENT_SERVICES_PACKAGE_NAME, getTempInputFileUri(), Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        grantUriPermission(PixelAmbientServices.PIXEL_AMBIENT_SERVICES_PACKAGE_NAME, getSuperpacksFileUri(), Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    @SuppressLint("NewApi")
    private fun getBlurUtils(): BlurUtils {
        return when {
            // Build.VERSION.SDK_INT >= 31 -> BlurUtilsAndroid31(resources)
            BuildCompat.isAtLeastS() -> BlurUtilsAndroid31(resources)
            Build.VERSION.SDK_INT >= 30 -> BlurUtilsAndroid30(resources)
            else -> BlurUtilsAndroid26(resources)
        }
    }

}