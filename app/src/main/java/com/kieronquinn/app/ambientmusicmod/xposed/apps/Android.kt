package com.kieronquinn.app.ambientmusicmod.xposed.apps

import android.content.Context
import android.media.AudioManager
import android.os.Looper
import android.os.Process
import com.kieronquinn.app.ambientmusicmod.xposed.XposedAppHook
import com.kieronquinn.app.ambientmusicmod.utils.extensions.MethodHook
import com.kieronquinn.app.ambientmusicmod.utils.extensions.MethodReplacement
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getAudioFormat
import com.kieronquinn.app.ambientmusicmod.xposed.debug.XLog
import com.kieronquinn.app.ambientmusicmod.xposed.wrappers.ISoundTriggerMiddlewareService
import com.kieronquinn.app.ambientmusicmod.xposed.wrappers.SoundTrigger
import com.kieronquinn.app.ambientmusicmod.xposed.wrappers.SoundTriggerModule
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class Android : XposedAppHook() {

    override val packageName = "android"
    override val appName = "Android System"

    private var statusListener: SoundTrigger.StatusListener? = null

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    /**
     * Required AudioFormat for the sound model
     */
    private val audioFormat by lazy {
        getAudioFormat()
    }

    /**
     * Called when the system is hooked, calls down to separated methods for different types of hooks
     */
    override fun onAppHooked(lpparam: XC_LoadPackage.LoadPackageParam) {
        XLog.d("Android Hooked!", true)
        setupSoundTriggerHooks(lpparam)
        hookPermissionCheck(lpparam)
    }

    /**
     *  XPOSED HOOK DEFINITIONS FOR Android System
     */

    /**
     *  Hooks various Sound Trigger related calls:
     *  - Replaces getModelState(int), preventing it calling down to the hardware as this isn't supported on soundtrigger-2.1
     *  - Hooks the constructor of SoundTriggerModule, taking a reference to the StatusListener and storing it for later
     *  - Hooks getModelState(int) [one level above the replacement], and calls onRecognition of the stored StatusListener with a dummy generic recognition event
     *      - this is the magic that starts the recognition process
     */
    private fun setupSoundTriggerHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod("com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat", lpparam.classLoader, "getModelState", Integer.TYPE, MethodReplacement {
            XLog.d("Replacing getModelState(${it.args[0]}) with doing nothing", true)
        })
        XposedHelpers.findAndHookConstructor(SoundTriggerModule.getClass(lpparam.classLoader), ISoundTriggerMiddlewareService.getClass(lpparam.classLoader), Integer.TYPE, SoundTrigger.StatusListener.getClass(lpparam.classLoader), Looper::class.java, MethodHook(afterHookedMethod = {
            XLog.d("SoundTriggerModule INIT ${lpparam.packageName}", true)
            val statusListener = SoundTrigger.StatusListener(it.args[2], lpparam.classLoader)
            //Store statusListener for the current call
            this@Android.statusListener = statusListener
        }))
        XposedHelpers.findAndHookMethod(SoundTriggerModule.getClass(lpparam.classLoader), "getModelState", Integer.TYPE, MethodHook(afterHookedMethod = {
            statusListener?.onRecognition(lpparam.classLoader.createGenericRecognitionEvent(it.args[0] as Int)) ?: run {
                XLog.d("StatusListener is null", true)
            }
        }))
    }

    /**
     *  Hooks calls to check for the MANAGE_SOUND_TRIGGER permission in the middleware. For some reason, these were failing on my OP7TP, despite the caller being system
     *  TODO: Investigate further why these are failing as they are potentially a security risk if the app-level enforcement is bypassed
     */
    private fun hookPermissionCheck(lpparam: XC_LoadPackage.LoadPackageParam){
        XposedHelpers.findAndHookMethod("com.android.server.soundtrigger.SoundTriggerService", lpparam.classLoader, "enforceCallingPermission", String::class.java, MethodReplacement {
            XLog.d("Disabling enforceCallingPermission check for ${it.args[0]} ${Process.myUid()} / ${Process.myPid()}", true)
            null
        })

        XposedHelpers.findAndHookMethod("com.android.server.soundtrigger_middleware.SoundTriggerMiddlewareValidation", lpparam.classLoader, "enforcePermission", String::class.java, MethodReplacement {
            XLog.d("Disabling enforceCallingPermission check for ${it.args[0]} ${Process.myUid()} / ${Process.myPid()}", true)
            null
        })
    }

    /**
     *  END XPOSED HOOKS
     */

    /**
     *  Create a dummy GenericRecognitionEvent for a given captureHandle, newly generated recording session ID and audioFormat to be used and the GET_STATE_RESPONSE
     *  event type
     *  This, when passed to onRecognitionEvent, starts the recognition process
     */
    private fun ClassLoader.createGenericRecognitionEvent(captureHandle: Int): SoundTrigger.GenericRecognitionEvent {
        val newRecordingSessionId = audioManager.generateAudioSessionId()
        return SoundTrigger.GenericRecognitionEvent(
            this,
            SoundTrigger.RECOGNITION_STATUS_GET_STATE_RESPONSE,
            captureHandle,
            true,
            newRecordingSessionId,
            0,
            0,
            false,
            audioFormat,
            ByteArray(0)
        )
    }

}