package com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.trackinfo

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.Scopes
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentTrackInfoBottomSheetBinding
import com.kieronquinn.app.ambientmusicmod.model.recognition.Player
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsRepository.ShardCountry
import com.kieronquinn.app.ambientmusicmod.ui.base.BaseBottomSheetFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.TracklistViewModel
import com.kieronquinn.app.ambientmusicmod.ui.screens.tracklist.trackinfo.TrackInfoBottomSheetViewModel.TrackInfoSettingsItem
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onApplyInsets
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent

class TrackInfoBottomSheetFragment: BaseBottomSheetFragment<FragmentTrackInfoBottomSheetBinding>(FragmentTrackInfoBottomSheetBinding::inflate), KoinScopeComponent {

    override val scope by lazy {
        getKoin().getOrCreateScope<TracklistViewModel>(Scopes.TRACK_LIST.name)
    }

    private val args by navArgs<TrackInfoBottomSheetFragmentArgs>()
    private val viewModel by viewModel<TrackInfoBottomSheetViewModel>()

    private val track by lazy {
        args.track
    }

    private val players by lazy {
        Player.getPlayers(requireContext(), track.playerUrls, track.googleId)
    }

    private val adapter by lazy {
        TrackInfoBottomSheetAdapter(binding.trackInfoRecyclerview, emptyList())
    }

    private val supportsEdit by lazy {
        viewModel.supportsEdit()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInsets(view)
        setupRecyclerView()
        setupClose()
        setupEdit()
    }

    override fun onDestroyView() {
        binding.trackInfoRecyclerview.adapter = null
        super.onDestroyView()
    }

    private fun setupInsets(view: View) {
        binding.root.onApplyInsets { _, insets ->
            val bottomPadding = resources.getDimension(R.dimen.margin_16).toInt()
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottomInset + bottomPadding)
        }
    }

    private fun setupRecyclerView() = with(binding.trackInfoRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@TrackInfoBottomSheetFragment.adapter
        whenResumed {
            this@TrackInfoBottomSheetFragment.adapter.update(loadItems(), this@with)
        }
    }

    private fun setupClose() = with(binding.trackInfoPositive) {
        setTextColor(monet.getAccentColor(requireContext()))
        whenResumed {
            onClicked().collect {
                viewModel.onCloseClicked()
            }
        }
    }

    private fun setupEdit() = with(binding.trackInfoNeutral) {
        isVisible = args.track.isLinear && supportsEdit
        setTextColor(monet.getAccentColor(requireContext()))
        whenResumed {
            onClicked().collect {
                viewModel.onEditClicked(track, args.fromArtists)
            }
        }
    }

    private fun loadItems(): List<BaseSettingsItem> = listOfNotNull(
        GenericSettingsItem.Setting(
            getString(R.string.tracklist_track_info_track_name),
            track.trackName,
            R.drawable.ic_nav_tracklist_tracks
        ) {},
        GenericSettingsItem.Setting(
            getString(R.string.tracklist_track_info_artist),
            track.artist,
            R.drawable.ic_nav_tracklist_artists
        ) {},
        track.album?.takeIf { it.isNotBlank() }?.let {
            GenericSettingsItem.Setting(
                getString(R.string.tracklist_track_info_album),
                it,
                R.drawable.ic_settings_show_album_art
            ) {}
        },
        track.year?.takeIf { it > 0 }?.let {
            GenericSettingsItem.Setting(
                getString(R.string.tracklist_track_info_year),
                it.toString(),
                R.drawable.ic_track_info_year
            ) {}
        },
        track.database?.getDatabaseInfo()?.let {
            TrackInfoSettingsItem.Country(
                R.string.tracklist_track_info_country,
                it.second,
                it.first
            )
        },
        if(players.isNotEmpty()){
            TrackInfoSettingsItem.Players(players, viewModel::onChipClicked)
        }else null
    )

    private fun String.getDatabaseInfo(): Pair<Int, Int> {
        if(this == ShardCountry.CORE_SHARED_FILENAME) {
            return Pair(
                R.drawable.ic_global,
                R.string.tracklist_track_info_country_global
            )
        }
        val shard = ShardCountry.forCode(substring(0, 2))
        return Pair(shard.icon, shard.countryName)
    }

}