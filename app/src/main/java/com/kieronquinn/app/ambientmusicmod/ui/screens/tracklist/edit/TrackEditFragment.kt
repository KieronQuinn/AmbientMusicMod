package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.edit

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.Scopes
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentTrackEditBinding
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.ui.base.ProvidesBack
import com.kieronquinn.app.ambientmusicmod.ui.base.ProvidesOverflow
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.TracklistViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.edit.TrackEditViewModel.State
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onApplyInsets
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onChanged
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent

class TrackEditFragment: BoundFragment<FragmentTrackEditBinding>(FragmentTrackEditBinding::inflate), KoinScopeComponent, ProvidesOverflow, ProvidesBack {

    override val scope by lazy {
        getKoin().getOrCreateScope<TracklistViewModel>(Scopes.TRACK_LIST.name)
    }

    private val viewModel by viewModel<TrackEditViewModel>()
    private val args by navArgs<TrackEditFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupInsets()
        setupTrackName()
        setupArtist()
        setupSave()
        setupState()
        setupError()
        viewModel.setupWithTrack(args.track)
    }

    override fun inflateMenu(menuInflater: MenuInflater, menu: Menu) {
        menuInflater.inflate(R.menu.menu_track_edit, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        viewModel.onDeleteClicked()
        return true
    }

    override fun onBackPressed(): Boolean {
        viewModel.onBackPressed()
        return true
    }

    private fun setupMonet() {
        binding.trackEditTrackName.applyMonet()
        binding.trackEditTrackNameEdit.applyMonet()
        binding.trackEditArtist.applyMonet()
        binding.trackEditArtistEdit.applyMonet()
        binding.trackEditArtistSave.backgroundTintList =
            ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
    }

    private fun setupInsets() = with(binding.root) {
        val padding = resources.getDimensionPixelSize(R.dimen.bottom_nav_height_margins)
        onApplyInsets { view, insets ->
            val bottomInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottomInsets + padding)
        }
    }

    private fun setupTrackName() = with(binding.trackEditTrackNameEdit) {
        whenResumed {
            onChanged().collect {
                viewModel.setTrackName(it?.toString() ?: "")
            }
        }
    }

    private fun setupArtist() = with(binding.trackEditArtistEdit) {
        whenResumed {
            onChanged().collect {
                viewModel.setArtist(it?.toString() ?: "")
            }
        }
    }

    private fun setupSave() = whenResumed {
        binding.trackEditArtistSave.onClicked().collect {
            viewModel.onSaveClicked()
        }
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) = with(binding) {
        when(state){
            is State.Loading -> {
                trackEditLoading.isVisible = true
                trackEditLoaded.isVisible = false
                trackEditArtistSave.isVisible = false
                trackEditLoadingLabel.setText(R.string.loading)
            }
            is State.Saving -> {
                trackEditLoading.isVisible = true
                trackEditLoaded.isVisible = false
                trackEditArtistSave.isVisible = false
                trackEditLoadingLabel.setText(R.string.tracklist_track_edit_saving)
            }
            is State.Loaded -> {
                trackEditLoading.isVisible = false
                trackEditLoaded.isVisible = true
                trackEditArtistSave.isVisible = true
                if(trackEditTrackNameEdit.text.isNullOrEmpty()) {
                    trackEditTrackNameEdit.setText(state.trackName)
                }
                if(trackEditArtistEdit.text.isNullOrEmpty()) {
                    trackEditArtistEdit.setText(state.artist)
                }
                trackEditTrackName.isErrorEnabled = state.trackNameError
                trackEditTrackName.error = if(state.trackNameError){
                    getString(R.string.tracklist_track_edit_track_error)
                }else null
                trackEditArtist.isErrorEnabled = state.artistError
                trackEditArtist.error = if(state.artistError){
                    getString(R.string.tracklist_track_edit_artist_error)
                }else null
            }
        }
    }

    private fun setupError() = whenResumed {
        viewModel.saveErrorBus.collect {
            Toast.makeText(requireContext(), R.string.tracklist_track_edit_error, Toast.LENGTH_LONG).show()
        }
    }

}