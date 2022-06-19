package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.generic

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.Scopes
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentTracklistGenericBinding
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.TracklistViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.generic.GenericTracklistViewModel.State
import com.kieronquinn.app.ambientmusicmod.utils.extensions.*
import com.kieronquinn.monetcompat.extensions.applyMonet
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.koin.core.component.KoinScopeComponent

abstract class GenericTracklistFragment<T>: BoundFragment<FragmentTracklistGenericBinding>(FragmentTracklistGenericBinding::inflate), KoinScopeComponent {

    override val scope by lazy {
        getKoin().getOrCreateScope<TracklistViewModel>(Scopes.TRACK_LIST.name)
    }

    abstract val viewModel: GenericTracklistViewModel<T>
    abstract val adapter: GenericTracklistAdapter<T>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        setupSearchClear()
        setupState()
        setupMonet()
    }

    private fun setupMonet() {
        binding.tracklistGenericLoadingProgress.applyMonet()
        binding.tracklistGenericSearch.searchBox.applyMonet()
        binding.tracklistGenericSearch.searchBox.backgroundTintList = ColorStateList.valueOf(
            monet.getBackgroundColorSecondary(requireContext()) ?: monet.getBackgroundColor(
                requireContext()
            )
        )
    }

    private fun setupRecyclerView() = with(binding.tracklistGenericRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@GenericTracklistFragment.adapter
        applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State<T>) {
        when(state){
            is State.Loading -> {
                binding.tracklistGenericLoading.isVisible = true
                binding.tracklistGenericRecyclerview.isVisible = false
                if(state.indeterminate){
                    binding.tracklistGenericLoadingProgress.isIndeterminate = true
                }else{
                    binding.tracklistGenericLoadingProgress.isIndeterminate = false
                    binding.tracklistGenericLoadingProgress.progress = state.progress
                }
            }
            is State.Loaded -> {
                binding.tracklistGenericLoading.isVisible = false
                binding.tracklistGenericRecyclerview.isVisible = true
                adapter.items = state.list
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupSearch() {
        setSearchText(viewModel.searchText.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            binding.tracklistGenericSearch.searchBox.onChanged().debounce(250L).collect {
                viewModel.setSearchText(it ?: "")
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            binding.tracklistGenericSearch.searchBox.onEditorActionSent(EditorInfo.IME_ACTION_SEARCH).collect {
                binding.tracklistGenericSearch.searchBox.hideIme()
            }
        }
    }

    private fun setupSearchClear() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        launch {
            viewModel.searchShowClear.collect {
                binding.tracklistGenericSearch.searchClear.isVisible = it
            }
        }
        launch {
            binding.tracklistGenericSearch.searchClear.onClicked().collect {
                setSearchText("")
            }
        }
    }

    private fun setSearchText(text: CharSequence) {
        binding.tracklistGenericSearch.searchBox.run {
            this.text?.let {
                it.clear()
                it.append(text)
            } ?: setText(text)
        }
    }

    protected fun onOnDemandClicked() {
        Snackbar.make(requireView(), R.string.tracklist_snackbar_ondemand, Snackbar.LENGTH_LONG)
            .applyMonet()
            .setTypeface(ResourcesCompat.getFont(requireContext(), R.font.google_sans_text_medium))
            .apply {
                view.applyBottomNavigationMargin()
            }
            .show()
    }

}