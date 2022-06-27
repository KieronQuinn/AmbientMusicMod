package com.kieronquinn.app.ambientmusicmod

import android.app.Application
import android.content.Context
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.color.DynamicColors
import com.kieronquinn.app.ambientmusicmod.components.blur.BlurProvider
import com.kieronquinn.app.ambientmusicmod.components.navigation.*
import com.kieronquinn.app.ambientmusicmod.repositories.*
import com.kieronquinn.app.ambientmusicmod.ui.activities.MainActivityViewModel
import com.kieronquinn.app.ambientmusicmod.ui.activities.MainActivityViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore.BackupRestoreViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore.BackupRestoreViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore.backup.BackupRestoreBackupViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore.backup.BackupRestoreBackupViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore.restore.BackupRestoreRestoreViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore.restore.BackupRestoreRestoreViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore.restoreoptions.BackupRestoreOptionsViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore.restoreoptions.BackupRestoreOptionsViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.batteryoptimisation.BatteryOptimisationViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.batteryoptimisation.BatteryOptimisationViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.container.ContainerViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.container.ContainerViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.contributors.ContributorsViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.contributors.ContributorsViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.LockScreenViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.LockScreenViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.action.LockScreenActionViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.action.LockScreenActionViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.ownerinfo.LockScreenOwnerInfoViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.ownerinfo.LockScreenOwnerInfoViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.ownerinfo.fallback.LockScreenOwnerInfoFallbackBottomSheetViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.ownerinfo.fallback.LockScreenOwnerInfoFallbackBottomSheetViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.position.LockScreenPositionViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.position.LockScreenPositionViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.LockScreenTextColourViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.LockScreenTextColourViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.custom.LockScreenCustomTextColourCustomViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.custom.LockScreenCustomTextColourCustomViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.monet.LockScreenCustomTextColourMonetViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.monet.LockScreenCustomTextColourMonetViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.nowplaying.NowPlayingViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.nowplaying.NowPlayingViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.ondemand.OnDemandViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.ondemand.OnDemandViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.recognition.RecognitionViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.recognition.RecognitionViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.SettingsViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.SettingsViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.advanced.SettingsAdvancedViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.advanced.SettingsAdvancedViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.advanced.gain.SettingsAdvancedGainBottomSheetViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.advanced.gain.SettingsAdvancedGainBottomSheetViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.bedtime.SettingsBedtimeViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.bedtime.SettingsBedtimeViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.recognitionbuffer.SettingsRecognitionBufferViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.recognitionbuffer.SettingsRecognitionBufferViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.recognitionperiod.SettingsRecognitionPeriodViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.recognitionperiod.SettingsRecognitionPeriodViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.batteryoptimisation.SetupBatteryOptimisationViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.batteryoptimisation.SetupBatteryOptimisationViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.complete.SetupCompleteViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.complete.SetupCompleteViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.container.SetupContainerViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.container.SetupContainerViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.countrypicker.SetupCountryPickerViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.countrypicker.SetupCountryPickerViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.datausage.SetupDataUsageViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.datausage.SetupDataUsageViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.installpam.SetupInstallPAMViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.installpam.SetupInstallPAMViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.landing.SetupLandingViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.landing.SetupLandingViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.permissions.SetupPermissionsViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.permissions.SetupPermissionsViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.shizuku.SetupShizukuViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.shizuku.SetupShizukuViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.TracklistViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.TracklistViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.artists.TracklistArtistsViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.artists.TracklistArtistsViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.artists.artisttracks.TracklistArtistTracksViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.artists.artisttracks.TracklistArtistTracksViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.tracks.TracklistTracksViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.tracks.TracklistTracksViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.updates.UpdatesViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.updates.UpdatesViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.updates.country.CountryPickerViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.updates.country.CountryPickerViewModelImpl
import com.kieronquinn.app.ambientmusicmod.ui.screens.updates.download.UpdatesDownloadViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.updates.download.UpdatesDownloadViewModelImpl
import com.kieronquinn.monetcompat.core.MonetCompat
import dagger.hilt.android.HiltAndroidApp
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.lsposed.hiddenapibypass.HiddenApiBypass

