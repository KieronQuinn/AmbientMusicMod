package com.kieronquinn.app.ambientmusicmod.xposed.debug

import android.app.AndroidAppHelper
import android.util.Log
import dalvik.system.DexFile
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.Modifier

object GlobalLogHooks {

    /**
     *  Logs every single method call that the app makes. This causes the app to take a good few minutes to launch
     *  This is purely for debugging (comparing a "known good" run to one that is getting blocked somewhere)
     */
    fun logEveryMethod(lpparam: XC_LoadPackage.LoadPackageParam){
        val applicationInfo = AndroidAppHelper.currentApplicationInfo()
        var methodCount = 0
        var classCount = 0
        val classes = DexFile(applicationInfo.sourceDir).run {
            val list = entries().asSequence().toList()
            close()
            list
        }
        for(className in classes){
            val clazz = lpparam.classLoader.loadClass(className)
            if(clazz.isInterface) continue
            val methods = clazz.declaredMethods
            for(method in methods){
                if(Modifier.isAbstract(method.modifiers)) continue
                XposedHelpers.findAndHookMethod(clazz, method.name, *method.parameterTypes, object: XC_MethodHook(){
                    override fun beforeHookedMethod(param: MethodHookParam?) {
                        super.beforeHookedMethod(param)
                        Log.d("XAmbientM", "$className -> ${method.name}(${method.parameterTypes.joinToString(", ")})")
                    }
                })
                methodCount++
            }
            classCount++
        }
        Log.d("XAmbient", "Hooked $methodCount methods in $classCount classes")
    }

}