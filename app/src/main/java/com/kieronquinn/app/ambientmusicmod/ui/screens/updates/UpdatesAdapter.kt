package com.kieronquinn.app.ambientmusicmod.ui.screens.updates

import android.content.res.ColorStateList
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.ItemUpdatesAboutBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemUpdatesAmmBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemUpdatesPamBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemUpdatesShardsBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.repositories.ShardsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.UpdatesRepository.UpdateState
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.ambientmusicmod.ui.screens.updates.UpdatesViewModel.UpdatesSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.formatDateTime
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isDarkMode
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import kotlinx.coroutines.flow.collect

class UpdatesAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items) {

    private val chipBackground by lazy {
        ColorStateList.valueOf(monet.getPrimaryColor(recyclerView.context))
    }

    private val googleSansTextMedium by lazy {
        ResourcesCompat.getFont(recyclerView.context, R.font.google_sans_text_medium)
    }

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<UpdatesSettingsItem.ItemType>(viewType)
            ?: super.getItemType(viewType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, itemType: BaseSettingsItemType): BaseSettingsAdapter.ViewHolder {
        return when (itemType) {
            UpdatesSettingsItem.ItemType.SHARDS -> ViewHolder.Shards(
                ItemUpdatesShardsBinding.inflate(layoutInflater, parent, false)
            )
            UpdatesSettingsItem.ItemType.AMM -> ViewHolder.AMM(
                ItemUpdatesAmmBinding.inflate(layoutInflater, parent, false)
            )
            UpdatesSettingsItem.ItemType.PAM -> ViewHolder.PAM(
                ItemUpdatesPamBinding.inflate(layoutInflater, parent, false)
            )
            UpdatesSettingsItem.ItemType.ABOUT -> ViewHolder.About(
                ItemUpdatesAboutBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: BaseSettingsAdapter.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder.Shards -> {
                holder.setup(items[position] as UpdatesSettingsItem.Shards)
            }
            is ViewHolder.AMM -> {
                holder.setup(items[position] as UpdatesSettingsItem.AMM)
            }
            is ViewHolder.PAM -> {
                holder.setup(items[position] as UpdatesSettingsItem.PAM)
            }
            is ViewHolder.About -> {
                holder.setup(items[position] as UpdatesSettingsItem.About)
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun ViewHolder.Shards.setup(shards: UpdatesSettingsItem.Shards) = with(binding) {
        root.backgroundTintList = ColorStateList.valueOf(
            monet.getPrimaryColor(root.context, !root.context.isDarkMode)
        )
        val accent = monet.getAccentColor(root.context)
        val shardsState = shards.shardsState
        updatesShardsTrackCount.text = shardsState.local.trackCount.toString()
        val personalisedCount = shardsState.local.personalisedTrackCount
        if(personalisedCount > 0) {
            updatesShardsPersonalisedTrackData.isVisible = true
            updatesShardsTrackCountPersonalised.text = personalisedCount.toString()
        }else{
            updatesShardsPersonalisedTrackData.isVisible = false
        }
        val shouldShowTracks = personalisedCount + shardsState.local.trackCount > 0
        val remote = shardsState.remote
        when {
            shardsState.downloadState == ShardsRepository.ShardDownloadState.DOWNLOADING -> {
                updatesShardsTrackDataContainer.isVisible = false
                updatesShardsCountry.isVisible = false
                updatesShardsUpdate.isVisible = false
                updatesShardsViewSpace.isVisible = false
                updatesShardsViewTracks.isVisible = false
                updatesShardsTrackDataLoading.isVisible = true
                updatesShardsStatus.setText(R.string.updates_shards_status_updating)
                updatesShardsVersion.text = root.context.getString(
                    R.string.updates_version, shardsState.local.versionCode.toString()
                )
                updatesShardsDate.text = root.context.getString(
                    R.string.updates_shards_date, root.context.formatDateTime(shardsState.local.date)
                )
            }
            shardsState.downloadState == ShardsRepository.ShardDownloadState.WAITING_FOR_NETWORK -> {
                updatesShardsTrackDataContainer.isVisible = false
                updatesShardsCountry.isVisible = false
                updatesShardsUpdate.isVisible = false
                updatesShardsViewSpace.isVisible = false
                updatesShardsViewTracks.isVisible = false
                updatesShardsTrackDataLoading.isVisible = true
                updatesShardsStatus.setText(R.string.updates_shards_status_waiting_for_wifi)
                updatesShardsVersion.text = root.context.getString(
                    R.string.updates_version, shardsState.local.versionCode.toString()
                )
                updatesShardsDate.text = root.context.getString(
                    R.string.updates_shards_date, root.context.formatDateTime(shardsState.local.date)
                )
            }
            shardsState.downloadState == ShardsRepository.ShardDownloadState.WAITING_FOR_CHARGING -> {
                updatesShardsTrackDataContainer.isVisible = false
                updatesShardsCountry.isVisible = false
                updatesShardsUpdate.isVisible = false
                updatesShardsViewSpace.isVisible = false
                updatesShardsViewTracks.isVisible = false
                updatesShardsTrackDataLoading.isVisible = true
                updatesShardsStatus.setText(R.string.updates_shards_status_waiting_for_charging)
                updatesShardsVersion.text = root.context.getString(
                    R.string.updates_version, shardsState.local.versionCode.toString()
                )
                updatesShardsDate.text = root.context.getString(
                    R.string.updates_shards_date, root.context.formatDateTime(shardsState.local.date)
                )
            }
            shardsState.updateAvailable && remote != null -> {
                updatesShardsTrackDataContainer.isVisible = shouldShowTracks
                updatesShardsStatus.setText(R.string.updates_status_update_available)
                val version = root.context.getString(
                    R.string.updates_version_update,
                    remote.versionCode.toString(), shardsState.local.versionCode.toString()
                )
                updatesShardsVersion.text = version
                val date = root.context.getString(
                    R.string.updates_shards_date_update,
                    root.context.formatDateTime(remote.date),
                    root.context.formatDateTime(shardsState.local.date)
                )
                updatesShardsDate.text = date
                updatesShardsUpdate.isVisible = true
                updatesShardsViewSpace.isVisible = true
                updatesShardsCountry.isVisible = shouldShowTracks
                updatesShardsViewTracks.isVisible = shouldShowTracks
                updatesShardsTrackDataLoading.isVisible = false
            }
            else -> {
                updatesShardsTrackDataContainer.isVisible = shouldShowTracks
                updatesShardsStatus.setText(R.string.updates_status_up_to_date)
                updatesShardsVersion.text = root.context.getString(
                    R.string.updates_version, shardsState.local.versionCode.toString()
                )
                updatesShardsDate.text = root.context.getString(
                    R.string.updates_shards_date, root.context.formatDateTime(shardsState.local.date)
                )
                updatesShardsUpdate.isVisible = false
                updatesShardsViewSpace.isVisible = false
                updatesShardsCountry.isVisible = shouldShowTracks
                updatesShardsViewTracks.isVisible = shouldShowTracks
                updatesShardsTrackDataLoading.isVisible = false
            }
        }
        updatesShardsUpdate.setTextColor(accent)
        updatesShardsUpdate.overrideRippleColor(accent)
        updatesShardsViewTracks.setTextColor(accent)
        updatesShardsViewTracks.overrideRippleColor(accent)
        updatesShardsTrackDataLoading.applyMonet()
        updatesShardsIcon.imageTintList = ColorStateList.valueOf(accent)
        updatesShardsCountry.clipToOutline = true
        updatesShardsCountry.setImageResource(shards.shardsState.local.selectedCountry.icon)
        lifecycleScope.launchWhenResumed {
            updatesShardsUpdate.onClicked().collect { shards.onUpdateClicked(shards.shardsState) }
        }
        lifecycleScope.launchWhenResumed {
            updatesShardsViewTracks.onClicked().collect { shards.onViewTracksClicked() }
        }
        lifecycleScope.launchWhenResumed {
            updatesShardsCountry.onClicked().collect { shards.onCountryClicked() }
        }
    }

    private fun ViewHolder.AMM.setup(amm: UpdatesSettingsItem.AMM) = with(binding) {
        root.backgroundTintList = ColorStateList.valueOf(
            monet.getPrimaryColor(root.context, !root.context.isDarkMode)
        )
        val accent = monet.getAccentColor(root.context)
        when (amm.updateState) {
            is UpdateState.UpdateAvailable -> {
                updatesAmmStatus.setText(R.string.updates_status_update_available)
                updatesAmmUpdate.setText(R.string.updates_update)
                val version = root.context.getString(
                    R.string.updates_version_update,
                    amm.updateState.remoteVersion, amm.updateState.localVersion
                )
                updatesAmmVersion.text = version
                updatesAmmUpdate.isVisible = true
            }
            is UpdateState.UpToDate -> {
                updatesAmmStatus.setText(R.string.updates_status_up_to_date)
                updatesAmmVersion.text =
                    root.context.getString(R.string.updates_version, amm.updateState.localVersion)
                updatesAmmUpdate.isVisible = false
            }
            is UpdateState.NotInstalled -> {
                updatesAmmStatus.setText(R.string.updates_status_not_installed)
                updatesAmmUpdate.setText(R.string.updates_install)
                updatesAmmVersion.text =
                    root.context.getString(R.string.updates_version, amm.updateState.remoteVersion)
                updatesAmmUpdate.isVisible = true
            }
            is UpdateState.FailedToFetchUpdate -> {
                updatesAmmStatus.setText(R.string.updates_status_installed)
                updatesAmmUpdate.setText(R.string.updates_install)
                updatesAmmVersion.setText(R.string.updates_version_failed)
                updatesAmmUpdate.isVisible = false
            }
            is UpdateState.FailedToFetchInitial -> {
                updatesAmmStatus.setText(R.string.updates_status_not_installed)
                updatesAmmUpdate.setText(R.string.updates_install)
                updatesAmmVersion.setText(R.string.updates_version_failed)
                updatesAmmUpdate.isVisible = false
            }
        }
        updatesAmmUpdate.setTextColor(accent)
        updatesAmmUpdate.overrideRippleColor(accent)
        updatesAmmIcon.imageTintList = ColorStateList.valueOf(accent)
        lifecycleScope.launchWhenResumed {
            updatesAmmUpdate.onClicked().collect { amm.onUpdateClicked(amm.updateState) }
        }
    }

    private fun ViewHolder.PAM.setup(amm: UpdatesSettingsItem.PAM) = with(binding) {
        root.backgroundTintList = ColorStateList.valueOf(
            monet.getPrimaryColor(root.context, !root.context.isDarkMode)
        )
        val accent = monet.getAccentColor(root.context)
        when (amm.updateState) {
            is UpdateState.UpdateAvailable -> {
                updatesPamStatus.setText(R.string.updates_status_update_available)
                updatesPamUpdate.setText(R.string.updates_update)
                val version = root.context.getString(
                    R.string.updates_version_update,
                    amm.updateState.remoteVersion, amm.updateState.localVersion
                )
                updatesPamVersion.text = version
                updatesPamUpdate.isVisible = true
            }
            is UpdateState.UpToDate -> {
                updatesPamStatus.setText(R.string.updates_status_up_to_date)
                updatesPamVersion.text =
                    root.context.getString(R.string.updates_version, amm.updateState.localVersion)
                updatesPamUpdate.isVisible = false
            }
            is UpdateState.NotInstalled -> {
                updatesPamStatus.setText(R.string.updates_status_not_installed)
                updatesPamUpdate.setText(R.string.updates_install)
                updatesPamVersion.text =
                    root.context.getString(R.string.updates_version, amm.updateState.remoteVersion)
                updatesPamUpdate.isVisible = true
            }
            is UpdateState.FailedToFetchUpdate -> {
                updatesPamStatus.setText(R.string.updates_status_installed)
                updatesPamUpdate.setText(R.string.updates_install)
                updatesPamVersion.setText(R.string.updates_version_failed)
                updatesPamUpdate.isVisible = false
            }
            is UpdateState.FailedToFetchInitial -> {
                updatesPamStatus.setText(R.string.updates_status_not_installed)
                updatesPamUpdate.setText(R.string.updates_install)
                updatesPamVersion.setText(R.string.updates_version_failed)
                updatesPamUpdate.isVisible = false
            }
        }
        updatesPamUpdate.setTextColor(accent)
        updatesPamUpdate.overrideRippleColor(accent)
        updatesPamIcon.imageTintList = ColorStateList.valueOf(accent)
        lifecycleScope.launchWhenResumed {
            updatesPamUpdate.onClicked().collect { amm.onUpdateClicked(amm.updateState) }
        }
    }

    private fun ViewHolder.About.setup(about: UpdatesSettingsItem.About) = with(binding) {
        val context = root.context
        val content = context.getString(R.string.about_version, BuildConfig.VERSION_NAME)
        itemUpdatesAboutContent.text = content
        root.backgroundTintList = ColorStateList.valueOf(
            monet.getPrimaryColor(root.context, !root.context.isDarkMode)
        )
        mapOf(
            itemUpdatesAboutContributors to about.onContributorsClicked,
            itemUpdatesAboutDonate to about.onDonateClicked,
            itemUpdatesAboutGithub to about.onGitHubClicked,
            itemUpdatesAboutLibraries to about.onLibrariesClicked,
            itemUpdatesAboutTwitter to about.onTwitterClicked,
            itemUpdatesAboutXda to about.onXdaClicked
        ).forEach { chip ->
            with(chip.key){
                chipBackgroundColor = chipBackground
                typeface = googleSansTextMedium
                lifecycleScope.launchWhenResumed {
                    onClicked().collect {
                        chip.value()
                    }
                }
            }
        }
    }

    sealed class ViewHolder(override val binding: ViewBinding): BaseSettingsAdapter.ViewHolder(binding) {
        data class Shards(override val binding: ItemUpdatesShardsBinding): ViewHolder(binding)
        data class AMM(override val binding: ItemUpdatesAmmBinding): ViewHolder(binding)
        data class PAM(override val binding: ItemUpdatesPamBinding): ViewHolder(binding)
        data class About(override val binding: ItemUpdatesAboutBinding): ViewHolder(binding)
    }

}