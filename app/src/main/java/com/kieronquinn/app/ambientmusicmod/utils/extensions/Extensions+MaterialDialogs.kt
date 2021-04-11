package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.core.view.updatePadding
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onShow
import com.afollestad.materialdialogs.utils.MDUtil.dimenPx
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.utils.blur.BlurUtils
import org.koin.core.context.GlobalContext

fun MaterialDialog.applyAmbientTheme(): MaterialDialog {
    cornerRadius(res = R.dimen.bottom_sheet_corner_radius)
    onShow { dialog ->
        view.apply {
            findViewById<ViewGroup>(R.id.md_title_layout).apply {
                val mdTitleView = this.findViewById<TextView>(R.id.md_text_title)
                val containerWidth = measuredWidth - (2 * dimenPx(R.dimen.md_dialog_frame_margin_horizontal))
                mdTitleView.apply {
                    scrollBarSize = 0
                    post {
                        val newLeftMargin = (containerWidth / 2) - (measuredWidth / 2)
                        updatePadding(left = newLeftMargin)
                    }
                }
            }
            findViewById<ViewGroup>(R.id.md_content_layout)?.apply {
                val mdTextMessage = this.findViewById<TextView>(R.id.md_text_message)
                mdTextMessage?.typeface = Typeface.DEFAULT
            }
        }
        window?.decorView?.findViewById<ViewGroup>(R.id.md_button_layout)?.apply {
            window?.navigationBarColor = (background as ColorDrawable).color
        }
    }
    return this
}