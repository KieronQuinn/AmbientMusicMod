package com.kieronquinn.app.ambientmusicmod.ui.activities

import android.os.Bundle
import android.view.View
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.work.WorkManager
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.repositories.JobsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.utils.extensions.delayPreDrawUntilFlow
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenCreated
import com.kieronquinn.app.ambientmusicmod.work.PeriodicBackupWorker
import com.kieronquinn.app.ambientmusicmod.work.UpdateWorker
import com.kieronquinn.monetcompat.app.MonetCompatActivity
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity: MonetCompatActivity() {

    companion object {
        const val EXTRA_SKIP_SPLASH = "SKIP_SPLASH"
    }

    override val applyBackgroundColorToWindow = true

    private val viewModel by viewModel<MainActivityViewModel>()
    private val jobsRepository by inject<JobsRepository>()
    private val settings by inject<SettingsRepository>()

    private val splashDelay by lazy {
        viewModel.startDestination.filterNotNull().map { true }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if(!intent.getBooleanExtra(EXTRA_SKIP_SPLASH, false)) {
            findViewById<View>(android.R.id.content)
                .delayPreDrawUntilFlow(splashDelay, lifecycle, this)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        whenCreated {
            monet.awaitMonetReady()
            setContentView(R.layout.activity_main)
            queueWork()
        }
    }

    private suspend fun queueWork() {
        UpdateWorker.queueWorker(this)
        PeriodicBackupWorker.enqueueOrCancelWorker(
            WorkManager.getInstance(this),
            settings.periodicBackupEnabled.get(),
            settings.periodicBackupInterval.get()
        )
    }

    override fun onResume() {
        super.onResume()
        jobsRepository.setShouldExpediteJobs(true)
    }

    override fun onPause() {
        jobsRepository.setShouldExpediteJobs(false)
        super.onPause()
    }

}