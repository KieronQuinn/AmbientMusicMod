package com.kieronquinn.app.ambientmusicmod.xposed.wrappers

import java.util.*

/**
 *  Wraps the @hide class android.media.soundtrigger.SoundTriggerWrapper to allow for nicer calls in the Xposed classes
 */
class SoundTriggerManager(override val original: Any, override val classLoader: ClassLoader): BaseWrapper(original, classLoader) {

    override val originalClass: Class<*> = Class.forName("android.media.soundtrigger.SoundTriggerManager")

    fun getModelState(uuid: UUID){
        originalClass.getMethod("getModelState", UUID::class.java).invoke(original, uuid)
    }

}