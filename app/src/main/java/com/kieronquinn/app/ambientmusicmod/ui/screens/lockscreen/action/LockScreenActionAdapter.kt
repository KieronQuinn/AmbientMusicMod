package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.action

import android.content.res.ColorStateList
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemLockscreenActionBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.action.LockScreenActionViewModel.LockScreenActionSettingsItem
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isDarkMode
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.flow.collect

class LockScreenActionAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    override var items: List<BaseSettingsItem>
) : BaseSettingsAdapter(recyclerView, items) {

    override fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<LockScreenActionSettingsItem.ItemType>(viewType)
            ?: super.getItemType(viewType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, itemType: BaseSettingsItemType): ViewHolder {
        return when (itemType) {
            LockScreenActionSettingsItem.ItemType.ACTION -> LockScreenActionViewHolder.Action(
                ItemLockscreenActionBinding.inflate(layoutInflater, parent, false)
            )
            else -> super.onCreateViewHolder(parent, itemType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is LockScreenActionViewHolder.Action -> {
                val item = items[position] as LockScreenActionSettingsItem.Action
                holder.setup(item)
            }
            else -> super.onBindViewHolder(holder, position)
        }
    }

    private fun LockScreenActionViewHolder.Action.setup(
        action: LockScreenActionSettingsItem.Action
    ) = with(binding) {
        lockscreenActionTitle.setText(action.action.title)
        lockscreenActionContent.setText(action.action.content)
        lockscreenActionRadio.isChecked = action.enabled
        lockscreenActionRadio.applyMonet()
        val background = monet.getPrimaryColor(root.context, !root.context.isDarkMode)
        root.backgroundTintList = ColorStateList.valueOf(background)
        root.setOnClickListener {
            lockscreenActionRadio.callOnClick()
        }
        lifecycleScope.launchWhenResumed {
            lockscreenActionRadio.onClicked().collect {
                action.onClicked(action.action)
            }
        }
    }

    sealed class LockScreenActionViewHolder(override val binding: ViewBinding) : ViewHolder(binding) {
        data class Action(override val binding: ItemLockscreenActionBinding) :
            LockScreenActionViewHolder(binding)
    }

}