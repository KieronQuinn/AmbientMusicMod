package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import com.kieronquinn.app.ambientmusicmod.repositories.ApiRepository

fun ContentResolver.safeQuery(
    uri: Uri,
    projection: Array<String?>?,
    selection: String?,
    selectionArgs: Array<String?>?,
    sortOrder: String?
): Cursor? {
    ApiRepository.assertCompatibility()
    return try {
        query(uri, projection, selection, selectionArgs, sortOrder)
    }catch (e: SecurityException){
        //Provider not found
        null
    }
}

fun ContentResolver.safeRegisterContentObserver(
    uri: Uri, notifyForDescendants: Boolean, observer: ContentObserver
) {
    return try {
        registerContentObserver(uri, notifyForDescendants, observer)
    }catch (e: SecurityException){
        //Provider not found
    }
}