package com.kieronquinn.app.ambientmusicmod.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.kieronquinn.app.ambientmusicmod.repositories.DeviceConfigRepository
import org.koin.android.ext.android.inject

class SettingsProvider: ContentProvider() {

    private val deviceConfig by inject<DeviceConfigRepository>()

    override fun onCreate(): Boolean {
        return true
    }

    private fun getDeviceConfig(): Cursor {
        return MatrixCursor(arrayOf("key", "value")).apply {
            deviceConfig.getAllDeviceConfigValues().forEach {
                addRow(arrayOf(it.first, it.second))
            }
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        when(uri.pathSegments[0]){
            "device_config" -> return getDeviceConfig()
        }
        return null
    }

    override fun getType(p0: Uri): String? {
        return null
    }

    override fun insert(p0: Uri, p1: ContentValues?): Uri? {
        return null
    }

    override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int {
        return 0
    }

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int {
        return 0
    }
}