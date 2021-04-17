package com.kieronquinn.app.ambientmusicmod.model.preference

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.utils.ObfuscatedNames
import de.robv.android.xposed.XposedHelpers

class PreferenceScreen(private val original: Any, private val classLoader: ClassLoader){

    private val preferenceGroupClass by lazy {
        XposedHelpers.findClass("androidx.preference.PreferenceGroup", classLoader)
    }

    private val preferenceClass by lazy {
        XposedHelpers.findClass("androidx.preference.Preference", classLoader)
    }

    fun findPreference(key: CharSequence): Any {
        @ObfuscatedNames("class PreferenceGroup -> method with IllegalArgumentException <Key cannot be null>")
        return preferenceGroupClass.getMethod("c", CharSequence::class.java).invoke(original, key)
    }

    fun addPreference(index: Int, preference: Preference){
        @ObfuscatedNames("class PreferenceGroup -> list file")
        val preferenceList = preferenceGroupClass.getField("b").get(original) as java.util.List<Any>

        preferenceList.add(index, preference.getOriginal())

        @ObfuscatedNames("search for This can cause unintended behaviour -> last method call inside the <if> one higher")
        preferenceClass.getMethod("m").invoke(original)
    }

}

class Preference(private val original: Any, private val classLoader: ClassLoader, private val pasContext: Context){

    private val moduleContext by lazy {
        pasContext.createPackageContext(BuildConfig.APPLICATION_ID, Context.CONTEXT_IGNORE_SECURITY)
    }

    private val preferenceClass by lazy {
        XposedHelpers.findClass("androidx.preference.Preference", classLoader)
    }

    fun setVisible(visible: Boolean){

        /**
         *  Example obfuscated structure
         *  public final void b(boolean arg2) {
         *       if(this.w != arg2) {
         *          this.w = arg2;
         *          yc v2 = this.A;
         *          if(v2 != null) {
         *              ((yv)v2).b();
         *          }
         *      }
         *  }
         */

        @ObfuscatedNames("class Preference -> search for method passing one boolean argument with example structure as above (main thing is the listener null check & call)")
        preferenceClass.getMethod("b", Boolean::class.java).invoke(original, visible)
    }

    fun setKey(key: String){
        @ObfuscatedNames("search <not found for preference>, the field is the concatenated string before it")
        preferenceClass.getDeclaredField("f").apply {
            isAccessible = true
        }.set(original, key)
    }

    fun setTitle(title: CharSequence){
        @ObfuscatedNames("class Preference -> method with single CharSequence argument and no reference to summaryProvider")
        preferenceClass.getMethod("b", CharSequence::class.java).invoke(original, title)
    }

    fun getTitle(): CharSequence {
        @ObfuscatedNames("look in setTitle() from above and find field that is set")
        return preferenceClass.getField("q").get(original) as CharSequence
    }

    fun setSummary(summary: CharSequence){
        @ObfuscatedNames("class Preference -> search for <Preference already has a SummaryProvider set.>")
        preferenceClass.getMethod("a", CharSequence::class.java).invoke(original, summary)
    }

    fun setIcon(drawable: Drawable){
        @ObfuscatedNames("class Preference -> method with single Drawable argument")
        preferenceClass.getMethod("a", Drawable::class.java).invoke(original, drawable)
    }

    fun setIconFromModule(@DrawableRes iconRes: Int){
        setIcon(ContextCompat.getDrawable(moduleContext, iconRes) ?: return)
    }

    fun setIconFromApp(@DrawableRes iconRes: Int){
        setIcon(ContextCompat.getDrawable(pasContext, iconRes) ?: return)
    }

    fun getOriginal(): Any {
        return original
    }

}