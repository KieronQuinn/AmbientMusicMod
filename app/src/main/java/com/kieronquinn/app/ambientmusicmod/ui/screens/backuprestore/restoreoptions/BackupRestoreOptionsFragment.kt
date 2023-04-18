package com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore.restoreoptions

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentBackupRestoreOptionsBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.backuprestore.restoreoptions.BackupRestoreOptionsViewModel.State
import com.kieronquinn.app.ambientmusicmod.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.ambientmusicmod.utils.extensions.applyBottomNavigationMarginShort
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import com.kieronquinn.app.ambientmusicmod.utils.monetcompat.MonetElevationOverlayProvider
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class BackupRestoreOptionsFragment: BoundFragment<FragmentBackupRestoreOptionsBinding>(FragmentBackupRestoreOptionsBinding::inflate), BackAvailable {

    private val viewModel by viewModel<BackupRestoreOptionsViewModel>()
    private val args by navArgs<BackupRestoreOptionsFragmentArgs>()

    private val adapter by lazy {
        BackupRestoreOptionsAdapter(binding.backupRestoreOptionsRecyclerView, emptyList())
    }

    private val elevationOverlayProvider by lazy {
        MonetElevationOverlayProvider(requireContext())
    }

    private val headerBackground by lazy {
        elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(
            resources.getDimension(R.dimen.bottom_nav_elevation)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupMonet()
        setupState()
        setupNext()
        setupInsets()
    }

    private fun setupRecyclerView() = with(binding.backupRestoreOptionsRecyclerView) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@BackupRestoreOptionsFragment.adapter
        applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
    }

    private fun setupInsets() {
        val regularPadding = resources.getDimension(R.dimen.margin_16)
        binding.backupRestoreOptionsRestore.applyBottomNavigationMarginShort(regularPadding)
        val fabMargin = resources.getDimension(R.dimen.fab_margin) +
                resources.getDimension(R.dimen.margin_16)
        binding.backupRestoreOptionsRecyclerView.applyBottomNavigationInset(fabMargin)
    }

    private fun setupMonet() {
        binding.backupRestoreOptionsRestore.applyMonet()
        val accent = monet.getAccentColor(requireContext())
        binding.backupRestoreOptionsRestore.overrideRippleColor(accent)
        binding.backupRestoreOptionsLoadingProgress.applyMonet()
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
            is State.Loading -> {
                binding.backupRestoreOptionsLoading.isVisible = true
                binding.backupRestoreOptionsLoaded.isVisible = false
            }
            is State.Loaded -> {
                binding.backupRestoreOptionsLoading.isVisible = false
                binding.backupRestoreOptionsLoaded.isVisible = true
                adapter.update(loadItems(state), binding.backupRestoreOptionsRecyclerView)
            }
        }
    }

    private fun loadItems(state: State.Loaded): List<BaseSettingsItem> = listOf(
        GenericSettingsItem.SwitchSetting(
            state.restoreFavourites,
            getString(R.string.backup_restore_restore_options_restore_favourites_title),
            getString(R.string.backup_restore_restore_options_restore_favourites_content),
            R.drawable.ic_backup_restore_options_favourites,
            onChanged = viewModel::onRestoreFavouritesChanged
        ),
        GenericSettingsItem.SwitchSetting(
            state.clearFavourites,
            getString(R.string.backup_restore_restore_options_restore_clear_favourites_title),
            getString(R.string.backup_restore_restore_options_restore_clear_favourites_content),
            R.drawable.ic_backup_restore_options_favourites_delete,
            onChanged = viewModel::onClearFavouritesChanged
        ),
        GenericSettingsItem.SwitchSetting(
            state.restoreHistory,
            getString(R.string.backup_restore_restore_options_restore_history_title),
            getString(R.string.backup_restore_restore_options_restore_history_content),
            R.drawable.ic_backup_restore_options_history,
            onChanged = viewModel::onRestoreHistoryChanged
        ),
        GenericSettingsItem.SwitchSetting(
            state.clearHistory,
            getString(R.string.backup_restore_restore_options_restore_clear_history_title),
            getString(R.string.backup_restore_restore_options_restore_clear_history_content),
            R.drawable.ic_backup_restore_options_history_delete,
            onChanged = viewModel::onClearHistoryChanged
        ),
        GenericSettingsItem.SwitchSetting(
            state.restoreLinear,
            getString(R.string.backup_restore_restore_options_restore_linear_title),
            getString(R.string.backup_restore_restore_options_restore_linear_content),
            R.drawable.ic_backup_restore_options_linear,
            onChanged = viewModel::onRestoreLinearChanged
        ),
        GenericSettingsItem.SwitchSetting(
            state.clearLinear,
            getString(R.string.backup_restore_restore_options_restore_clear_linear_title),
            getString(R.string.backup_restore_restore_options_restore_clear_linear_content),
            R.drawable.ic_backup_restore_options_linear_delete,
            onChanged = viewModel::onClearLinearChanged
        ),
        GenericSettingsItem.SwitchSetting(
            state.restoreSettings,
            getString(R.string.backup_restore_restore_options_restore_settings_title),
            getString(R.string.backup_restore_restore_options_restore_settings_content),
            R.drawable.ic_backup_restore_options_settings,
            onChanged = viewModel::onRestoreSettingsChanged
        )
    )

    private fun setupNext() = whenResumed {
        binding.backupRestoreOptionsRestore.onClicked().collect {
            viewModel.onNextClicked(args.uri)
        }
    }

}