package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.content.ContentValues
import android.database.Cursor

fun <T> Cursor.map(row: (Cursor) -> T): List<T> {
    moveToFirst()
    if(isAfterLast) return emptyList()
    val list = ArrayList<T>()
    do {
        list.add(row(this))
    }while (moveToNext())
    return list
}

fun Cursor.toContentValues(): List<ContentValues> {
    val columns = columnNames
    return map {
        ContentValues().apply {
            columns.forEachIndexed { index, key ->
                put(this@toContentValues, index, key)
            }
        }
    }
}

private fun ContentValues.put(cursor: Cursor, index: Int, key: String) {
    when(cursor.getType(index)){
        Cursor.FIELD_TYPE_STRING -> put(key, cursor.getString(index))
        Cursor.FIELD_TYPE_FLOAT -> put(key, cursor.getFloat(index))
        Cursor.FIELD_TYPE_INTEGER -> put(key, cursor.getInt(index))
        Cursor.FIELD_TYPE_BLOB -> put(key, cursor.getBlob(index))
        else -> {} //No-op
    }
}