@HiltAndroidApp
class AmbientMusicMod: Application() {

    private val repositoriesModule = module {
        single<ApiRepository> { ApiRepositoryImpl(get()) }
        single<SettingsRepository> { SettingsRepositoryImpl(get()) }
        single<DeviceConfigRepository>(createdAtStart = true) { DeviceConfigRepositoryImpl(get(), get()) }
        single<RootNavigation> { RootNavigationImpl() }
        single<ContainerNavigation> { ContainerNavigationImpl() }
        single<TracklistNavigation> { TracklistNavigationImpl() }
        single<SetupNavigation> { SetupNavigationImpl() }
        single<ShizukuServiceRepository> { ShizukuServiceRepositoryImpl(get()) }
        single<AmbientServiceRepository> { AmbientServiceRepositoryImpl(get()) }
        single<RecognitionRepository> { RecognitionRepositoryImpl(get(), get(), get()) }
        single<RemoteSettingsRepository> { RemoteSettingsRepositoryImpl(get(), get(), get(), get(), get(), get(), get()) }
        single<AccessibilityRepository> { AccessibilityRepositoryImpl(get(), get()) }
        single<BedtimeRepository> { BedtimeRepositoryImpl(get(), get()) }
        single<ShardsRepository> { ShardsRepositoryImpl(get(), get()) }
        single<UpdatesRepository> { UpdatesRepositoryImpl(get()) }
        single<WidgetRepository>(createdAtStart = true) { WidgetRepositoryImpl(get(), get(), get(), get()) }
        single<BackupRestoreRepository> { BackupRestoreRepositoryImpl(get(), get(), get(), get()) }
        single<BatteryOptimisationRepository> { BatteryOptimisationRepositoryImpl(get()) }
        single { BlurProvider.getBlurProvider(resources) }
        single { createMarkwon() }
    }

