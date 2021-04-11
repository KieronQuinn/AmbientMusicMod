package com.kieronquinn.app.ambientmusicmod.xposed.debug

import android.media.AudioFormat
import android.util.Log
import com.kieronquinn.app.ambientmusicmod.xposed.wrappers.SoundTrigger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object AndroidLogHooks {

    fun hookLogs(lpparam: XC_LoadPackage.LoadPackageParam){
        XposedHelpers.findAndHookConstructor(SoundTrigger.GenericRecognitionEvent.getClass(lpparam.classLoader), Integer.TYPE, Integer.TYPE, Boolean::class.java, Integer.TYPE, Integer.TYPE, Integer.TYPE, Boolean::class.java, AudioFormat::class.java, ByteArray::class.java, object: XC_MethodHook(){
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val status = param.args[0] as Int
                val soundModelHandle = param.args[1] as Int
                val captureAvailable = param.args[2] as Boolean
                val captureSession = param.args[3] as Int
                val captureDelayMs = param.args[4] as Int
                val capturePreambleMs = param.args[5] as Int
                val triggerInData = param.args[6] as Boolean
                val captureFormat = param.args[7] as AudioFormat
                val data = param.args[8] as ByteArray
                val logFriendlyString = AudioFormat::class.java.getMethod("toLogFriendlyString").invoke(captureFormat) as String
                Log.d("XAmbientH", "GenericRecognitionEvent INIT status $status soundModelHandle $soundModelHandle captureAvailable $captureAvailable captureSession $captureSession captureDelayMs $captureDelayMs capturePreambleMs $capturePreambleMs triggerInData $triggerInData captureFormat $logFriendlyString data $data")
            }
        })
        XposedHelpers.findAndHookMethod("com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat", lpparam.classLoader, "handleHalStatus", Integer.TYPE, String::class.java, object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                super.afterHookedMethod(param)
                Log.d("XAmbientH", "handleHalStatus status ${param.args[0]} for ${param.args[1]}")
            }
        })

        XposedHelpers.findAndHookMethod("com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat", lpparam.classLoader, "getModelState", Integer.TYPE, object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                Log.d("XAmbientH", "getModelState with handle ${param.args[0]}")
            }
        })

        XposedHelpers.findAndHookMethod("com.android.server.soundtrigger_middleware.SoundTriggerHw2Enforcer", lpparam.classLoader, "getModelState", Integer.TYPE, object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                Log.d("XAmbientH", "getModelState with handle ${param.args[0]}")
            }
        })
        val callbackClass = XposedHelpers.findClass("com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat\$SoundTriggerCallback", lpparam.classLoader)
        XposedBridge.hookMethod(callbackClass.declaredConstructors.first(), object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                Log.d("XAmbientH", "SoundTriggerCallback init")
            }
        })
        val recognitionConfig = XposedHelpers.findClass("android.hardware.soundtrigger.V2_3.RecognitionConfig", lpparam.classLoader)
        val callback = XposedHelpers.findClass("com.android.server.soundtrigger_middleware.ISoundTriggerHw2\$Callback", lpparam.classLoader)
        XposedHelpers.findAndHookMethod("com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat", lpparam.classLoader, "startRecognition_2_1", Integer.TYPE, recognitionConfig, callback, Integer.TYPE, object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                Log.d("XAmbientH", "startRecognition_2_1 with handle ${param.args[0]} on ${lpparam.packageName}")
            }
        })
        val recognitionEvent2_1 = XposedHelpers.findClass("android.hardware.soundtrigger.V2_1.ISoundTriggerHwCallback\$RecognitionEvent", lpparam.classLoader)
        XposedHelpers.findAndHookMethod("com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat\$SoundTriggerCallback", lpparam.classLoader, "recognitionCallback_2_1", recognitionEvent2_1, Integer.TYPE, object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                Log.d("XAmbientH", "recognitionCallback_2_1 with cookie ${param.args[1]}")
            }
        })
        val phraseRecognitionEvent2_1 = XposedHelpers.findClass("android.hardware.soundtrigger.V2_1.ISoundTriggerHwCallback\$PhraseRecognitionEvent", lpparam.classLoader)
        XposedHelpers.findAndHookMethod("com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat\$SoundTriggerCallback", lpparam.classLoader, "phraseRecognitionCallback_2_1", phraseRecognitionEvent2_1, Integer.TYPE, object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                Log.d("XAmbientH", "phraseRecognitionEvent with cookie ${param.args[1]}")
            }
        })
        val modelEvent2_1 = XposedHelpers.findClass("android.hardware.soundtrigger.V2_1.ISoundTriggerHwCallback\$ModelEvent", lpparam.classLoader)
        XposedHelpers.findAndHookMethod("com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat\$SoundTriggerCallback", lpparam.classLoader, "soundModelCallback_2_1", modelEvent2_1, Integer.TYPE, object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                Log.d("XAmbientH", "soundModelCallback_2_1 with cookie ${param.args[1]}")
            }
        })
        val recognitionEvent2_0 = XposedHelpers.findClass("android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback\$RecognitionEvent", lpparam.classLoader)
        XposedHelpers.findAndHookMethod("com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat\$SoundTriggerCallback", lpparam.classLoader, "recognitionCallback", recognitionEvent2_0, Integer.TYPE, object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                Log.d("XAmbientH", "recognitionCallback with cookie ${param.args[1]}")
            }
        })
        val phraseRecognitionEvent2_0 = XposedHelpers.findClass("android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback\$PhraseRecognitionEvent", lpparam.classLoader)
        XposedHelpers.findAndHookMethod("com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat\$SoundTriggerCallback", lpparam.classLoader, "phraseRecognitionCallback", phraseRecognitionEvent2_0, Integer.TYPE, object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                Log.d("XAmbientH", "phraseRecognitionCallback with cookie ${param.args[1]}")
            }
        })
        val modelEvent2_0 = XposedHelpers.findClass("android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback\$ModelEvent", lpparam.classLoader)
        XposedHelpers.findAndHookMethod("com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat\$SoundTriggerCallback", lpparam.classLoader, "soundModelCallback", modelEvent2_0, Integer.TYPE, object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                Log.d("XAmbientH", "soundModelCallback with cookie ${param.args[1]}")
            }
        })
    }

}