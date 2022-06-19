package com.kieronquinn.app.ambientmusicmod.ui.screens.settings.recognitionperiod

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository.RecognitionPeriod
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.recognitionperiod.SettingsRecognitionPeriodViewModel.SettingsRecognitionPeriodSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.screens.settings.recognitionperiod.SettingsRecognitionPeriodViewModel.State
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsRecognitionPeriodFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<SettingsRecognitionPeriodViewModel>()

    private val googleSans by lazy {
        ResourcesCompat.getFont(requireContext(), R.font.google_sans_text)
    }

    override val adapter by lazy {
        SettingsRecognitionPeriodAdapter(binding.settingsBaseRecyclerView, emptyList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
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
                adapter.update(loadItems(state), binding.settingsBaseRecyclerView)
            }
        }
    }

    private fun loadItems(state: State.Loaded): List<BaseSettingsItem> {
        val switch = GenericSettingsItem.Switch(
            state.adaptiveEnabled,
            getSwitchTitle(),
            viewModel::onAutomaticChanged
        )
        val items = RecognitionPeriod.values().map {
            SettingsRecognitionPeriodSettingsItem.Period(
                it,
                it == state.period,
                viewModel::onPeriodSelected
            )
        }
        return listOf(switch) + items
    }

    private fun getSwitchTitle(): CharSequence {
        val sizeSpan = RelativeSizeSpan(0.75f)
        val fontSpan = TypefaceSpan(googleSans ?: Typeface.DEFAULT)
        val secondLine = SpannableString(
            getString(R.string.settings_recognition_period_adaptive_content)
        ).apply {
            setSpan(sizeSpan, 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            setSpan(fontSpan, 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        }
        return SpannableStringBuilder().apply {
            appendLine(getString(R.string.settings_recognition_period_adaptive))
            append(secondLine)
        }
    }

}