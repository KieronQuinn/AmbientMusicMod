package com.kieronquinn.app.ambientmusicmod.components.installer

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Xml
import androidx.documentfile.provider.DocumentFile
import com.kieronquinn.app.ambientmusicmod.app.ui.installer.InstallerViewModelImpl
import com.kieronquinn.app.ambientmusicmod.constants.*
import com.kieronquinn.app.ambientmusicmod.utils.extensions.copyAssetFolder
import com.kieronquinn.app.ambientmusicmod.utils.extensions.zipDirectory
import org.xmlpull.v1.XmlPullParserFactory
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Installer {

    private const val TEMP_MODULE_NAME = "magisk_module.zip"
    private const val MODULE_NAME = "Ambient"

    private const val SOUND_TRIGGER_LIB_PATH = "/system/vendor/lib/hw"
    private const val SOUND_TRIGGER_LIB64_PATH = "/system/vendor/lib64/hw"

    private val SOUND_TRIGGER_FILENAME_PATTERN = "android.hardware.soundtrigger@(.*)-impl.so".toRegex()

    fun createModule(context: Context): XMLCreationResult {
        //Extract module from assets
        val assetManager = context.assets
        assetManager.copyAssetFolder("magisk", context.filesDir.absolutePath)
        //Create XML folders
        val outputXml = File(context.filesDir.absolutePath + "/Ambient/" + SOUND_TRIGGER_PLATFORM_PATH)
        outputXml.parentFile?.mkdirs()
        //Create the actual XML or fail
        val result = createXML(outputXml)
        val customiseFile = File(context.filesDir.absolutePath + "/Ambient", "customize.sh")
        addBuildDetailsToCustomise(customiseFile)
        return result
    }

    private fun addBuildDetailsToCustomise(file: File){
        file.run {
            var text = readText()
            text = text.replace("%BUILDMODEL%", Build.MODEL)
            text = text.replace("%BUILDFIRMVER%", Build.ID)
            writeText(text)
        }
    }

    fun zipModule(context: Context): Boolean {
        val outputZip = File(context.filesDir, TEMP_MODULE_NAME)
        val moduleDir = File(context.filesDir, MODULE_NAME)
        runCatching {
            val serviceShFile = File(moduleDir, "service.sh")
            appendVersionPropsToScript(serviceShFile)
            moduleDir.zipDirectory(outputZip)
        }.onSuccess {
            return true
        }
        return false
    }

    fun copyModuleToOutput(context: Context, outputUri: Uri): Boolean {
        runCatching {
            val outputZip = File(context.filesDir, TEMP_MODULE_NAME)
            if(!outputZip.exists()) return false
            val documentFile = DocumentFile.fromSingleUri(context, outputUri) ?: return false
            val outputStream = context.contentResolver.openOutputStream(documentFile.uri) ?: return false
            outputZip.inputStream().copyTo(outputStream)
        }.onSuccess {
            return true
        }
        return false
    }

    private fun createXML(outputFile: File): XMLCreationResult {
        val output = FileOutputStream(outputFile)
        val xmlSerializer = Xml.newSerializer().apply {
            setOutput(output, Charsets.ISO_8859_1.name())
        }
        val fileInputStream = File(SOUND_TRIGGER_PLATFORM_PATH).inputStream()
        val xmlParser = XmlPullParserFactory.newInstance().newPullParser().apply {
            setInput(fileInputStream, Charsets.ISO_8859_1.name())
        }
        SoundTriggerPlatformXML(xmlSerializer).run {
            //Runs until a suitable point of injection, cloning as it goes
            val result = xmlParser.skipUntilInjectionPoint()
            Log.d("Installer", "XML injection type $result")
            when(result){
                SoundTriggerPlatformXML.CurrentXMLType.PARTIAL_MUSIC -> {
                    //Inject just the usecase section and move on
                    writeParitalMusicSection(2)
                    xmlParser.skipUntilEndOfSoundModelConfig()
                }
                SoundTriggerPlatformXML.CurrentXMLType.NO_MUSIC -> {
                    //Inject the whole Music section (may not work for all)
                    writeGoogleMusicDetectionSection(2)
                }
                SoundTriggerPlatformXML.CurrentXMLType.INCOMPATIBLE -> {
                    //The XML was not formatted as expected
                    output.flush()
                    output.close()
                    fileInputStream.close()
                    return XMLCreationResult.Failed(result)
                }
                SoundTriggerPlatformXML.CurrentXMLType.ALREADY_INSTALLED -> {
                    //The module is already installed, we cannot install on top of a current install (make user disable -> reboot -> try again)
                    output.flush()
                    output.close()
                    fileInputStream.close()
                    return XMLCreationResult.Failed(result)
                }
            }
            //Finish the clone job
            xmlParser.cloneUntilEndOfDocument()
        }
        xmlSerializer.endDocument()
        output.flush()
        output.close()
        fileInputStream.close()
        return XMLCreationResult.Success
    }

    fun cleanup(context: Context, outputUri: Uri? = null){
        val moduleDir = File(context.filesDir, MODULE_NAME)
        if(moduleDir.exists()) moduleDir.deleteRecursively()
        val outputZip = File(context.filesDir, TEMP_MODULE_NAME)
        if(outputZip.exists()) outputZip.delete()
        if(outputUri == null) return
        DocumentFile.fromSingleUri(context, outputUri)?.let {
            if(it.exists()) it.delete()
        }
    }

    private fun appendVersionPropsToScript(file: File){
        FileOutputStream(file, true).bufferedWriter().use {
            it.appendLine("resetprop $MODULE_VERSION_PROP \"${BUILD_MODULE_VERSION}\"")
            it.appendLine("resetprop $MODULE_VERSION_CODE_PROP $BUILD_MODULE_VERSION_CODE")
        }
    }


    fun getMaxSoundTriggerVersion(): Double {
        val libFiles = (File(SOUND_TRIGGER_LIB_PATH).listFiles() ?: emptyArray()) + (File(SOUND_TRIGGER_LIB64_PATH).listFiles() ?: emptyArray())
        return libFiles.mapNotNull { SOUND_TRIGGER_FILENAME_PATTERN.matchEntire(it.name)?.groups?.get(1)?.value?.toDoubleOrNull() }.maxOrNull() ?: 0.0
    }


    sealed class XMLCreationResult {
        object Success: XMLCreationResult()
        data class Failed(val reason: SoundTriggerPlatformXML.CurrentXMLType): XMLCreationResult()
    }

}