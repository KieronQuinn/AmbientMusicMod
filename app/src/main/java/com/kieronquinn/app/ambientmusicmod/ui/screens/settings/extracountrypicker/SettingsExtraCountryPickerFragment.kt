package com.kieronquinn.app.ambientmusicmod.ui.screens.settings.extracountrypicker

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentSetupCountryPickerBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsRepository
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.extracountrypicker.SettingsExtraCountryPickerViewModel.SettingsExtraCountryPickerSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.extracountrypicker.SettingsExtraCountryPickerViewModel.SettingsExtraCountryPickerSettingsItem.Header
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.extracountrypicker.SettingsExtraCountryPickerViewModel.State
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getLegacyWorkaroundNavBarHeight
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isDarkMode
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onApplyInsets
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsExtraCountryPickerFragment: BoundFragment<FragmentSetupCountryPickerBinding>(FragmentSetupCountryPickerBinding::inflate), BackAvailable {

    private val viewModel by viewModel<SettingsExtraCountryPickerViewModel>()

    private val adapter by lazy {
        SettingsExtraCountryPickerAdapter(binding.setupCountryPickerRecyclerview, emptyList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupMonet()
        setupInsets()
        setupNext()
        setupState()
        setupError()
    }

    private fun setupRecyclerView() = with(binding.setupCountryPickerRecyclerview){
        layoutManager = LinearLayoutManager(context)
        adapter = this@SettingsExtraCountryPickerFragment.adapter
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
        val standardPadding = resources.getDimension(R.dimen.bottom_nav_height).toInt()
        val legacyWorkaround = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            requireContext().getLegacyWorkaroundNavBarHeight()
        } else 0
        binding.setupCountryPickerRecyclerview.onApplyInsets { view, insets ->
            view.updatePadding(
                bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom +
                        standardPadding + legacyWorkaround
            )
        }
    }

    private fun setupNext() {
        binding.setupCountryPickerNext.isVisible = false
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun setupError() = whenResumed {
        viewModel.errorBus.collect {
            Toast.makeText(
                requireContext(), R.string.extra_country_picker_error, Toast.LENGTH_LONG
            ).show()
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
        val countries = ShardsRepository.ShardCountry.values().filterNot {
            it == state.primaryCountry
        }.map {
            SettingsExtraCountryPickerSettingsItem.Country(
                it,
                state.selectedCountries.contains(it),
                viewModel::onCountrySelected
            )
        }.sortedBy {
            getString(it.country.countryName).lowercase()
        }
        return listOf(Header) + countries
    }

}