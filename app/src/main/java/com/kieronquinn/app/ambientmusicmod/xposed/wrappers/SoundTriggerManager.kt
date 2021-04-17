package com.kieronquinn.app.ambientmusicmod.xposed.wrappers

import android.system.OsConstants.*
import java.util.*


/**
 *  Wraps the @hide class android.media.soundtrigger.SoundTriggerWrapper to allow for nicer calls in the Xposed classes and root service
 */
class SoundTriggerManager(override val original: Any, override val classLoader: ClassLoader): BaseWrapper(
    original,
    classLoader
) {

    companion object {
        const val STATUS_OK = 0
        const val STATUS_ERROR = Int.MIN_VALUE // -2147483648
        val STATUS_PERMISSION_DENIED = -EPERM // -1
        val STATUS_NO_INIT = -ENODEV // -19
        val STATUS_BAD_VALUE = -EINVAL // -22
        val STATUS_DEAD_OBJECT = -EPIPE // -32
        val STATUS_INVALID_OPERATION = -ENOSYS // -38
        val STATUS_NO_DATA = -ENODATA // -61 - Not sure what causes this one, it's not in the available source code
        val STATUS_XPOSED_OVERRIDE = -8880 // Xposed override used in module check, -XP
        val STATUS_MANUAL_OVERRIDE = -7779  // Used when the module check was overriden, shown in log dump, -MO
        val STATUS_NOT_RUN = -7882 // Used when module check has not been run in log dump, -AM

        fun isResponseOk(response: Int): Boolean {
            //STATUS_INVALID_OPERATION comes back when SoundTrigger doesn't support it, but will do after Xposed intervention so that is a fine response
            val badResponses = arrayOf(
                STATUS_BAD_VALUE,
                STATUS_DEAD_OBJECT,
                STATUS_ERROR,
                STATUS_NO_INIT,
                STATUS_NO_DATA,
                STATUS_PERMISSION_DENIED
            )
            return !badResponses.contains(response)
        }
    }

    override val originalClass: Class<*> = Class.forName("android.media.soundtrigger.SoundTriggerManager")

    fun getModelState(uuid: UUID): Int {
        return originalClass.getMethod("getModelState", UUID::class.java).invoke(original, uuid) as Int
    }

}