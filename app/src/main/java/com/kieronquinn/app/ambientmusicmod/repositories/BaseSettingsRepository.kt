package com.kieronquinn.app.ambientmusicmod.repositories

import android.content.SharedPreferences
import android.graphics.Color
import com.google.gson.Gson
import com.kieronquinn.app.ambientmusicmod.repositories.BaseSettingsRepository.AmbientMusicModSetting
import com.kieronquinn.app.ambientmusicmod.repositories.BaseSettingsRepositoryImpl.SettingsConverters.SHARED_BOOLEAN
import com.kieronquinn.app.ambientmusicmod.repositories.BaseSettingsRepositoryImpl.SettingsConverters.SHARED_COLOR
import com.kieronquinn.app.ambientmusicmod.repositories.BaseSettingsRepositoryImpl.SettingsConverters.SHARED_DOUBLE
import com.kieronquinn.app.ambientmusicmod.repositories.BaseSettingsRepositoryImpl.SettingsConverters.SHARED_FLOAT
import com.kieronquinn.app.ambientmusicmod.repositories.BaseSettingsRepositoryImpl.SettingsConverters.SHARED_INT
import com.kieronquinn.app.ambientmusicmod.repositories.BaseSettingsRepositoryImpl.SettingsConverters.SHARED_LONG
import com.kieronquinn.app.ambientmusicmod.repositories.BaseSettingsRepositoryImpl.SettingsConverters.SHARED_STRING
import com.kieronquinn.app.ambientmusicmod.utils.extensions.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface BaseSettingsRepository: KoinComponent {

    val sharedPreferences: SharedPreferences

    abstract class AmbientMusicModSetting<T> {
        abstract suspend fun exists(): Boolean
        abstract fun existsSync(): Boolean
        abstract suspend fun set(value: T)
        abstract suspend fun get(): T
        abstract suspend fun getOrNull(): T?
        abstract suspend fun clear()
        abstract fun getSync(): T
        abstract fun asFlow(): Flow<T>
        abstract fun asFlowNullable(): Flow<T?>
        abstract suspend fun notifyChange()
    }

    /**
     *  Helper implementation of [AmbientMusicModSetting] that takes a regular StateFlow and calls a method
     *  ([onSet]) when [set] is called, allowing for external data to be handled by regular switch
     *  items. [clear] is not implemented, [exists] and [existsSync] will always return true.
     */
    class FakeAmbientMusicModSetting<T>(private val flow: StateFlow<T>, private val onSet: (value: T) -> Unit): AmbientMusicModSetting<T>() {

        override fun getSync(): T {
            return flow.value
        }

        override fun asFlow(): Flow<T> {
            return flow
        }

        override fun asFlowNullable(): Flow<T?> {
            throw RuntimeException("Not implemented!")
        }

        override suspend fun set(value: T) {
            onSet(value)
        }

        override suspend fun get(): T {
            return flow.value
        }

        override suspend fun getOrNull(): T? {
            return if(exists()){
                get()
            }else{
                null
            }
        }

        override suspend fun exists(): Boolean {
            return true
        }

        override fun existsSync(): Boolean {
            return true
        }

        override suspend fun clear() {
            throw RuntimeException("Not implemented!")
        }

        override suspend fun notifyChange() {
            throw RuntimeException("Not implemented!")
        }

    }

}

abstract class BaseSettingsRepositoryImpl: BaseSettingsRepository {

    private val gson by inject<Gson>()

    fun boolean(key: String, default: Boolean, onChanged: MutableSharedFlow<String>? = null) =
        AmbientMusicModSettingImpl(key, default, SHARED_BOOLEAN, onChanged)

    fun string(key: String, default: String, onChanged: MutableSharedFlow<String>? = null) =
        AmbientMusicModSettingImpl(key, default, SHARED_STRING, onChanged)

    fun long(key: String, default: Long, onChanged: MutableSharedFlow<String>? = null) =
        AmbientMusicModSettingImpl(key, default, SHARED_LONG, onChanged)

    fun double(key: String, default: Double, onChanged: MutableSharedFlow<String>? = null) =
        AmbientMusicModSettingImpl(key, default, SHARED_DOUBLE, onChanged)

