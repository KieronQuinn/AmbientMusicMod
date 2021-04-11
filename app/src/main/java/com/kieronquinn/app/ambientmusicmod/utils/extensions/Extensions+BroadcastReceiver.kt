package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun BroadcastReceiver(onReceive: (Context, Intent?) -> Unit) = object: BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent?) {
        onReceive.invoke(context, intent)
    }
}

private const val SECURE_BROADCAST_RECEIVER_EXTRA_PENDING_INTENT = "verification_intent"
private val SECURE_BROADCAST_PACKAGE_WHITELIST = arrayOf(BuildConfig.APPLICATION_ID, PixelAmbientServices.PIXEL_AMBIENT_SERVICES_PACKAGE_NAME)

/**
 *  A BroadcastReceiver that must contain a PendingIntent created by either Pixel Ambient Services or Ambient Music Mod to work.
 *  This should be used in conjunction with Context.sendSecureBroadcast()
 */
fun SecureBroadcastReceiver(onReceive: (Context, Intent?) -> Unit) = object: BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingIntent = intent?.getParcelableExtra<PendingIntent>(SECURE_BROADCAST_RECEIVER_EXTRA_PENDING_INTENT) ?: return
        if(!SECURE_BROADCAST_PACKAGE_WHITELIST.contains(pendingIntent.creatorPackage)) return
        onReceive.invoke(context, intent)
    }
}

/**
 *  Sends a Broadcast to a given intent, with the added PendingIntent with the creatorPackage of this app
 *  This should be used in conjunction with SecureBroadcastReceiver
 */
fun Context.sendSecureBroadcast(intent: Intent){
    sendBroadcast(intent.apply {
        putExtra(SECURE_BROADCAST_RECEIVER_EXTRA_PENDING_INTENT, PendingIntent.getBroadcast(this@sendSecureBroadcast, intent.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT))
    })
}

@ExperimentalCoroutinesApi
fun Context.broadcastReceiverFlow(intentFilter: IntentFilter, secure: Boolean = true) = callbackFlow<Intent?> {
    val onReceive = { intent: Intent? ->
        Log.d("ModelResponse", "Response for ${intentFilter.getAction(0)}")
        offer(intent)
    }
    val receiver = if(secure){
        SecureBroadcastReceiver { _, intent -> onReceive.invoke(intent) }
    }else{
        BroadcastReceiver { _, intent -> onReceive.invoke(intent) }
    }
    Log.d("ModelResponse", "Registering ${intentFilter.getAction(0)}")
    registerReceiver(receiver, intentFilter)
    awaitClose {
        Log.d("ModelResponse", "Unregistering ${intentFilter.getAction(0)}")
        unregisterReceiver(receiver)
    }
}