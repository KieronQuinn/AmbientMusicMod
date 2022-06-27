package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.custom.LockScreenCustomTextColourCustomFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.textcolour.custom.monet.LockScreenCustomTextColourMonetFragment

class LockScreenCustomTextColourViewPagerAdapter(fragment: Fragment): FragmentStateAdapter(fragment) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> LockScreenCustomTextColourMonetFragment()
            1 -> LockScreenCustomTextColourCustomFragment()
            else -> throw RuntimeException("Invalid position $position")
        }
    }

}