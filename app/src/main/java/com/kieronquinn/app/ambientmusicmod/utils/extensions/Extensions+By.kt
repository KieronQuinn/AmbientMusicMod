package com.kieronquinn.app.ambientmusicmod.utils.extensions

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline fun <T> ReadWriteProperty(crossinline getValue: () -> T, crossinline setValue: (T) -> Unit): ReadWriteProperty<Any?, T> {
    return object: ReadWriteProperty<Any?, T> {

        override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return getValue.invoke()
        }

        override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            setValue.invoke(value)
        }

    }
}

inline fun <T> ReadOnlyProperty(crossinline getValue: () -> T): ReadOnlyProperty<Any?, T> {
    return object: ReadOnlyProperty<Any?, T> {

        override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return getValue.invoke()
        }

    }
}