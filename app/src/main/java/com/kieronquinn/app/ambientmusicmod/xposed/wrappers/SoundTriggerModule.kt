package com.kieronquinn.app.ambientmusicmod.xposed.wrappers

/**
 *  Represents the @hide class android.hardware.soundtrigger.SoundTriggerModule to allow for nicer calls in the Xposed classes
 */
object SoundTriggerModule {

    fun getClass(classLoader: ClassLoader): Class<*> {
        return Class.forName("android.hardware.soundtrigger.SoundTriggerModule", false, classLoader)
    }

}