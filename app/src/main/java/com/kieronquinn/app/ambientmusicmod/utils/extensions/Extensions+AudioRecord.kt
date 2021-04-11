package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.app.AndroidAppHelper
import android.content.Context
import android.media.AudioFormat
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.utils.AudioUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

fun Context.getTempInputFile(): File {
    return File(cacheDir, "input.pcm")
}

fun Context.getTempInputFileUri(): Uri {
    val tempInputFile = getTempInputFile()
    return FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", tempInputFile)
}

fun writeAudioRecordToFile(shorts: ShortArray, outputFilename: String = "output.pcm") {
    GlobalScope.launch {
        val context = AndroidAppHelper.currentApplication() as Context
        withContext(Dispatchers.IO){
            try {
                Log.d("AudioRecord", "Write to file start")
                val outputStream = FileOutputStream(File(context.filesDir, outputFilename))
                outputStream.write(AudioUtils.short2byte(shorts))
                outputStream.close()
                Log.d("AudioRecord", "Write to file complete")
            }catch (e: Exception){
                Log.d("AudioRecord", "Error", e)
            }
        }
    }
}

fun writeAudioRecordToUri(shorts: ShortArray, outputUri: Uri) {
    GlobalScope.launch {
        val context = AndroidAppHelper.currentApplication() as Context
        val contentResolver = context.contentResolver
        withContext(Dispatchers.IO){
            try {
                Log.d("AudioRecord", "Write to file with URI ${outputUri.toString()} start")
                val outputStream = contentResolver.openOutputStream(outputUri)
                outputStream?.write(AudioUtils.short2byte(shorts))
                outputStream?.close()
                Log.d("AudioRecord", "Write to file complete")
            }catch (e: Exception){
                Log.d("AudioRecord", "Error", e)
            }
        }
    }
}

fun getAudioFormat(out: Boolean = false): AudioFormat {
    return AudioFormat.Builder().apply {
        setSampleRate(16000)
        setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        if(out){
            setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        }else{
            setChannelMask(AudioFormat.CHANNEL_IN_MONO)
        }
    }.build()
}