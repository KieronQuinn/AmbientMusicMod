package com.kieronquinn.app.ambientmusicmod.ui.screens.setup.permissions

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentSetupPermissionsBinding
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.permissions.SetupPermissionsViewModel.State
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getLegacyWorkaroundNavBarHeight
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isDarkMode
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onApplyInsets
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupPermissionsFragment: BoundFragment<FragmentSetupPermissionsBinding>(FragmentSetupPermissionsBinding::inflate), BackAvailable {

    private val viewModel by viewModel<SetupPermissionsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupMonet()
        setupGrant()
        setupInsets()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
    }

    private fun setupMonet() {
        binding.setupPermissionsGrant.applyMonet()
        val accent = monet.getAccentColor(requireContext())
        binding.setupPermissionsGrant.overrideRippleColor(accent)
        val background = monet.getPrimaryColor(requireContext(), !requireContext().isDarkMode)
        binding.setupPermissionsControls.backgroundTintList = ColorStateList.valueOf(background)
        binding.setupPermissionsLoadingProgress.applyMonet()
    }

    private fun setupInsets() {
        val standardPadding = resources.getDimension(R.dimen.margin_16).toInt()
        val legacyWorkaround = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            requireContext().getLegacyWorkaroundNavBarHeight()
        } else 0
        binding.setupPermissionsControls.onApplyInsets { view, insets ->
            view.updatePadding(
                bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom +
                        standardPadding + legacyWorkaround
            )
        }
    }

    private fun setupGrant() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.setupPermissionsGrant.onClicked().collect {
            viewModel.showPermissionPrompt()
        }
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
            State.Loading -> {
                binding.setupPermissionsLoading.isVisible = true
                binding.setupPermissionsScrollable.isVisible = false
                binding.setupPermissionsControls.isVisible = false
            }
            State.Request -> {
                binding.setupPermissionsLoading.isVisible = false
                binding.setupPermissionsScrollable.isVisible = true
                binding.setupPermissionsControls.isVisible = true
            }
            State.Granted -> {
                binding.setupPermissionsLoading.isVisible = true
                binding.setupPermissionsScrollable.isVisible = false
                binding.setupPermissionsControls.isVisible = false
                viewModel.moveToNext()
            }
        }
    }

}