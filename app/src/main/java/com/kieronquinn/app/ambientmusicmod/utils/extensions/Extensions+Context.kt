package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import com.kieronquinn.app.ambientmusicmod.constants.xposedApps

fun Context.isAppInstalled(packageName: String): Boolean {
    return packageManager.isAppInstalled(packageName)
}

fun Context.isDarkTheme() = resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK).run {
    when (this) {
        Configuration.UI_MODE_NIGHT_YES -> true
        Configuration.UI_MODE_NIGHT_NO -> false
        else -> false
    }
}

fun Context.isXposedInstalled(): Boolean {
    return xposedApps.any { isAppInstalled(it) }
}

/**
 * Create a formatted CharSequence from a string resource containing arguments and HTML formatting
 *
 * The string resource must be wrapped in a CDATA section so that the HTML formatting is conserved.
 *
 * Example of an HTML formatted string resource:
 * <string name="html_formatted"><![CDATA[ bold text: <B>%1$s</B> ]]></string>
 */
fun Context.getText(@StringRes id: Int, vararg args: Any?): CharSequence =
        HtmlCompat.fromHtml(String.format(getString(id), *args), HtmlCompat.FROM_HTML_MODE_COMPACT)
