package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.monet

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.monet.LockScreenCustomTextColourMonetViewModel.State
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class LockScreenCustomTextColourMonetFragment: BaseSettingsFragment() {

    override val addAdditionalPadding = true
    override val disableNestedScrolling = true

    private val viewModel by viewModel<LockScreenCustomTextColourMonetViewModel>()

    override val adapter by lazy {
        LockScreenCustomTextColourMonetAdapter(
            binding.settingsBaseRecyclerView, emptyList(), viewModel::onColourClicked
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupState()
    }

    private fun setupMonet() {
        binding.settingsBaseLoadingProgress.applyMonet()
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
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
                adapter.update(state.colours, binding.settingsBaseRecyclerView)
            }
        }
    }

}