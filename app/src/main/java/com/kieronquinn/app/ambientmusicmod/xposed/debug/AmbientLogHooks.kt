package com.kieronquinn.app.ambientmusicmod.xposed.debug

import android.os.Build
import android.os.Bundle
import android.util.Log
import com.kieronquinn.app.ambientmusicmod.utils.ObfuscatedNames
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.*

object AmbientLogHooks {

    fun hookLogs(lpparam: XC_LoadPackage.LoadPackageParam){
        val recognitionEventClass = XposedHelpers.findClass("android.hardware.soundtrigger.SoundTrigger\$RecognitionEvent", lpparam.classLoader)
        @ObfuscatedNames("search for onGenericRecognitionEvent")
        XposedHelpers.findAndHookMethod("ewy", lpparam.classLoader, "onGenericRecognitionEvent", UUID::class.java, Bundle::class.java, Integer.TYPE, recognitionEventClass, object: XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                super.afterHookedMethod(param)
                val uuid = param.args[0] as UUID
                val bundle = param.args[1] as? Bundle
                val int1 = param.args[2] as Integer
                val event = param.args[3]
                Log.d("XAmbientH", "service onGenericRecognitionEvent $uuid $bundle $int1 $event")
            }
        })
        @ObfuscatedNames("search for metadata value, this and following hooks are all the same method name with differing params")
        XposedHelpers.findAndHookMethod("duo", lpparam.classLoader, "a", String::class.java, String::class.java, Integer.TYPE, String::class.java, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val message = "${param.args[0]} ${param.args[1]} ${param.args[2]} ${param.args[3]}"
                Log.d("AmbientLog", message)
            }
        })
        XposedHelpers.findAndHookMethod("duo", lpparam.classLoader, "a", String::class.java, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val message = "${param.args[0]}"
                Log.d("AmbientLog", message)
            }
        })
        XposedHelpers.findAndHookMethod("duo", lpparam.classLoader, "a", Throwable::class.java, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val throwable = param.args[0] as Throwable
                Log.d("AmbientLog", "Error: ", throwable)
            }
        })
        XposedHelpers.findAndHookMethod("duo", lpparam.classLoader, "a", String::class.java, Double::class.java, Object::class.java, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val formattingString = param.args[0] as String
                try {
                    Log.d("AmbientLog", formattingString.format(param.args[2].toString()) + " ${param.args[1]}")
                }catch (e: Exception){
                    Log.d("AmbientLog", arrayOf(formattingString, param.args[2].toString() + " ${param.args[1]}").joinToString(", "))
                }
            }
        })
        XposedHelpers.findAndHookMethod("duo", lpparam.classLoader, "a", String::class.java, Integer.TYPE, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val formattingString = param.args[0] as String
                try {
                    Log.d("AmbientLog", formattingString.format(param.args[1]))
                }catch (e: Exception){
                    Log.d("AmbientLog", arrayOf(formattingString, param.args[1]).joinToString(", "))
                }
            }
        })
        XposedHelpers.findAndHookMethod("duo", lpparam.classLoader, "a", String::class.java, Integer.TYPE, Long::class.java, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val formattingString = param.args[0] as String
                try {
                    Log.d("AmbientLog", formattingString.format(param.args[1], param.args[2]))
                }catch (e: Exception){
                    Log.d("AmbientLog", arrayOf(formattingString, param.args[1], param.args[2]).joinToString(", "))
                }
            }
        })
        XposedHelpers.findAndHookMethod("duo", lpparam.classLoader, "a", String::class.java, Integer.TYPE, Object::class.java, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val formattingString = param.args[0] as String
                try {
                    Log.d("AmbientLog", formattingString.format(param.args[1], param.args[2].toString()))
                }catch (e: Exception){
                    Log.d("AmbientLog", arrayOf(formattingString, param.args[1], param.args[2].toString()).joinToString(", "))
                }
            }
        })
        XposedHelpers.findAndHookMethod("duo", lpparam.classLoader, "a", String::class.java, Long::class.java, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val formattingString = param.args[0] as String
                try {
                    Log.d("AmbientLog", formattingString.format(param.args[1]))
                }catch (e: Exception){
                    Log.d("AmbientLog", arrayOf(formattingString, param.args[1]).joinToString(", "))
                }
            }
        })
        XposedHelpers.findAndHookMethod("duo", lpparam.classLoader, "a", String::class.java, Object::class.java, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val formattingString = param.args[0] as String
                try {
                    Log.d("AmbientLog", formattingString.format(param.args[1].toString()))
                }catch (e: Exception){
                    Log.d("AmbientLog", arrayOf(formattingString, param.args[1].toString()).joinToString(", "))
                }
            }
        })
        XposedHelpers.findAndHookMethod("duo", lpparam.classLoader, "a", String::class.java, Object::class.java, Integer.TYPE, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val formattingString = param.args[0] as String
                try {
                    Log.d("AmbientLog", formattingString.format(param.args[1].toString(), param.args[2]))
                }catch (e: Exception){
                    Log.d("AmbientLog", arrayOf(formattingString, param.args[1].toString(), param.args[2]).joinToString(", "))
                }
            }
        })
        XposedHelpers.findAndHookMethod("duo", lpparam.classLoader, "a", String::class.java, Object::class.java, Long::class.java, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val formattingString = param.args[0] as String
                try {
                    Log.d("AmbientLog", formattingString.format(param.args[1].toString(), param.args[2]))
                }catch (e: Exception){
                    Log.d("AmbientLog", arrayOf(formattingString, param.args[1].toString(), param.args[2]).joinToString(", "))
                }
            }
        })
        XposedHelpers.findAndHookMethod("duo", lpparam.classLoader, "a", String::class.java, Object::class.java, Object::class.java, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val formattingString = param.args[0] as String
                try {
                    Log.d("AmbientLog", formattingString.format(param.args[1].toString(), param.args[2].toString()))
                }catch (e: Exception){
                    Log.d("AmbientLog", arrayOf(formattingString, param.args[1].toString(), param.args[2].toString()).joinToString(", "))
                }
            }
        })
        XposedHelpers.findAndHookMethod("duo", lpparam.classLoader, "a", String::class.java, Object::class.java, Object::class.java, Object::class.java, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val formattingString = param.args[0] as String
                try {
                    Log.d("AmbientLog", formattingString.format(param.args[1].toString(), param.args[2].toString(), param.args[3].toString()))
                }catch (e: Exception){
                    Log.d("AmbientLog", arrayOf(formattingString, param.args[1].toString(), param.args[2].toString(), param.args[3].toString()).joinToString(", "))
                }
            }
        })
        XposedHelpers.findAndHookMethod("duo", lpparam.classLoader, "a", String::class.java, Object::class.java, Object::class.java, Object::class.java, Object::class.java, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val formattingString = param.args[0] as String
                try {
                    Log.d("AmbientLog", formattingString.format(param.args[1].toString(), param.args[2].toString(), param.args[3].toString(), param.args[4].toString()))
                }catch (e: Exception){
                    Log.d("AmbientLog", arrayOf(formattingString, param.args[1].toString(), param.args[2].toString(), param.args[3].toString(), param.args[4].toString()).joinToString(", "))
                }
            }
        })
        XposedHelpers.findAndHookMethod("duo", lpparam.classLoader, "a", String::class.java, Object::class.java, Object::class.java, Object::class.java, Object::class.java, Object::class.java, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val formattingString = param.args[0] as String
                Log.d("AmbientLog", formattingString.format(param.args[1].toString(), param.args[2].toString(), param.args[3].toString(), param.args[4].toString(), param.args[5].toString()))
            }
        })
        XposedHelpers.findAndHookMethod("duo", lpparam.classLoader, "a", String::class.java, Object::class.java, Object::class.java, Object::class.java, Object::class.java, Object::class.java, Object::class.java, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val formattingString = param.args[0] as String
                try {
                    Log.d("AmbientLog", formattingString.format(param.args[1].toString(), param.args[2].toString(), param.args[3].toString(), param.args[4].toString(), param.args[5].toString(), param.args[6].toString()))
                }catch (e: Exception){
                    Log.d("AmbientLog", arrayOf(formattingString, param.args[1].toString(), param.args[2].toString(), param.args[3].toString(), param.args[4].toString(), param.args[5].toString(), param.args[6].toString()).joinToString(", "))
                }
            }
        })
        XposedHelpers.findAndHookMethod("duo", lpparam.classLoader, "a", String::class.java, Object::class.java, Object::class.java, Object::class.java, Object::class.java, Object::class.java, Object::class.java, Object::class.java, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val formattingString = param.args[0] as String
                try {
                    Log.d("AmbientLog", formattingString.format(param.args[1].toString(), param.args[2].toString(), param.args[3].toString(), param.args[4].toString(), param.args[5].toString(), param.args[6].toString(), param.args[7].toString()))
                }catch (e: Exception){
                    Log.d("AmbientLog", arrayOf(formattingString, param.args[1].toString(), param.args[2].toString(), param.args[3].toString(), param.args[4].toString(), param.args[5].toString(), param.args[6].toString(), param.args[7].toString()).joinToString(", "))
                }
            }
        })
        XposedHelpers.findAndHookMethod("duo", lpparam.classLoader, "a", String::class.java, Object::class.java, Object::class.java, Object::class.java, Object::class.java, Object::class.java, Object::class.java, Object::class.java, Object::class.java, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val formattingString = param.args[0] as String
                try {
                    Log.d("AmbientLog", formattingString.format(param.args[1].toString(), param.args[2].toString(), param.args[3].toString(), param.args[4].toString(), param.args[5].toString(), param.args[6].toString(), param.args[7].toString(), param.args[8].toString()))
                }catch (e: Exception){
                    Log.d("AmbientLog", arrayOf(formattingString, param.args[1].toString(), param.args[2].toString(), param.args[3].toString(), param.args[4].toString(), param.args[5].toString(), param.args[6].toString(), param.args[7].toString(), param.args[8].toString()).joinToString(", "))
                }
            }
        })
    }

    /**
     *  Calls the est_g method (name may change) in PAS which is used to differentiate between the hardware-based model (Pixel 4 only) and the software based model (all others)
     */
    @ObfuscatedNames("Search for G1V5VHBME0Mq6trmUxb9Q9URJXm0Sof1")
    private fun est_g(lpparam: XC_LoadPackage.LoadPackageParam){
        val v0 = Build.DEVICE
        val v1 = Build.MANUFACTURER
        val dxf_a = XposedHelpers.callStaticMethod(XposedHelpers.findClass("dxf", lpparam.classLoader), "a")
        val dxf_a_a = XposedHelpers.callMethod(dxf_a, "a", "G1V5VHBME0Mq6trmUxb9Q9URJXm0Sof1|" + v0 + "|" + v1.toUpperCase(Locale.US))
        val dxf_a_a_c = XposedHelpers.callMethod(dxf_a_a, "c")
        Log.d("XAmbient", "est_g re-run ouutput: $dxf_a_a_c")
    }

}