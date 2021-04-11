package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.view.WindowManager
import androidx.fragment.app.Fragment

var Fragment.keepScreenOn: Boolean
    get() = throw NotImplementedError("Cannot check keepScreenOn state on a fragment")
    set(value) {
        if(value) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }else{
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }