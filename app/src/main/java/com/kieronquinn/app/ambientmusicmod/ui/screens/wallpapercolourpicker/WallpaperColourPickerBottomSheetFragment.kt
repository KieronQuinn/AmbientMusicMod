package com.kieronquinn.app.ambientmusicmod.ui.screens.wallpapercolourpicker

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentWallpaperColorPickerBottomSheetBinding
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.ui.base.BaseBottomSheetFragment
import org.koin.android.ext.android.inject

class WallpaperColourPickerBottomSheetFragment :
    BaseBottomSheetFragment<FragmentWallpaperColorPickerBottomSheetBinding>(
        FragmentWallpaperColorPickerBottomSheetBinding::inflate
    ) {

    private val settings by inject<SettingsRepository>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val navigationInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraPadding = resources.getDimension(R.dimen.margin_16).toInt()
            view.updatePadding(
                left = navigationInsets.left,
                right = navigationInsets.right,
                bottom = navigationInsets.bottom + extraPadding
            )
            insets
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            with(binding) {
                val availableColors = monet.getAvailableWallpaperColors() ?: emptyList()
                //No available colors = likely using a live wallpaper, show a toast and dismiss
                if (availableColors.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.color_picker_unavailable),
                        Toast.LENGTH_LONG
                    ).show()
                    dismiss()
                    return@launchWhenResumed
                }
                root.backgroundTintList =
                    ColorStateList.valueOf(monet.getBackgroundColor(requireContext()))
                colorPickerList.layoutManager =
                    LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                colorPickerList.adapter = WallpaperColourPickerAdapter(
                    requireContext(),
                    monet.getSelectedWallpaperColor(),
                    availableColors
                ) {
                    onColorPicked(it)
                }
                colorPickerOk.setOnClickListener {
                    dialog?.dismiss()
                }
                colorPickerOk.setTextColor(monet.getAccentColor(requireContext()))
            }
        }
    }

    private fun onColorPicked(color: Int) = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        settings.monetColor.set(color)
        //Trigger a manual update
        monet.updateMonetColors()
    }


}