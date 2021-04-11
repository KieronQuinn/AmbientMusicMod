package com.kieronquinn.app.ambientmusicmod.utils

/**
 *  Purely for convenience & documentation: if the Ambient app ever needs updating, all uses of obfuscated names are tagged
 *  with explanations of how to find their new names in a new decompiled APK
 */
@Target(AnnotationTarget.EXPRESSION, AnnotationTarget.FUNCTION, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ObfuscatedNames(val howToFind: String)