    fun float(key: String, default: Float, onChanged: MutableSharedFlow<String>? = null) =
        AmbientMusicModSettingImpl(key, default, SHARED_FLOAT, onChanged)

    fun int(key: String, default: Int, onChanged: MutableSharedFlow<String>? = null) =
        AmbientMusicModSettingImpl(key, default, SHARED_INT, onChanged)

    fun color(key: String, default: Int, onChanged: MutableSharedFlow<String>? = null) =
        AmbientMusicModSettingImpl(key, default, SHARED_COLOR, onChanged)

    fun <T : Any> gson(
        key: String,
        default: T,
        onChanged: MutableSharedFlow<String>? = null
    ): AmbientMusicModSettingImpl<T> {
        val sharedGson = { _: BaseSettingsRepositoryImpl, _: String, _: T ->
            sharedGson(key, default)
        }
        return AmbientMusicModSettingImpl(key, default, sharedGson, onChanged)
    }

    inline fun <reified T: Enum<T>> enum(
        key: String,
        default: T,
        onChanged: MutableSharedFlow<String>? = null
    ) = AmbientMusicModSettingImpl(
        key, default, { _, enumKey, enumDefault -> sharedEnum(enumKey, enumDefault) }, onChanged
    )

    private fun shared(key: String, default: Boolean) = ReadWriteProperty({
        sharedPreferences.getBoolean(key, default)
    }, {
        sharedPreferences.edit().putBoolean(key, it).commit()
    })

    private fun shared(key: String, default: String) = ReadWriteProperty({
        sharedPreferences.getString(key, default) ?: default
    }, {
        sharedPreferences.edit().putString(key, it).commit()
    })

    private fun shared(key: String, default: Int) = ReadWriteProperty({
        sharedPreferences.getInt(key, default)
    }, {
        sharedPreferences.edit().putInt(key, it).commit()
    })

    private fun shared(key: String, default: Float) = ReadWriteProperty({
        sharedPreferences.getFloat(key, default)
    }, {
        sharedPreferences.edit().putFloat(key, it).commit()
    })

    private fun shared(key: String, default: Long) = ReadWriteProperty({
        sharedPreferences.getLong(key, default)
    }, {
        sharedPreferences.edit().putLong(key, it).commit()
    })

    private fun shared(key: String, default: Double) = ReadWriteProperty({
        sharedPreferences.getString(key, default.toString())!!.toDouble()
    }, {
        sharedPreferences.edit().putString(key, it.toString()).commit()
    })

    private fun sharedColor(key: String, unusedDefault: Int) = ReadWriteProperty({
        val rawColor = sharedPreferences.getString(key, "") ?: ""
        if(rawColor.isEmpty()) Integer.MAX_VALUE
        else Color.parseColor(rawColor)
    }, {
        sharedPreferences.edit().putString(key, it.toHexString()).commit()
    })

    private fun <T> sharedGson(key: String, default: T) = ReadWriteProperty({
        val rawJson = sharedPreferences.getString(key, "") ?: ""
        try {
            gson.fromJson(rawJson, default!!::class.java)
        }catch (e: Exception){
            default
        }
    }, {
        sharedPreferences.edit().putString(key, gson.toJson(it)).commit()
    })

    object SettingsConverters {
        internal val SHARED_INT: (BaseSettingsRepositoryImpl, String, Int) -> ReadWriteProperty<Any?, Int> =
            BaseSettingsRepositoryImpl::shared
        internal val SHARED_STRING: (BaseSettingsRepositoryImpl, String, String) -> ReadWriteProperty<Any?, String> =
            BaseSettingsRepositoryImpl::shared
        internal val SHARED_BOOLEAN: (BaseSettingsRepositoryImpl, String, Boolean) -> ReadWriteProperty<Any?, Boolean> =
            BaseSettingsRepositoryImpl::shared
        internal val SHARED_FLOAT: (BaseSettingsRepositoryImpl, String, Float) -> ReadWriteProperty<Any?, Float> =
            BaseSettingsRepositoryImpl::shared
        internal val SHARED_LONG: (BaseSettingsRepositoryImpl, String, Long) -> ReadWriteProperty<Any?, Long> =
            BaseSettingsRepositoryImpl::shared
        internal val SHARED_DOUBLE: (BaseSettingsRepositoryImpl, String, Double) -> ReadWriteProperty<Any?, Double> =
            BaseSettingsRepositoryImpl::shared
        internal val SHARED_COLOR: (BaseSettingsRepositoryImpl, String, Int) -> ReadWriteProperty<Any?, Int> =
            BaseSettingsRepositoryImpl::sharedColor
        internal val SHARED_GSON: (BaseSettingsRepositoryImpl, String, Any?) -> ReadWriteProperty<Any?, Any?> =
            BaseSettingsRepositoryImpl::sharedGson
    }

