package com.kieronquinn.app.ambientmusicmod.ui.screens.updates.download

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentUpdatesDownloadFragmentBinding
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.ui.base.ProvidesTitle
import com.kieronquinn.app.ambientmusicmod.ui.screens.updates.download.UpdatesDownloadViewModel.State
import com.kieronquinn.app.ambientmusicmod.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.ambientmusicmod.utils.extensions.applyBottomNavigationMargin
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isDarkMode
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import io.noties.markwon.Markwon
import kotlinx.coroutines.flow.collect
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.roundToInt

class UpdatesDownloadFragment: BoundFragment<FragmentUpdatesDownloadFragmentBinding>(FragmentUpdatesDownloadFragmentBinding::inflate), BackAvailable, ProvidesTitle {

    private val viewModel by viewModel<UpdatesDownloadViewModel>()
    private val args by navArgs<UpdatesDownloadFragmentArgs>()
    private val markwon by inject<Markwon>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupStartInstall()
        setupGitHubButton()
        setupFabState()
        setupFabClick()
        setupInsets()
        setupMonet()
        viewModel.setupWithRelease(args.release)
    }

    private fun setupMonet() {
        val accent = monet.getAccentColor(requireContext())
        binding.updatesDownloadCard.backgroundTintList = ColorStateList.valueOf(
            monet.getPrimaryColor(requireContext(), !requireContext().isDarkMode)
        )
        binding.updatesDownloadStartInstall.setTextColor(accent)
        binding.updatesDownloadStartInstall.overrideRippleColor(accent)
        binding.updatesDownloadProgress.applyMonet()
        binding.updatesDownloadProgressIndeterminate.applyMonet()
        binding.updatesDownloadIcon.imageTintList = ColorStateList.valueOf(accent)
        binding.updatesDownloadDownloadBrowser.setTextColor(accent)
        binding.updatesDownloadFab.backgroundTintList =
            ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
    }

    private fun setupInsets() {
        binding.updatesDownloadInfo.applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
        binding.updatesDownloadFab.applyBottomNavigationMargin()
    }

    private fun setupStartInstall() = whenResumed {
        binding.updatesDownloadStartInstall.onClicked().collect {
            viewModel.startInstall()
        }
    }

    private fun setupGitHubButton() = whenResumed {
        binding.updatesDownloadDownloadBrowser.onClicked().collect {
            viewModel.onDownloadBrowserClicked(args.release.gitHubUrl)
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

    private fun handleState(state: State){
        when(state){
            is State.Loading -> setupWithLoading()
            is State.Info -> setupWithInfo(state)
            is State.StartDownload -> setupWithStartDownload()
            is State.Downloading -> setupWithDownloading(state)
            is State.Done, is State.StartInstall -> setupWithDone()
            is State.Failed -> setupWithFailed()
        }
    }

    private fun setupWithLoading() {
        binding.updatesDownloadInfo.isVisible = false
        binding.updatesDownloadProgress.isVisible = false
        binding.updatesDownloadProgressIndeterminate.isVisible = true
        binding.updatesDownloadTitle.isVisible = true
        binding.updatesDownloadIcon.isVisible = false
        binding.updatesDownloadStartInstall.isVisible = false
        binding.updatesDownloadTitle.setText(R.string.updates_download_loading)
    }

    private fun setupWithInfo(info: State.Info){
        val release = info.release
        binding.updatesDownloadInfo.isVisible = true
        binding.updatesDownloadProgress.isVisible = false
        binding.updatesDownloadProgressIndeterminate.isVisible = false
        binding.updatesDownloadTitle.isVisible = false
        binding.updatesDownloadIcon.isVisible = false
        binding.updatesDownloadStartInstall.isVisible = false
        binding.updatesDownloadHeading.text = getString(R.string.updates_download_heading, release.title, release.versionName)
        binding.updatesDownloadSubheading.text = getString(R.string.updates_download_subheading, release.installedVersion)
        binding.updatesDownloadSubheading.isVisible = !release.installedVersion.isNullOrEmpty()
        binding.updatesDownloadBody.text = markwon.toMarkdown(release.body)
        binding.updatesDownloadInfo.applyBottomNavigationInset()
        whenResumed {
            binding.updatesDownloadDownloadBrowser.onClicked().collect {
                viewModel.onDownloadBrowserClicked(release.gitHubUrl)
            }
        }
    }

    private fun setupWithStartDownload() {
        binding.updatesDownloadInfo.isVisible = false
        binding.updatesDownloadProgress.isVisible = false
        binding.updatesDownloadProgressIndeterminate.isVisible = true
        binding.updatesDownloadTitle.isVisible = true
        binding.updatesDownloadIcon.isVisible = false
        binding.updatesDownloadStartInstall.isVisible = false
        binding.updatesDownloadTitle.setText(R.string.update_downloader_downloading_title)
    }

    private fun setupWithDownloading(state: State.Downloading) {
        binding.updatesDownloadInfo.isVisible = false
        binding.updatesDownloadProgress.isVisible = true
        binding.updatesDownloadProgressIndeterminate.isVisible = false
        binding.updatesDownloadTitle.isVisible = true
        binding.updatesDownloadIcon.isVisible = false
        binding.updatesDownloadStartInstall.isVisible = false
        binding.updatesDownloadProgress.progress = (state.progress * 100).roundToInt()
        binding.updatesDownloadTitle.setText(R.string.update_downloader_downloading_title)
    }

    private fun setupWithDone() {
        binding.updatesDownloadInfo.isVisible = false
        binding.updatesDownloadProgress.isVisible = false
        binding.updatesDownloadProgressIndeterminate.isVisible = false
        binding.updatesDownloadTitle.isVisible = true
        binding.updatesDownloadIcon.isVisible = true
        binding.updatesDownloadStartInstall.isVisible = true
        binding.updatesDownloadTitle.setText(R.string.updates_download_done)
        binding.updatesDownloadIcon.setImageResource(R.drawable.ic_update_download_done)
    }

    private fun setupWithFailed() {
        binding.updatesDownloadInfo.isVisible = false
        binding.updatesDownloadProgress.isVisible = false
        binding.updatesDownloadProgressIndeterminate.isVisible = false
        binding.updatesDownloadTitle.isVisible = true
        binding.updatesDownloadIcon.isVisible = true
        binding.updatesDownloadStartInstall.isVisible = true
        binding.updatesDownloadTitle.setText(R.string.updates_download_failed)
        binding.updatesDownloadIcon.setImageResource(R.drawable.ic_error_circle)
    }

    private fun setupFabState() {
        handleFabState(viewModel.showFab.value)
        whenResumed {
            viewModel.showFab.collect {
                handleFabState(it)
            }
        }
    }

    private fun handleFabState(showFab: Boolean){
        binding.updatesDownloadFab.isVisible = showFab
    }

    private fun setupFabClick() = whenResumed {
        binding.updatesDownloadFab.onClicked().collect {
            viewModel.startDownload()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        viewModel.onPause()
        super.onPause()
    }

    override fun getTitle(): CharSequence? {
        return args.release.title
    }

}