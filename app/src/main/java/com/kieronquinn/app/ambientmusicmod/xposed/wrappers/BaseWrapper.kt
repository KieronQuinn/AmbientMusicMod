package com.kieronquinn.app.ambientmusicmod.xposed.wrappers

/**
 *  Base class for wrappers, containing the original (reflection instantiated) object, the ClassLoader used to load the original class and a reference of the reflected class
 */
abstract class BaseWrapper(open val original: Any, open val classLoader: ClassLoader) {

    abstract val originalClass: Class<*>

}