    inner class AmbientMusicModSettingImpl<T>(
        private val key: String,
        private val default: T,
        shared: (BaseSettingsRepositoryImpl, String, T) -> ReadWriteProperty<Any?, T>,
        private val onChange: MutableSharedFlow<String>? = null
    ) : AmbientMusicModSetting<T>() {

        private var rawSetting by shared(this@BaseSettingsRepositoryImpl, key, default)

        override suspend fun exists(): Boolean {
            return withContext(Dispatchers.IO) {
                sharedPreferences.contains(key)
            }
        }

        /**
         *  Should only be used where there is no alternative
         */
        override fun existsSync(): Boolean {
            return runBlocking {
                exists()
            }
        }

        override suspend fun set(value: T) {
            withContext(Dispatchers.IO) {
                rawSetting = value
                onChange?.emit(key)
            }
        }

        override suspend fun get(): T {
            return withContext(Dispatchers.IO) {
                rawSetting ?: default
            }
        }

        override suspend fun getOrNull(): T? {
            return if(exists()){
                get()
            }else null
        }

        /**
         *  Should only be used where there is no alternative
         */
        override fun getSync(): T {
            return runBlocking {
                get()
            }
        }

        override suspend fun clear() {
            withContext(Dispatchers.IO) {
                sharedPreferences.edit().remove(key).commit()
            }
        }

        override fun asFlow() = callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                trySend(rawSetting ?: default)
            }
            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
            trySend(rawSetting ?: default)
            awaitClose {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.flowOn(Dispatchers.IO)

        override fun asFlowNullable() = callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                trySend(rawSetting)
            }
            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
            if(existsSync()) trySend(rawSetting)
            awaitClose {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.flowOn(Dispatchers.IO)

        override suspend fun notifyChange() {
            onChange?.emit(key)
        }

    }

    inline fun <reified T : Enum<T>> sharedEnum(
        key: String,
        default: Enum<T>
    ): ReadWriteProperty<Any?, T> {
        return object : ReadWriteProperty<Any?, T> {

            override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return java.lang.Enum.valueOf(
                    T::class.java,
                    sharedPreferences.getString(key, default.name)
                )
            }

            override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                sharedPreferences.edit().putString(key, value.name).commit()
            }

        }
    }

    protected inline fun <reified T> sharedList(
        key: String,
        default: List<T>,
        crossinline transform: (List<T>) -> String,
        crossinline reverseTransform: (String) -> List<T>
    ) = ReadWriteProperty({
        reverseTransform(sharedPreferences.getString(key, null) ?: transform(default))
    }, {
        sharedPreferences.edit().putString(key, transform(it)).commit()
    })

    private fun stringListTypeConverter(list: List<String>): String {
        if (list.isEmpty()) return ""
        return list.joinToString(",")
    }

    private fun stringListTypeReverseConverter(pref: String): List<String> {
        if (pref.isEmpty()) return emptyList()
        if (!pref.contains(",")) return listOf(pref.trim())
        return pref.split(",")
    }

    protected inline fun <T> ReadWriteProperty(
        crossinline getValue: () -> T,
        crossinline setValue: (T) -> Unit
    ): ReadWriteProperty<Any?, T> {
        return object : ReadWriteProperty<Any?, T> {

            override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return getValue.invoke()
            }

            override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                setValue.invoke(value)
            }

        }
    }

}

suspend fun AmbientMusicModSetting<Boolean>.invert() {
    set(!get())
}