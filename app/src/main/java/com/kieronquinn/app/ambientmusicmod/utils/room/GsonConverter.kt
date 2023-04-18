package com.kieronquinn.app.ambientmusicmod.utils.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object GsonConverter: KoinComponent {

    private val gson by inject<Gson>()
    private val setType = object: TypeToken<Array<String>>(){}.type

    @TypeConverter
    fun fromString(value: String): Array<String> {
        return gson.fromJson(value, setType)
    }

    @TypeConverter
    fun fromArray(array: Array<String>): String {
        return gson.toJson(array)
    }

}