package com.kieronquinn.app.ambientmusicmod.app.service

import android.annotation.SuppressLint
import android.content.Intent
import android.os.IBinder
import android.os.ParcelUuid
import com.kieronquinn.app.ambientmusicmod.IRootService
import com.kieronquinn.app.ambientmusicmod.xposed.wrappers.SoundTriggerManager
import com.topjohnwu.superuser.ipc.RootService

/**
 *  Root Service (using libsu RootService) that calls getModelState() on Sound Trigger with the Ambient model UUID
 *  The UUID actually doesn't matter here, what we're interested in is the response from the system.
 *  0 -> OK, model is loaded
 *  -38 -> "Crash", getModelState is not natively supported (probably Sound Trigger 2.1), but will start working with the Xposed module
 *  Any other unknown states (See SoundTriggerManager.isResponseOk()) are also OK, anything else that is known is a fail.
 *  The most common fail response is -MAX_INT, which indicates the method call failed gracefully and getModelState is NOT supported.
 */
class AmbientRootService: RootService() {

    private val rootBinder = object: IRootService.Stub() {

        @SuppressLint("WrongConstant")
        override fun getModelState(uuid: ParcelUuid): Int {
            val soundTrigger = SoundTriggerManager(getSystemService("soundtrigger"), classLoader)
            return soundTrigger.getModelState(uuid.uuid)
        }

    }

    override fun onBind(intent: Intent): IBinder {
        return rootBinder
    }

}