package com.kieronquinn.app.ambientmusicmod.components.superpacks

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.components.superpacks.Superpacks.SUPERPACK_AMBIENT_MUSIC_INDEX
import com.kieronquinn.app.ambientmusicmod.utils.extensions.unzip
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices
import kotlinx.coroutines.*
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

fun Context.getTempSuperpacksFile(): File {
    return File(cacheDir, "superpacks.zip")
}

fun Context.getSuperpacksDirectory(): File {
    //Store superpacks in cache in app, so the user can clear cache if they need the space without nuking the prefs
    return if(packageName == PixelAmbientServices.PIXEL_AMBIENT_SERVICES_PACKAGE_NAME){
        File(filesDir, "superpacks")
    }else{
        File(cacheDir, "superpacks")
    }
}

fun Context.getSuperpacksManifestDirectory(): File {
    return File(getSuperpacksDirectory(), "manifests")
}

private fun Context.getSuperpacksIndexFolder(): File {
    return File(getSuperpacksDirectory(), SUPERPACK_AMBIENT_MUSIC_INDEX)
}

fun Context.getSuperpacksFileUri(): Uri {
    val tempSuperpacksFile = getTempSuperpacksFile()
    return FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", tempSuperpacksFile)
}

fun getCoreMatcherFile(): File {
    return File("/system/product/etc/ambient", "matcher_tah.leveldb")
}

object Superpacks {

    const val SUPERPACK_AMBIENT_MUSIC_INDEX = "ambientmusic-index-17_09_02"

    fun extractSuperpacks(context: Context): Boolean {
        val zipFile = context.getTempSuperpacksFile()
        try {
            val outputFolder = context.getSuperpacksDirectory()
            if (!zipFile.exists()) {
                return false
            }
            //Clear the way
            outputFolder.deleteRecursively()
            outputFolder.mkdirs()
            //Extract the zip
            ZipInputStream(FileInputStream(zipFile)).unzip(outputFolder)
            //Delete zip
            zipFile.delete()
            return true
        }catch (e: EOFException){
            //Zip corrupted
            //Delete zip
            zipFile.delete()
            return false
        }
    }

    /**
     *  Runs a given task for each superpack file. The tasks are automatically threaded to speed up searches.
     *  Returns if any task returned 'false' (ie. failed)
     */
    fun forEachSuperpack(context: Context, coreFile: File? = null, forEach: suspend (File, Int, Int) -> Boolean): Boolean = runBlocking {
        val list = ArrayList<File>()
        if(coreFile != null) list.add(coreFile)
        list.addAll(context.getSuperpacksIndexFolder().listFiles()?.filter { !it.name.startsWith("cc-") } ?: emptyList())
        val jobs = list.mapIndexed { index, file ->
            async(Dispatchers.Default) {
                forEach.invoke(file, index, list.size)
            }
        }
        !jobs.any { !it.await() }
    }

    /**
     *  Returns the newest version number of a given superpack by listing manifests and selecting the one with the highest version
     */
    fun getSuperpackVersion(context: Context, superpackName: String): Int {
        context.getSuperpacksManifestDirectory().run {
            if(!this.exists()) return 0
            val regex = "$superpackName-(.*)".toRegex()
            val versions = list()?.mapNotNull { regex.find(it)?.groupValues?.get(1)?.toIntOrNull() }
            return versions?.maxOrNull() ?: 0
        }
    }

}