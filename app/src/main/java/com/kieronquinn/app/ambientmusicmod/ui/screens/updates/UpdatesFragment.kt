package com.kieronquinn.app.ambientmusicmod.ui.screens.updates

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentUpdatesBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.updates.UpdatesViewModel.State
import com.kieronquinn.app.ambientmusicmod.ui.screens.updates.UpdatesViewModel.UpdatesSettingsItem
import com.kieronquinn.app.ambientmusicmod.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class UpdatesFragment: BoundFragment<FragmentUpdatesBinding>(FragmentUpdatesBinding::inflate) {

    private val viewModel by viewModel<UpdatesViewModel>()

    private val adapter by lazy {
        UpdatesAdapter(binding.updatesRecyclerView, emptyList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupRecyclerView()
        setupLoading()
        setupSwipeRefresh()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
    }

    private fun setupSwipeRefresh() = with(binding.updatesSwipeRefresh) {
        setColorSchemeColors(monet.getAccentColor(context))
        setOnRefreshListener {
            viewModel.reload(true)
        }
    }

    private fun setupRecyclerView() = with(binding.updatesRecyclerView) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@UpdatesFragment.adapter
        applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
    }

    private fun setupLoading() = with(binding.updatesLoadingProgress) {
        applyMonet()
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
                binding.updatesLoading.isVisible = true
                binding.updatesRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                binding.updatesSwipeRefresh.isRefreshing = false
                binding.updatesLoading.isVisible = false
                binding.updatesRecyclerView.isVisible = true
                adapter.update(loadItems(state), binding.updatesRecyclerView)
            }
        }
    }

    private fun loadItems(state: State.Loaded): List<BaseSettingsItem> = listOf(
        UpdatesSettingsItem.Shards(
            state.shardsState,
            viewModel::onShardsUpdateClicked,
            viewModel::onShardsViewTracksClicked,
            viewModel::onCountryClicked
        ),
        UpdatesSettingsItem.AMM(state.ammState){
           viewModel.onAMMUpdateClicked(getString(R.string.updates_amm_title), it)
        },
        UpdatesSettingsItem.PAM(state.pamState) {
            viewModel.onPAMUpdateClicked(getString(R.string.updates_pam_title), it)
        },
        GenericSettingsItem.SwitchSetting(
            state.automaticDatabaseUpdates,
            getString(R.string.updates_automatic_database_downloads),
            getString(R.string.updates_automatic_database_downloads_content),
            R.drawable.ic_updates_automatic_database_updates,
            onChanged = viewModel::onAutomaticDatabaseUpdatesChanged
        ),
        UpdatesSettingsItem.About(
            viewModel::onContributorsClicked,
            viewModel::onDonateClicked,
            viewModel::onGitHubClicked,
            viewModel::onTwitterClicked,
            viewModel::onXdaClicked,
            viewModel::onLibrariesClicked
        )
    )

}