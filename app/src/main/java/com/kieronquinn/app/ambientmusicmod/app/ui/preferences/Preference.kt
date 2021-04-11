package com.kieronquinn.app.ambientmusicmod.app.ui.preferences

import android.content.Context
import android.content.res.ColorStateList
import android.text.util.Linkify
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updateLayoutParams
import androidx.preference.PreferenceViewHolder
import com.kieronquinn.app.ambientmusicmod.R
import me.saket.bettermovementmethod.BetterLinkMovementMethod

class Preference : androidx.preference.Preference {

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

    private var hasClickListener = false
    private var linkClickListener: ((String) -> Boolean)? = null
    private var root: LinearLayout? = null
    private var tintColor: Int? = null

    override fun setOnPreferenceClickListener(onPreferenceClickListener: OnPreferenceClickListener?) {
        super.setOnPreferenceClickListener(onPreferenceClickListener)
        hasClickListener = onPreferenceClickListener != null
    }

    fun setLinkClickListener(linkClickListener: (String) -> Boolean){
        this.linkClickListener = linkClickListener
    }

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
        titleView.typeface = ResourcesCompat.getFont(context, R.font.google_sans_medium)
        titleView.alpha = if(isEnabled) 1f else 0.5f
        val summaryView = holder.findViewById(android.R.id.summary) as? TextView
        summaryView?.maxLines = Int.MAX_VALUE
        holder.itemView.post {
            if(!hasClickListener) holder.itemView.isClickable = false
            if(linkClickListener != null){
                summaryView?.let {
                    Linkify.addLinks(summaryView, Linkify.ALL)
                }
                summaryView?.movementMethod = BetterLinkMovementMethod.newInstance().setOnLinkClickListener { _, url -> linkClickListener?.invoke(url) ?: false }
            }
        }
        root = summaryView?.parent?.parent as LinearLayout
        setBackgroundTint(tintColor)
    }

    fun setBackgroundTint(@ColorInt tintColor: Int?){
        root?.run {
            if(tintColor != null) {
                background = ContextCompat.getDrawable(context, R.drawable.background_preference)
                backgroundTintList = ColorStateList.valueOf(tintColor)
                foreground = ContextCompat.getDrawable(context, R.drawable.foreground_preference_tinted)
                updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = resources.getDimension(R.dimen.margin_extra_small).toInt()
                    bottomMargin = resources.getDimension(R.dimen.margin_extra_small).toInt()
                }
            }else{
                background = null
                foreground = ContextCompat.getDrawable(context, R.drawable.foreground_preference)
                updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = 0
                    bottomMargin = 0
                }
            }
        }
        this@Preference.tintColor = tintColor
    }

}