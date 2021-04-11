package com.kieronquinn.app.ambientmusicmod.utils.extensions

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun File.zipDirectory(outputZipFile: File) {
    ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipFile))).use { zos ->
        walkTopDown().forEach { file ->
            val zipFileName = file.absolutePath.removePrefix(absolutePath).removePrefix("/")
            if(zipFileName.isEmpty()) return@forEach
            val entry = ZipEntry( "$zipFileName${(if (file.isDirectory) "/" else "" )}")
            zos.putNextEntry(entry)
            if (file.isFile) {
                file.inputStream().copyTo(zos)
            }
        }
    }
}

fun File.zipDirectory(outputStream: OutputStream, compressionLevel: Int? = null) {
    ZipOutputStream(BufferedOutputStream(outputStream)).use { zos ->
        compressionLevel?.let {
            zos.setLevel(compressionLevel)
        }
        walkTopDown().forEach { file ->
            val zipFileName = file.absolutePath.removePrefix(absolutePath).removePrefix("/")
            if(zipFileName.isEmpty()) return@forEach
            val entry = ZipEntry( "$zipFileName${(if (file.isDirectory) "/" else "" )}")
            zos.putNextEntry(entry)
            if (file.isFile) {
                file.inputStream().copyTo(zos)
            }
        }
    }
}

fun ZipInputStream.unzip(location: File) {
    if (location.exists() && !location.isDirectory)
        throw IllegalStateException("Location file must be directory or not exist")

    if (!location.isDirectory) location.mkdirs()

    val locationPath = location.absolutePath.let {
        if (!it.endsWith(File.separator)) "$it${File.separator}"
        else it
    }

    var zipEntry: ZipEntry?
    var unzipFile: File
    var unzipParentDir: File?

    while (nextEntry.also { zipEntry = it } != null) {
        unzipFile = File(locationPath + zipEntry!!.name)
        if (zipEntry!!.isDirectory) {
            if (!unzipFile.isDirectory) unzipFile.mkdirs()
        } else {
            unzipParentDir = unzipFile.parentFile
            if (unzipParentDir != null && !unzipParentDir.isDirectory) {
                unzipParentDir.mkdirs()
            }
            BufferedOutputStream(FileOutputStream(unzipFile)).use { outStream ->
                copyTo(outStream)
            }
        }
    }
}