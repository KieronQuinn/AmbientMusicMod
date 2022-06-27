package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentLockscreenCustomTextColourBinding
import com.kieronquinn.app.ambientmusicmod.ui.base.BackAvailable
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.ui.base.LockCollapsed
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onSelected
import com.kieronquinn.app.ambientmusicmod.utils.extensions.selectTab
import com.kieronquinn.monetcompat.extensions.toArgb
import kotlinx.coroutines.flow.collect

class LockScreenCustomTextColourFragment: BoundFragment<FragmentLockscreenCustomTextColourBinding>(FragmentLockscreenCustomTextColourBinding::inflate), BackAvailable, LockCollapsed {

    private val viewPagerAdapter by lazy {
        LockScreenCustomTextColourViewPagerAdapter(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.customTextColourPager.adapter = viewPagerAdapter
        binding.customTextColourPager.isUserInputEnabled = false
        setupTabs()
    }

    private fun setupTabs() = with(binding.customTextColourTabs) {
        val tabBackground = monet.getMonetColors().accent1[600]?.toArgb()
            ?: monet.getAccentColor(context, false)
        backgroundTintList = ColorStateList.valueOf(tabBackground)
        setSelectedTabIndicatorColor(monet.getAccentColor(context))
        selectTab(binding.customTextColourPager.currentItem)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            onSelected().collect {
                binding.customTextColourPager.currentItem = it
            }
        }
    }

}