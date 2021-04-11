package com.kieronquinn.app.ambientmusicmod.xposed.wrappers

/**
 *  Wraps the @hide interface android.media.soundtrigger_middleware.ISoundTriggerMiddlewareService to allow for nicer calls in the Xposed classes
 */
object ISoundTriggerMiddlewareService {

    fun getClass(classLoader: ClassLoader): Class<*> {
        return Class.forName("android.media.soundtrigger_middleware.ISoundTriggerMiddlewareService", false, classLoader)
    }

}