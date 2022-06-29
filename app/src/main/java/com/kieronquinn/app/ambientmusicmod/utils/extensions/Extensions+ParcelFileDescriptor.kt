package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.IOException

fun ParcelFileDescriptor_createPipe(): Pair<ParcelFileDescriptor, ParcelFileDescriptor>? {
    val fileDescriptors: Array<ParcelFileDescriptor> = try {
        ParcelFileDescriptor.createPipe()
    } catch (e: IOException) {
        Log.e("PFD", "Failed to create audio stream pipe", e)
        return null
    }
    if (fileDescriptors.size != 2) {
        Log.e("PFD", "Failed to create audio stream pipe, "
                    + "unexpected number of file descriptors")
        return null
    }
    if (!fileDescriptors[0].fileDescriptor.valid()
        || !fileDescriptors[1].fileDescriptor.valid()
    ) {
        Log.e("PFD", "Failed to create audio stream pipe, didn't "
                    + "receive a pair of valid file descriptors.")
        return null
    }
    return Pair(fileDescriptors[0], fileDescriptors[1])
}