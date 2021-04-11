package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.content.res.AssetManager
import java.io.File
import java.io.File.separator
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

fun AssetManager.copyAssetFolder(srcName: String, dstName: String): Boolean {
    return try {
        var result = true
        val fileList = this.list(srcName) ?: return false
        if (fileList.size == 0) {
            result = copyAssetFile(srcName, dstName)
        } else {
            val file = File(dstName)
            result = file.mkdirs()
            for (filename in fileList) {
                result = result and copyAssetFolder(
                    srcName + separator.toString() + filename,
                    dstName + separator.toString() + filename
                )
            }
        }
        result
    } catch (e: IOException) {
        e.printStackTrace()
        false
    }
}

fun AssetManager.copyAssetFile(srcName: String, dstName: String): Boolean {
    return try {
        val `in` = this.open(srcName)
        val outFile = File(dstName)
        val out: OutputStream = FileOutputStream(outFile)
        val buffer = ByteArray(1024)
        var read: Int
        while (`in`.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
        `in`.close()
        out.close()
        true
    } catch (e: IOException) {
        e.printStackTrace()
        false
    }
}