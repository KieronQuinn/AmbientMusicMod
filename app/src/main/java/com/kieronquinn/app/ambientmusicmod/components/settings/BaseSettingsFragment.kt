package com.kieronquinn.app.ambientmusicmod.components.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceFragmentCompat
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.container.AmbientContainerSharedViewModel
import com.kieronquinn.app.ambientmusicmod.app.ui.preferences.Preference
import com.kieronquinn.app.ambientmusicmod.app.ui.preferences.PreferenceCategory
import com.kieronquinn.app.ambientmusicmod.app.ui.preferences.SwitchPreference
import com.kieronquinn.app.ambientmusicmod.components.AmbientSharedPreferences
import com.kieronquinn.app.ambientmusicmod.utils.extensions.ReadOnlyProperty
import kotlinx.coroutines.flow.collect
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import kotlin.reflect.KFunction0

abstract class BaseSettingsFragment: PreferenceFragmentCompat(), ScrollableFragment {

    abstract val viewModel: BaseViewModel
    val sharedPrefs by inject<AmbientSharedPreferences>()
    private val containerViewModel by sharedViewModel<AmbientContainerSharedViewModel>()

    private val bottomPadding by lazy {
        resources.getDimension(R.dimen.activity_padding) + resources.getDimension(R.dimen.padding_8) * 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager.sharedPreferencesName = BuildConfig.APPLICATION_ID + "_prefs"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.overScrollMode = View.OVER_SCROLL_NEVER
        listView.isVerticalScrollBarEnabled = false
        listView.clipToPadding = false
        lifecycleScope.launchWhenCreated {
            containerViewModel.mainAppLinkHeight.collect {
                listView?.updatePadding(bottom = bottomPadding.toInt() + it.toInt())
            }
        }
    }

    override fun scrollToTop() {
        listView?.scrollToPosition(0)
    }

    fun preference(key: String) = ReadOnlyProperty {
        findPreference<Preference>(key)
    }

    fun switchPreference(key: String) = ReadOnlyProperty {
        findPreference<SwitchPreference>(key)
    }

    fun preferenceCategory(key: String) = ReadOnlyProperty {
        findPreference<PreferenceCategory>(key)
    }

    fun androidx.preference.Preference.setOnClickListener(onClick: KFunction0<Unit>){
        setOnPreferenceClickListener {
            onClick.invoke()
            true
        }
    }

}