package com.kieronquinn.app.ambientmusicmod.ui.screens.setup.datausage

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentSetupDataUsageBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.ui.base.ProvidesBack
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.datausage.SetupDataUsageViewModel.State
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getLegacyWorkaroundNavBarHeight
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isDarkMode
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onApplyInsets
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupDataUsageFragment: BoundFragment<FragmentSetupDataUsageBinding>(FragmentSetupDataUsageBinding::inflate), BackAvailable, ProvidesBack {

    private val viewModel by viewModel<SetupDataUsageViewModel>()

    private val adapter by lazy {
        SetupDataUsageAdapter(binding.setupDataUsageSettings, emptyList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupControls()
        setupMonet()
        setupInsets()
        setupState()
        setupNext()
    }

    private fun setupRecyclerView() = with(binding.setupDataUsageSettings){
        layoutManager = LinearLayoutManager(context)
        adapter = this@SetupDataUsageFragment.adapter
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
                binding.setupDataUsageLoading.isVisible = true
                binding.setupDataUsageScrollable.isVisible = false
                binding.setupDataUsageControls.isVisible = false
            }
            is State.Loaded -> {
                binding.setupDataUsageLoading.isVisible = false
                binding.setupDataUsageScrollable.isVisible = true
                binding.setupDataUsageControls.isVisible = true
                adapter.update(loadItems(state), binding.setupDataUsageSettings)
            }
        }
    }

    private fun loadItems(state: State.Loaded): List<BaseSettingsItem> {
        return listOf(
            GenericSettingsItem.SwitchSetting(
                state.superpacksRequireWiFi,
                getString(R.string.settings_advanced_superpacks_require_wifi_short),
                getText(R.string.settings_advanced_superpacks_require_wifi_content_short),
                R.drawable.ic_advanced_require_wifi,
                onChanged = viewModel::setSuperpacksRequireWiFi
            ),
            GenericSettingsItem.SwitchSetting(
                state.superpacksRequireCharging,
                getString(R.string.settings_advanced_superpacks_require_charging_short),
                getString(R.string.settings_advanced_superpacks_require_charging_content_short),
                R.drawable.ic_advanced_require_charging,
                onChanged = viewModel::setSuperpacksRequireCharging
            )
        )
    }

    private fun setupControls() {
        handleControls(viewModel.fabState.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.fabState.collect {
                handleControls(it)
            }
        }
    }

    private fun setupMonet() {
        binding.setupDataUsageNext.applyMonet()
        val accent = monet.getAccentColor(requireContext())
        binding.setupDataUsageNext.overrideRippleColor(accent)
        val background = monet.getPrimaryColor(requireContext(), !requireContext().isDarkMode)
        binding.setupDataUsageControls.backgroundTintList = ColorStateList.valueOf(background)
        binding.setupDataUsageLoadingProgress.applyMonet()
    }

    private fun setupInsets() {
        val standardPadding = resources.getDimension(R.dimen.margin_16).toInt()
        val legacyWorkaround = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            requireContext().getLegacyWorkaroundNavBarHeight()
        } else 0
        binding.setupDataUsageControls.onApplyInsets { view, insets ->
            view.updatePadding(
                bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom +
                        standardPadding + legacyWorkaround
            )
        }
    }

    private fun handleControls(fabState: SetupDataUsageViewModel.FabState?) {
        when(fabState){
            null -> {
                binding.setupDataUsageNext.isEnabled = false
                binding.setupDataUsageNext.alpha = 0.5f
                binding.setupDataUsageWarning.isVisible = false
            }
            else -> {
                binding.setupDataUsageNext.isEnabled = true
                binding.setupDataUsageNext.alpha = 1f
                binding.setupDataUsageWarning.isVisible = true
                fabState.warning?.let {
                    binding.setupDataUsageNext.isEnabled = false
                    binding.setupDataUsageNext.alpha = 0.5f
                    binding.setupDataUsageWarning.setText(it)
                } ?: run {
                    binding.setupDataUsageNext.isEnabled = true
                    binding.setupDataUsageNext.alpha = 1f
                    binding.setupDataUsageWarning.text = null
                }
            }
        }
    }

    private fun setupNext() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.setupDataUsageNext.onClicked().collect {
            viewModel.onNextClicked()
        }
    }

    override fun onBackPressed(): Boolean {
        //Pop back to landing rather than Shizuku
        viewModel.onBackPressed()
        return true
    }

}