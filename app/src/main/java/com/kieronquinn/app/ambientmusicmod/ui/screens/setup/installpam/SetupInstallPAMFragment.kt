package com.kieronquinn.app.ambientmusicmod.ui.screens.setup.installpam

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentSetupInstallPamBinding
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.installpam.SetupInstallPAMViewModel.State
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.roundToInt

class SetupInstallPAMFragment: BoundFragment<FragmentSetupInstallPamBinding>(FragmentSetupInstallPamBinding::inflate), BackAvailable {

    private val viewModel by viewModel<SetupInstallPAMViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupMonet()
        setupRetry()
        setupInstall()
    }

    private fun setupMonet() = with(binding) {
        setupInstallPamLoadingProgress.applyMonet()
        val accent = monet.getAccentColor(requireContext())
        setupInstallPamRetry.run {
            setTextColor(accent)
            iconTint = ColorStateList.valueOf(accent)
            overrideRippleColor(accent)
        }
        setupInstallPamInstallStart.run {
            setTextColor(accent)
            iconTint = ColorStateList.valueOf(accent)
            overrideRippleColor(accent)
        }
    }

    private fun setupRetry() = whenResumed {
        binding.setupInstallPamRetry.onClicked().collect {
            viewModel.restartDownload()
        }
    }

    private fun setupInstall() = whenResumed {
        binding.setupInstallPamInstallStart.onClicked().collect {
            viewModel.onInstallClicked()
        }
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) {
        when(state){
            is State.Loading -> showLoading()
            is State.Downloading -> showDownloading(state)
            is State.Error -> showError()
            is State.DownloadComplete -> showDownloadComplete()
            is State.Installed -> {
                showLoading()
                viewModel.moveToNext()
            }
        }
    }

    private fun showLoading() = with(binding) {
        setupInstallPamLoading.isVisible = true
        setupInstallPamError.isVisible = false
        setupInstallPamInstall.isVisible = false
        setupInstallPamLoadingProgress.isIndeterminate = true
        setupInstallPamLoadingLabel.setText(R.string.setup_install_pam_loading)
        setupInstallPamLoadingSubtitle.text = ""
    }

    private fun showDownloading(downloading: State.Downloading) = with(binding) {
        setupInstallPamLoading.isVisible = true
        setupInstallPamError.isVisible = false
        setupInstallPamInstall.isVisible = false
        //Show indeterminate for values that would be invisible on the ProgressBar
        setupInstallPamLoadingProgress.isIndeterminate = downloading.progress <= 1.0
        setupInstallPamLoadingProgress.progress = downloading.progress.roundToInt()
        setupInstallPamLoadingLabel.setText(R.string.setup_install_pam_downloading)
        setupInstallPamLoadingSubtitle.setText(R.string.setup_install_pam_downloading_subtitle)
    }

    private fun showError() = with(binding) {
        setupInstallPamLoading.isVisible = false
        setupInstallPamError.isVisible = true
        setupInstallPamInstall.isVisible = false
    }

    private fun showDownloadComplete() = with(binding) {
        setupInstallPamLoading.isVisible = false
        setupInstallPamError.isVisible = false
        setupInstallPamInstall.isVisible = true
    }

}