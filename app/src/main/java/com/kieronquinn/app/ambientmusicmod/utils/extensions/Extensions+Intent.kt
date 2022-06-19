package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.ui.activities.MainActivity

private const val INTENT_KEY_SECURITY_TAG = "security_tag"
private const val PENDING_INTENT_REQUEST_CODE = 999

fun Intent.applySecurity(context: Context) {
    val securityTag = PendingIntent.getActivity(
        context,
        PENDING_INTENT_REQUEST_CODE,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE
    )
    putExtra(INTENT_KEY_SECURITY_TAG, securityTag)
}

fun Intent.verifySecurity() {
    getParcelableExtra<PendingIntent>(INTENT_KEY_SECURITY_TAG)?.let {
        if(it.creatorPackage == BuildConfig.APPLICATION_ID) return
    }
    throw SecurityException("Unauthorised access")
}