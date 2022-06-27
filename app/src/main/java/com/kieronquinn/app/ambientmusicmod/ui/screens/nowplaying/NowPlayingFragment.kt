package com.kieronquinn.app.ambientmusicmod.ui.screens.nowplaying

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentNowPlayingBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository.SettingsState
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.ui.base.ProvidesOverflow
import com.kieronquinn.app.ambientmusicmod.ui.screens.nowplaying.NowPlayingViewModel.NowPlayingSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.screens.nowplaying.NowPlayingViewModel.State
import com.kieronquinn.app.ambientmusicmod.utils.extensions.*
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class NowPlayingFragment: BoundFragment<FragmentNowPlayingBinding>(FragmentNowPlayingBinding::inflate), ProvidesOverflow {

    private val viewModel by viewModel<NowPlayingViewModel>()

    private val adapter by lazy {
        NowPlayingAdapter(
            binding.nowPlayingRecyclerView,
            emptyList()
        )
    }

    private val fabMargin by lazy {
        resources.getDimension(R.dimen.bottom_nav_height_margins).toInt()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupState()
        setupMonet()
        setupFab()
        setupDialog()
    }

    private fun setupRecyclerView() = with(binding.nowPlayingRecyclerView){
        layoutManager = LinearLayoutManager(context)
        adapter = this@NowPlayingFragment.adapter
        val fabMargin = resources.getDimension(R.dimen.fab_margin) +
                resources.getDimension(R.dimen.margin_16)
        applyBottomNavigationInset(fabMargin)
    }

    private fun setupState(){
        handleState(viewModel.state.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun setupMonet() {
        binding.nowPlayingLoadingProgress.applyMonet()
    }

    private fun setupFab() = with(binding.fabNowplayingRecognise){
        backgroundTintList = ColorStateList.valueOf(monet.getPrimaryColor(context))
        val legacyWorkaround = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            context.getLegacyWorkaroundNavBarHeight()
        } else 0
        onApplyInsets { _, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            updateLayoutParams<ConstraintLayout.LayoutParams> {
                updateMargins(bottom = bottomInset + fabMargin + legacyWorkaround)
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            binding.nowPlayingRecyclerView.shouldShrinkFab().collect {
                if(it){
                    shrink()
                }else{
                    extend()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            binding.fabNowplayingRecognise.onClicked().collect {
                binding.fabNowplayingRecognise.collapse()
                viewModel.onRecogniseFabClicked()
            }
        }
    }

    private fun Context.getLegacyWorkaroundNavBarHeight(): Int {
        val resourceId: Int = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadSettings()
    }

    private fun handleState(state: State) {
        when(state) {
            is State.Loading -> {
                binding.fabNowplayingRecognise.isVisible = false
                binding.nowPlayingLoading.isVisible = true
                binding.nowPlayingError.isVisible = false
                binding.nowPlayingRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                when (state.settings) {
                    is SettingsState.Available -> {
                        val items = state.settings.getItems()
                        adapter.update(items, binding.nowPlayingRecyclerView)
                        if (state.settings.mainEnabled) {
                            binding.fabNowplayingRecognise.show()
                        } else {
                            binding.fabNowplayingRecognise.hide()
                        }
                        binding.nowPlayingLoading.isVisible = false
                        binding.nowPlayingRecyclerView.isVisible = true
                        binding.nowPlayingError.isVisible = false
                    }
                    is SettingsState.NoShizuku -> {
                        binding.fabNowplayingRecognise.isVisible = false
                        binding.nowPlayingLoading.isVisible = false
                        binding.nowPlayingError.isVisible = true
                        binding.nowPlayingRecyclerView.isVisible = false
                        binding.nowPlayingErrorLabel.setText(R.string.now_playing_error_shizuku)
                        binding.nowPlayingErrorLabelSub.setText(R.string.now_playing_error_shizuku_subtitle)
                    }
                    is SettingsState.NoPAM -> {
                        binding.fabNowplayingRecognise.isVisible = false
                        binding.nowPlayingLoading.isVisible = false
                        binding.nowPlayingError.isVisible = true
                        binding.nowPlayingRecyclerView.isVisible = false
                        binding.nowPlayingErrorLabel.setText(R.string.now_playing_error_api)
                        binding.nowPlayingErrorLabelSub.setText(R.string.now_playing_error_api_subtitle)
                    }
                    is SettingsState.NotSetup -> {
                        binding.fabNowplayingRecognise.isVisible = false
                        binding.nowPlayingLoading.isVisible = false
                        binding.nowPlayingError.isVisible = true
                        binding.nowPlayingRecyclerView.isVisible = false
                        binding.nowPlayingErrorLabel.setText(R.string.now_playing_error_generic)
                        binding.nowPlayingErrorLabelSub.setText(R.string.now_playing_error_generic_subtitle)
                    }
                }
            }
        }
    }

    private fun setupDialog() {
        val setAlpha = { visible: Boolean ->
            binding.fabNowplayingRecognise.alpha = if(visible) 0f else 1f
        }
        setAlpha(viewModel.recognitionDialogShowing.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.recognitionDialogShowing.collect {
                setAlpha(it)
            }
        }
    }

    private fun SettingsState.Available.getItems(): List<BaseSettingsItem> {
        val list = ArrayList<BaseSettingsItem>()
        val addFooter = {
            list.add(GenericSettingsItem.Setting(
                getString(R.string.faq_title),
                getString(R.string.faq_content),
                R.drawable.ic_faq,
                viewModel::onFaqClicked
            ))
            list.add(NowPlayingSettingsItem.Footer {
                viewModel.onFooterLinkClicked(requireContext().isDarkMode)
            })
        }
        list.add(NowPlayingSettingsItem.Header)
        bannerMessage?.let {
            list.add(NowPlayingSettingsItem.Banner(it, viewModel::onBannerButtonClicked))
        }
        list.add(GenericSettingsItem.Switch(
            mainEnabled,
            getString(R.string.item_nowplaying_header_switch),
            viewModel::onSwitchClicked
        ))
        if(!mainEnabled) return list.also { addFooter() }
        list.add(GenericSettingsItem.Setting(
            getString(R.string.item_nowplaying_notifications_title),
            notificationContent(),
            R.drawable.ic_nowplaying_notifications,
            viewModel::onNotificationsClicked
        ))
        list.add(GenericSettingsItem.Setting(
            getString(R.string.item_nowplaying_lockscreen_title),
            getString(R.string.item_nowplaying_lockscreen_content),
            R.drawable.ic_nowplaying_lock_screen,
            viewModel::onLockscreenClicked
        ))
        list.add(GenericSettingsItem.Setting(
            getString(R.string.item_nowplaying_ondemand_title),
            getString(R.string.item_nowplaying_ondemand_content),
            R.drawable.ic_nowplaying_ondemand,
            viewModel::onOnDemandClicked
        ))
        list.add(GenericSettingsItem.Setting(
            getString(R.string.item_nowplaying_history_title),
            historyContent(),
            R.drawable.ic_nowplaying_history,
            viewModel::onHistoryClicked
        ))
        list.add(GenericSettingsItem.Setting(
            getString(R.string.item_nowplaying_settings_title),
            getString(R.string.item_nowplaying_settings_content),
            R.drawable.ic_nowplaying_settings,
            viewModel::onSettingsClicked
        ))
        list.add(GenericSettingsItem.Setting(
            getString(R.string.backup_restore_title),
            getString(R.string.backup_restore_content),
            R.drawable.ic_backup_restore,
            viewModel::onBackupRestoreClicked
        ))
        addFooter()
        return list
    }

    private fun SettingsState.Available.notificationContent(): String {
        return if(notificationsEnabled){
            getString(R.string.item_nowplaying_notifications_content_enabled)
        }else{
            getString(R.string.item_nowplaying_notifications_content_disabled)
        }
    }

    private fun SettingsState.Available.historyContent(): String {
        return lastRecognisedSong?.let {
            val ago = DateUtils.getRelativeTimeSpanString(
                it.timestamp, System.currentTimeMillis(), 0
            )
            getString(R.string.item_nowplaying_history_content, it.track, it.artist, ago)
        } ?: getString(R.string.item_nowplaying_history_content_empty)
    }

    override fun inflateMenu(menuInflater: MenuInflater, menu: Menu) {
        //Only show wallpaper colour picker on < S
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            menuInflater.inflate(R.menu.menu_now_playing, menu)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when(menuItem.itemId){
            R.id.menu_wallpaper_colour_picker -> viewModel.onWallpaperColourPickerClicked()
        }
        return true
    }

}