package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.ownerinfo

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.ownerinfo.LockScreenOwnerInfoViewModel.LockScreenOwnerInfoSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.ownerinfo.LockScreenOwnerInfoViewModel.State
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class LockScreenOwnerInfoFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<LockScreenOwnerInfoViewModel>()

    override val adapter by lazy {
        LockScreenOwnerInfoAdapter(binding.settingsBaseRecyclerView, emptyList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
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
                binding.settingsBaseLoading.isVisible = true
                binding.settingsBaseRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                binding.settingsBaseLoading.isVisible = false
                binding.settingsBaseRecyclerView.isVisible = true
                adapter.update(loadItems(state), binding.settingsBaseRecyclerView)
            }
        }
    }

    private fun loadItems(state: State.Loaded): List<BaseSettingsItem> {
        val header = LockScreenOwnerInfoSettingsItem.Header(state.showNote)
        if(!state.compatible){
            return listOf(
                header,
                LockScreenOwnerInfoSettingsItem.Banner,
                LockScreenOwnerInfoSettingsItem.Footer
            )
        }
        val switch = GenericSettingsItem.Switch(
            state.enabled,
            getString(R.string.lockscreen_owner_info_enabled),
            viewModel::onSwitchChanged
        )
        if(!state.enabled){
            return listOf(
                header,
                switch,
                LockScreenOwnerInfoSettingsItem.Footer
            )
        }
        val showNote = GenericSettingsItem.SwitchSetting(
            state.showNote,
            getString(R.string.lockscreen_owner_info_show_note),
            getString(R.string.lockscreen_owner_info_show_note_content),
            R.drawable.ic_lockscreen_owner_info_show_note,
            onChanged = viewModel::onShowNoteChanged
        )
        val ownerInfoSetting = GenericSettingsItem.Setting(
            getString(R.string.lockscreen_owner_info_fallback),
            state.fallbackInfo.ifEmpty { getString(R.string.lockscreen_owner_info_fallback_content_empty) },
            R.drawable.ic_lock_screen_owner_info,
            onClick = viewModel::onFallbackClicked
        )
        return listOf(
            header,
            switch,
            showNote,
            ownerInfoSetting,
            LockScreenOwnerInfoSettingsItem.Footer
        )
    }

}