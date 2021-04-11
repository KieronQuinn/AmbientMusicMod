package com.kieronquinn.app.ambientmusicmod.app.ui.base

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.utils.blur.BlurUtils
import com.kieronquinn.app.ambientmusicmod.utils.extensions.applyAmbientTheme
import com.kieronquinn.app.ambientmusicmod.utils.extensions.asFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

abstract class BaseBottomSheetDialogFragment: BottomSheetDialogFragment() {

    private val blurUtils by inject<BlurUtils>()

    private val showBlurAnimation = ValueAnimator.ofFloat(0f, 1f).asFlow<Float> {
        it.duration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
    }

    private var showBlur: Job? = null
    private var hasStartedBlur = false

    private val bottomSheetCallback = object: BottomSheetBehavior.BottomSheetCallback(){
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            showBlur?.let {
                it.cancel()
                showBlur = null
            }
            setBlurRadius(dialog, 1f + slideOffset)
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {}
    }

    abstract fun onMaterialDialogCreated(materialDialog: MaterialDialog, savedInstanceState: Bundle?): MaterialDialog

    final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).applyAmbientTheme()
        val dialogView = dialog.view.parent as FrameLayout
        addElevation(dialog)
        val behaviour = BottomSheetBehavior.from(dialogView)
        behaviour.addBottomSheetCallback(bottomSheetCallback)
        showBlur = runShowBlur(dialog)
        return onMaterialDialogCreated(dialog, savedInstanceState)
    }

    internal fun MaterialDialog.withOk() {
        positiveButton(android.R.string.ok){ dismiss() }
    }

    private fun runShowBlur(dialog: MaterialDialog): Job {
        return lifecycleScope.launch {
            showBlurAnimation.collect {
                hasStartedBlur = true
                setBlurRadius(dialog, it)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        //Disable background dimming if we can use blur
        if(blurUtils.canBlur) disableBackgroundDim()
    }

    private fun setBlurRadius(dialog: Dialog?, radius: Float, post: Boolean = false) {
        dialog ?: return
        val decorView = dialog.window?.decorView ?: return
        val appDecorView = activity?.window?.decorView ?: return
        val applyBlur = {
            blurUtils.applyBlur(decorView, appDecorView, blurUtils.blurRadiusOfRatio(radius)){
                disableBackgroundDim()
            }
        }
        if(post) decorView.post(applyBlur)
        else applyBlur.invoke()
    }

    private fun addElevation(dialog: MaterialDialog){
        val dialogView = dialog.view.parent as FrameLayout
        val elevation = resources.getDimension(R.dimen.bottom_sheet_elevation)
        dialogView.elevation = elevation
        val dialogParentView = dialogView.parent as ViewGroup
        val dialogButtonView = dialogParentView.findViewById<ViewGroup>(R.id.md_button_layout)
        dialogButtonView.elevation = elevation
    }

    private fun disableBackgroundDim(){
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    override fun onPause() {
        super.onPause()
        //Remove the blur on pause (still causes some weirdness with SystemUI)
        setBlurRadius(dialog, 0f, true)
    }

    override fun onResume() {
        super.onResume()
        //Re-add the blur on resume as it's lost otherwise, but only if blur has already started (prevents interfering with animation)
        if(hasStartedBlur){
            setBlurRadius(dialog, 1f, true)
        }
    }

}