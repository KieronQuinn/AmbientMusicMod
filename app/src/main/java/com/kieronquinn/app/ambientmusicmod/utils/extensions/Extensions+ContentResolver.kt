package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
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

fun ContentResolver.safeUpdate(
    uri: Uri,
    values: ContentValues,
    selection: String,
    selectionArgs: Array<out String>
): Int {
    ApiRepository.assertCompatibility()
    return try {
        update(uri, values, selection, selectionArgs)
    }catch (e: SecurityException){
        //Provider not found
        0
    }
}

fun ContentResolver.safeDelete(
    uri: Uri,
    selection: String,
    selectionArgs: Array<out String>
): Int {
    ApiRepository.assertCompatibility()
    return try {
        delete(uri, selection, selectionArgs)
    }catch (e: SecurityException){
        //Provider not found
        0
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

fun ContentResolver.takeUriPermission(
    uri: Uri,
    flags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
) {
    takePersistableUriPermission(uri, flags)
}