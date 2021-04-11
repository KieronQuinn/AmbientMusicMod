package com.kieronquinn.app.ambientmusicmod.app.ui.preferences

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import com.kieronquinn.app.ambientmusicmod.R

class SwitchPreference : SwitchPreferenceCompat {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    )

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context) : super(context) {}

    private var root: LinearLayout? = null

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        root?.findViewById<TextView>(android.R.id.title)?.run{
            alpha = if(enabled) 1f else 0.5f
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        val titleView = holder?.findViewById(android.R.id.title) as TextView
        titleView.isSingleLine = false
        titleView.alpha = if(isEnabled) 1f else 0.5f
        titleView.typeface = ResourcesCompat.getFont(context, R.font.google_sans_medium)
        val summaryView = holder.findViewById(android.R.id.summary) as? TextView
        summaryView?.maxLines = Int.MAX_VALUE
        root = summaryView?.parent?.parent as LinearLayout
        setBackgroundTint(null)
    }
    fun setBackgroundTint(@ColorInt tintColor: Int?){
        root?.run {
            if(tintColor != null) {
                background = ContextCompat.getDrawable(context, R.drawable.background_preference)
                backgroundTintList = ColorStateList.valueOf(tintColor)
                foreground = ContextCompat.getDrawable(context, R.drawable.foreground_preference_tinted)
            }else{
                background = null
                foreground = ContextCompat.getDrawable(context, R.drawable.foreground_preference)
            }
        }
    }

}