package com.kieronquinn.app.ambientmusicmod.ui.screens.setup.countrypicker

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
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentSetupCountryPickerBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsRepository
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.countrypicker.SetupCountryPickerViewModel.SetupCountryPickerSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.countrypicker.SetupCountryPickerViewModel.SetupCountryPickerSettingsItem.Header
import com.kieronquinn.app.ambientmusicmod.ui.screens.setup.countrypicker.SetupCountryPickerViewModel.State
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getLegacyWorkaroundNavBarHeight
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isDarkMode
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onApplyInsets
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupCountryPickerFragment: BoundFragment<FragmentSetupCountryPickerBinding>(FragmentSetupCountryPickerBinding::inflate), BackAvailable {

    private val viewModel by viewModel<SetupCountryPickerViewModel>()

    private val adapter by lazy {
        SetupCountryPickerAdapter(binding.setupCountryPickerRecyclerview, emptyList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupMonet()
        setupInsets()
        setupNext()
        setupState()
    }

    private fun setupRecyclerView() = with(binding.setupCountryPickerRecyclerview){
        layoutManager = LinearLayoutManager(context)
        adapter = this@SetupCountryPickerFragment.adapter
    }

    private fun setupMonet() {
        binding.setupCountryPickerNext.applyMonet()
        val accent = monet.getAccentColor(requireContext())
        binding.setupCountryPickerNext.overrideRippleColor(accent)
        val background = monet.getPrimaryColor(requireContext(), !requireContext().isDarkMode)
        binding.setupCountryPickerControls.backgroundTintList = ColorStateList.valueOf(background)
        binding.setupCountryPickerLoadingProgress.applyMonet()
    }

    private fun setupInsets() {
        val standardPadding = resources.getDimension(R.dimen.margin_16).toInt()
        val legacyWorkaround = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            requireContext().getLegacyWorkaroundNavBarHeight()
        } else 0
        binding.setupCountryPickerControls.onApplyInsets { view, insets ->
            view.updatePadding(
                bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom +
                        standardPadding + legacyWorkaround
            )
        }
    }

    private fun setupNext() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.setupCountryPickerNext.onClicked().collect {
            viewModel.onNextClicked()
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
            is State.Loading -> {
                binding.setupCountryPickerLoading.isVisible = true
                binding.setupCountryPickerRecyclerview.isVisible = false
                binding.setupCountryPickerControls.isVisible = false
            }
            is State.Loaded -> {
                binding.setupCountryPickerLoading.isVisible = false
                binding.setupCountryPickerRecyclerview.isVisible = true
                binding.setupCountryPickerControls.isVisible = true
                adapter.update(loadItems(state), binding.setupCountryPickerRecyclerview)
            }
        }
    }

    private fun loadItems(state: State.Loaded): List<BaseSettingsItem> {
        val automatic = SetupCountryPickerSettingsItem.Country(
            null,
            state.selectedCountry == null,
            viewModel::onCountrySelected
        )
        val countries = ShardsRepository.ShardCountry.values().map {
            SetupCountryPickerSettingsItem.Country(
                it,
                state.selectedCountry == it,
                viewModel::onCountrySelected
            )
        }.sortedBy {
            getString(it.country!!.countryName).lowercase()
        }
        return listOf(Header, automatic) + countries
    }

}