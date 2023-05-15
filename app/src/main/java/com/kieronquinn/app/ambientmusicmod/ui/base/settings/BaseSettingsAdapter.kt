package com.kieronquinn.app.ambientmusicmod.ui.base.settings

import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemSettingsHeaderBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemSettingsSwitchBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemSettingsSwitchItemBinding
import com.kieronquinn.app.ambientmusicmod.databinding.ItemSettingsTextItemBinding
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.BaseSettingsItemType
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem
import com.kieronquinn.app.ambientmusicmod.model.settings.GenericSettingsItem.GenericSettingsItemType
import com.kieronquinn.app.ambientmusicmod.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onChanged
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onLongClicked
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.extensions.views.applyMonetLight

abstract class BaseSettingsAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    open var items: List<BaseSettingsItem>
): LifecycleAwareRecyclerView.ListAdapter<BaseSettingsItem, BaseSettingsAdapter.ViewHolder>(createDiffCallback(), recyclerView) {

    companion object {
        private fun createDiffCallback(): DiffUtil.ItemCallback<BaseSettingsItem> {
            return object: DiffUtil.ItemCallback<BaseSettingsItem>() {
                override fun areItemsTheSame(
                    oldItem: BaseSettingsItem,
                    newItem: BaseSettingsItem
                ): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(
                    oldItem: BaseSettingsItem,
                    newItem: BaseSettingsItem
                ): Boolean {
                    return newItem.deepEquals(newItem)
                }
            }
        }
    }

    protected val layoutInflater: LayoutInflater = LayoutInflater.from(recyclerView.context)

    protected val monet by lazy {
        MonetCompat.getInstance()
    }

    init {
        this.setHasStableIds(true)
    }

    @CallSuper
    open fun getItemId(item: BaseSettingsItem): Long {
        return when(item){
            is GenericSettingsItem.Setting -> item.title.hashCode().toLong()
            is GenericSettingsItem.SwitchSetting -> item.title.hashCode().toLong()
            else -> item.itemType.itemIndex.toLong()
        }
    }

    final override fun getItemId(position: Int): Long {
        return getItemId(items[position])
    }

    final override fun getItemViewType(position: Int): Int {
        return items[position].itemType.itemIndex
    }

    @CallSuper
    open fun onCreateViewHolder(parent: ViewGroup, itemType: BaseSettingsItemType): ViewHolder {
        itemType as GenericSettingsItemType
        return when(itemType){
            GenericSettingsItemType.HEADER -> GenericViewHolder.Header(
                ItemSettingsHeaderBinding.inflate(layoutInflater, parent, false)
            )
            GenericSettingsItemType.SWITCH -> GenericViewHolder.Switch(
                ItemSettingsSwitchBinding.inflate(layoutInflater, parent, false)
            )
            GenericSettingsItemType.SETTING -> GenericViewHolder.Setting(
                ItemSettingsTextItemBinding.inflate(layoutInflater, parent, false)
            )
            GenericSettingsItemType.SWITCH_SETTING -> GenericViewHolder.SwitchSetting(
                ItemSettingsSwitchItemBinding.inflate(layoutInflater, parent, false)
            )
            GenericSettingsItemType.DROPDOWN -> GenericViewHolder.Dropdown(
                ItemSettingsTextItemBinding.inflate(layoutInflater, parent, false)
            )
        }
    }

    @CallSuper
    open fun getItemType(viewType: Int): BaseSettingsItemType {
        return BaseSettingsItemType.findIndex<GenericSettingsItemType>(viewType)
            ?: throw RuntimeException("Failed to find item with index $viewType")
    }

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return onCreateViewHolder(
            parent,
            getItemType(viewType)
        )
    }

    @CallSuper
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        when(holder){
            is GenericViewHolder.Header -> holder.setup(item as GenericSettingsItem.Header)
            is GenericViewHolder.Switch -> holder.setup(item as GenericSettingsItem.Switch)
            is GenericViewHolder.Setting -> holder.setup(item as GenericSettingsItem.Setting)
            is GenericViewHolder.SwitchSetting -> holder.setup(item as GenericSettingsItem.SwitchSetting)
            is GenericViewHolder.Dropdown -> holder.setup(item as GenericSettingsItem.Dropdown<*>)
        }
    }

    /**
     *  Updates the list via [DiffUtil], and scrolls to the top if the size has changed
     */
    fun update(newList: List<BaseSettingsItem>, recyclerView: LifecycleAwareRecyclerView, forceScroll: Boolean = false) {
        submitList(newList) {
            val hasChangedSize = items.size != newList.size
            items = newList
            if(hasChangedSize || forceScroll) {
                recyclerView.scrollToPosition(0)
            }
        }
    }

    private fun GenericViewHolder.Header.setup(item: GenericSettingsItem.Header) = with(binding) {
        itemSettingsHeaderTitle.text = item.text
    }

    private fun GenericViewHolder.Switch.setup(item: GenericSettingsItem.Switch) = with(binding) {
        itemSettingsSwitchSwitch.isChecked = item.enabled
        itemSettingsSwitchSwitch.text = item.text
        whenResumed {
            binding.itemSettingsSwitchSwitch.onClicked().collect {
                item.onChanged(!itemSettingsSwitchSwitch.isChecked)
            }
        }
    }

    private fun GenericViewHolder.Setting.setup(item: GenericSettingsItem.Setting) = with(binding) {
        root.alpha = if(item.enabled) 1f else 0.5f
        root.isEnabled = item.enabled
        itemSettingsTextTitle.text = item.title
        itemSettingsTextContent.text = item.subtitle
        itemSettingsTextContent.isVisible = item.subtitle.isNotEmpty()
        itemSettingsTextIcon.setImageResource(item.icon)
        whenResumed {
            root.onClicked().collect {
                item.onClick()
            }
        }
        if(item.onLongClick != null){
            whenResumed {
                root.onLongClicked().collect {
                    item.onLongClick.invoke()
                }
            }
        }else{
            root.setOnLongClickListener(null)
        }
    }

    private fun GenericViewHolder.SwitchSetting.setup(item: GenericSettingsItem.SwitchSetting) = with(binding) {
        root.alpha = if(item.enabled) 1f else 0.5f
        itemSettingsSwitchTitle.text = item.title
        itemSettingsSwitchContent.text = item.subtitle
        itemSettingsSwitchContent.isVisible = item.subtitle.isNotEmpty()
        itemSettingsSwitchIcon.setImageResource(item.icon)
        itemSettingsSwitchSwitch.isChecked = item.checked
        itemSettingsSwitchSwitch.isEnabled = item.enabled
        itemSettingsSwitchSwitch.applyMonetLight()
        whenResumed {
            binding.itemSettingsSwitchSwitch.onChanged().collect {
                if(!item.enabled) return@collect
                item.onChanged(it)
            }
        }
        binding.root.setOnClickListener {
            if(!item.enabled) return@setOnClickListener
            binding.itemSettingsSwitchSwitch.toggle()
        }
    }

    private fun <T> GenericViewHolder.Dropdown.setup(
        item: GenericSettingsItem.Dropdown<T>
    ) = with(binding) {
        itemSettingsTextTitle.text = item.title
        itemSettingsTextContent.text = item.subtitle
        itemSettingsTextIcon.isVisible = item.icon != null
        itemSettingsTextSpace.isVisible = item.icon == null
        itemSettingsTextContent.isVisible = item.subtitle.isNotEmpty()
        itemSettingsTextIcon.setImageDrawable(item.icon)
        whenResumed {
            root.onClicked().collect {
                it.showDropdown(item)
            }
        }
    }

    private fun <T> View.showDropdown(
        item: GenericSettingsItem.Dropdown<T>
    ) {
        val popup = PopupMenu(context, this)
        item.options.forEachIndexed { index, option ->
            popup.menu.add(Menu.NONE, index, Menu.NONE, item.adapter(option))
        }
        popup.setOnMenuItemClickListener {
            item.onSet(item.options[it.itemId])
            popup.dismiss()
            true
        }
        popup.show()
    }

    sealed class GenericViewHolder(override val binding: ViewBinding): ViewHolder(binding) {
        data class Header(override val binding: ItemSettingsHeaderBinding): GenericViewHolder(binding)
        data class Switch(override val binding: ItemSettingsSwitchBinding): GenericViewHolder(binding)
        data class Setting(override val binding: ItemSettingsTextItemBinding): GenericViewHolder(binding)
        data class SwitchSetting(override val binding: ItemSettingsSwitchItemBinding): GenericViewHolder(binding)
        data class Dropdown(override val binding: ItemSettingsTextItemBinding): GenericViewHolder(binding)
    }

    abstract class ViewHolder(open val binding: ViewBinding): LifecycleAwareRecyclerView.ViewHolder(binding.root)

}