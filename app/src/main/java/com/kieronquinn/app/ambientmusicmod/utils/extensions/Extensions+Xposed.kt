package com.kieronquinn.app.ambientmusicmod.utils.extensions

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement

fun MethodHook(beforeHookedMethod: ((param: XC_MethodHook.MethodHookParam) -> Unit)? = null, afterHookedMethod: ((param: XC_MethodHook.MethodHookParam) -> Unit)? = null): XC_MethodHook {
    return object: XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            super.beforeHookedMethod(param)
            beforeHookedMethod?.invoke(param)
        }

        override fun afterHookedMethod(param: MethodHookParam) {
            super.afterHookedMethod(param)
            afterHookedMethod?.invoke(param)
        }
    }
}

fun MethodReplacement(replaceHookedMethod: ((param: XC_MethodHook.MethodHookParam) -> Any?)): XC_MethodReplacement {
    return object: XC_MethodReplacement() {
        override fun replaceHookedMethod(param: MethodHookParam): Any? {
            return replaceHookedMethod.invoke(param)
        }
    }
}