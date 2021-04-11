package com.kieronquinn.app.ambientmusicmod.xposed.wrappers

/**
 *  Represents the @hide class android.media.soundtrigger.SoundTriggerDetectionService for nicer calls from Xposed classes
 */
object SoundTriggerDetectionService {

    fun getClass(classLoader: ClassLoader): Class<*> {
        return Class.forName("android.media.soundtrigger.SoundTriggerDetectionService", false, classLoader)
    }

}