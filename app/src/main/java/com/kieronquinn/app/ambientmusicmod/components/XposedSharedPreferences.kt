package com.kieronquinn.app.ambientmusicmod.components

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.kieronquinn.app.ambientmusicmod.providers.SharedPrefsProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class XposedSharedPreferences(private val context: Context): AmbientSharedPreferences() {

    override val sharedPreferences: SharedPreferences
        get() = throw NotImplementedError("Cannot get SharedPreferences from XposedSharedPreferences")

    override fun shared(key: String, default: String) = ReadOnlyProperty {
        getSharedStringPref(context, key, default) ?: default
    }

    override fun shared(key: String, default: Int) = ReadOnlyProperty {
        getSharedIntPref(context, key, default)
    }

    override fun shared(key: String, default: Boolean) = ReadOnlyProperty {
        getSharedBoolPref(context, key, default)
    }

    override fun shared(key: String, default: Float) = ReadOnlyProperty {
        getSharedStringPref(context, key, default.toString())?.toFloatOrNull() ?: default
    }

    override fun shared(key: String, default: Double) = ReadOnlyProperty {
        getSharedStringPref(context, key, default.toString())?.toDoubleOrNull() ?: default
    }

    override fun shared(key: String, default: Long) = ReadOnlyProperty {
        getSharedStringPref(context, key, default.toString())?.toLongOrNull() ?: default
    }

    private inline fun <T> ReadOnlyProperty(crossinline getValue: () -> T): ReadWriteProperty<Any?, T> {
        return object: ReadWriteProperty<Any?, T> {

            override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return getValue.invoke()
            }

            override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                throw XposedSharedPreferencesException("XposedSharedPreferences cannot write app preferences, only read them")
            }

        }
    }

    override fun sendUpdateIntent() {
        throw XposedSharedPreferencesException("Cannot send update intent from XposedSharedPreferences")
    }

    private class XposedSharedPreferencesException(reason: String): Exception(reason)

    //Following methods based off https://code.highspec.ru/Mikanoshi/CustoMIUIzer

    private fun getSharedStringPref(context: Context, name: String, defValue: String): String? {
        val uri: Uri =
                stringPrefToUri(name)
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        return if (cursor != null) {
            cursor.moveToFirst()
            val prefValue: String = cursor.getString(0)
            cursor.close()
            return if(prefValue.isEmpty()) defValue
            else prefValue
        } else defValue
    }

    private fun getSharedIntPref(context: Context, name: String, defValue: Int): Int {
        val uri: Uri = intPrefToUri(name)
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        return if (cursor != null) {
            cursor.moveToFirst()
            val prefValue: Int = cursor.getInt(0)
            cursor.close()
            prefValue
        } else defValue
    }

    private fun getSharedBoolPref(context: Context, name: String, defValue: Boolean): Boolean {
        val uri: Uri =
                boolPrefToUri(name, defValue)
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        return if (cursor != null) {
            cursor.moveToFirst()
            val prefValue: Int = cursor.getInt(0)
            cursor.close()
            Log.d("PASS", "getBoolSharedPref $name, default $defValue with value $prefValue")
            prefValue == 1
        } else {
            Log.d("PASS", "getBoolSharedPref $name returning defValue $defValue")
            defValue
        }
    }

    fun getSharedBoolPrefOrNull(context: Context, name: String, default: Boolean): Boolean? {
        val uri: Uri = boolPrefToUri(name, default)
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        return if (cursor != null) {
            cursor.moveToFirst()
            val prefValue: Int = cursor.getInt(0)
            cursor.close()
            prefValue == 1
        } else {
            null
        }
    }

}

fun stringPrefToUri(name: String): Uri {
    return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/string/" + name)
}

fun intPrefToUri(name: String): Uri {
    return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/integer/" + name)
}

fun boolPrefToUri(name: String, default: Boolean): Uri {
    return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/boolean/" + name + "/" + default)
}