    private val viewModelsModule = module {
        viewModel<MainActivityViewModel> { MainActivityViewModelImpl(get(), get()) }
        viewModel<ContainerViewModel> { ContainerViewModelImpl(get(), get(), get()) }
        viewModel<NowPlayingViewModel> { NowPlayingViewModelImpl(get(), get(), get(), get(), get()) }
        viewModel<RecognitionViewModel> { RecognitionViewModelImpl(get(), get(), get(), get(), get()) }
        viewModel<OnDemandViewModel> { OnDemandViewModelImpl(get(), get(), get()) }
        viewModel<LockScreenViewModel> { LockScreenViewModelImpl(get(), get(), get(), get(), get(), get()) }
        viewModel<LockScreenPositionViewModel> { LockScreenPositionViewModelImpl(get(), get()) }
        viewModel<LockScreenActionViewModel> { LockScreenActionViewModelImpl(get()) }
        viewModel<LockScreenOwnerInfoViewModel> { LockScreenOwnerInfoViewModelImpl(get(), get(), get()) }
        viewModel<LockScreenOwnerInfoFallbackBottomSheetViewModel> { LockScreenOwnerInfoFallbackBottomSheetViewModelImpl(get(), get()) }
        viewModel<LockScreenTextColourViewModel> { LockScreenTextColourViewModelImpl(get(), get()) }
        viewModel<LockScreenCustomTextColourMonetViewModel> { LockScreenCustomTextColourMonetViewModelImpl(get(), get()) }
        viewModel<LockScreenCustomTextColourCustomViewModel> { LockScreenCustomTextColourCustomViewModelImpl(get(), get()) }
        viewModel<SettingsViewModel> { SettingsViewModelImpl(get(), get(), get()) }
        viewModel<SettingsRecognitionPeriodViewModel> { SettingsRecognitionPeriodViewModelImpl(get()) }
        viewModel<SettingsRecognitionBufferViewModel> { SettingsRecognitionBufferViewModelImpl(get()) }
        viewModel<SettingsBedtimeViewModel> { SettingsBedtimeViewModelImpl(get()) }
        viewModel<SettingsAdvancedViewModel> { SettingsAdvancedViewModelImpl(get(), get(), get()) }
        viewModel<SettingsAdvancedGainBottomSheetViewModel> { SettingsAdvancedGainBottomSheetViewModelImpl(get(), get()) }
        viewModel<UpdatesViewModel> { UpdatesViewModelImpl(get(), get(), get(), get(), get(), get(), get()) }
        viewModel<UpdatesDownloadViewModel> { UpdatesDownloadViewModelImpl(get(), get(), get()) }
        viewModel<CountryPickerViewModel> { CountryPickerViewModelImpl(get()) }
        viewModel<SetupContainerViewModel> { SetupContainerViewModelImpl(get(), get()) }
        viewModel<SetupLandingViewModel> { SetupLandingViewModelImpl(get()) }
        viewModel<SetupShizukuViewModel> { SetupShizukuViewModelImpl(get(), get(), get()) }
        viewModel<SetupDataUsageViewModel> { SetupDataUsageViewModelImpl(get(), get(), get(), get()) }
        viewModel<SetupCountryPickerViewModel> { SetupCountryPickerViewModelImpl(get(), get()) }
        viewModel<SetupInstallPAMViewModel> { SetupInstallPAMViewModelImpl(get(), get(), get()) }
        viewModel<SetupPermissionsViewModel> { SetupPermissionsViewModelImpl(get(), get()) }
        viewModel<SetupBatteryOptimisationViewModel> { SetupBatteryOptimisationViewModelImpl(get(), get()) }
        viewModel<SetupCompleteViewModel> { SetupCompleteViewModelImpl(get(), get(), get()) }
        viewModel<BackupRestoreViewModel> { BackupRestoreViewModelImpl(get()) }
        viewModel<BackupRestoreBackupViewModel> { BackupRestoreBackupViewModelImpl(get(), get()) }
        viewModel<BackupRestoreOptionsViewModel> { BackupRestoreOptionsViewModelImpl(get()) }
        viewModel<BackupRestoreRestoreViewModel> { BackupRestoreRestoreViewModelImpl(get(), get()) }
        viewModel<ContributorsViewModel> { ContributorsViewModelImpl(get()) }
        viewModel<BatteryOptimisationViewModel> { BatteryOptimisationViewModelImpl(get(), get(), get()) }
    }

    private val tracklistModule = module {
        viewModel<TracklistViewModel> { TracklistViewModelImpl(get(), get()) }
        scope<TracklistViewModel> {
            scoped<ShardsListRepository> { ShardsListRepositoryImpl(get(), get(), this) }
            viewModel<TracklistTracksViewModel> { TracklistTracksViewModelImpl(get()) }
            viewModel<TracklistArtistsViewModel> { TracklistArtistsViewModelImpl(get(), get()) }
            viewModel<TracklistArtistTracksViewModel> { TracklistArtistTracksViewModelImpl(get(), get()) }
        }
    }

    override fun attachBaseContext(base: Context) {
        HiddenApiBypass.addHiddenApiExemptions("")
        startKoin {
            androidContext(base)
            modules(repositoriesModule, viewModelsModule, tracklistModule)
        }
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        setupMonet()
    }

    private fun createMarkwon(): Markwon {
        val typeface = ResourcesCompat.getFont(this, R.font.google_sans_text_medium)
        return Markwon.builder(this).usePlugin(object: AbstractMarkwonPlugin() {
            override fun configureTheme(builder: MarkwonTheme.Builder) {
                typeface?.let {
                    builder.headingTypeface(it)
                    builder.headingBreakHeight(0)
                }
            }
        }).build()
    }

    private fun setupMonet(){
        val settings = get<SettingsRepository>()
        MonetCompat.wallpaperColorPicker = {
            val selectedColor = settings.monetColor.getSync()
            if(selectedColor != Integer.MAX_VALUE && it?.contains(selectedColor) == true) selectedColor
            else it?.firstOrNull()
        }
    }

}

enum class Scopes {
    TRACK_LIST
}

const val PACKAGE_NAME_PAM = "com.kieronquinn.app.pixelambientmusic"
const val PACKAGE_NAME_GSB = "com.google.android.googlequicksearchbox"