package com.kieronquinn.app.ambientmusicmod.app.ui.settings.developer.log.dump

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.manualtrigger.playback.SettingsManualTriggerPlaybackBottomSheetViewModel
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.installer.Installer
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import com.kieronquinn.app.ambientmusicmod.constants.MODULE_VERSION_CODE_PROP
import com.kieronquinn.app.ambientmusicmod.constants.MODULE_VERSION_PROP
import com.kieronquinn.app.ambientmusicmod.constants.SOUND_TRIGGER_PLATFORM_PATH
import com.kieronquinn.app.ambientmusicmod.constants.SOUND_TRIGGER_PLATFORM_PATH_BACKUP
import com.kieronquinn.app.ambientmusicmod.utils.ModuleStateCheck
import com.kieronquinn.app.ambientmusicmod.utils.extensions.SystemProperties_getInt
import com.kieronquinn.app.ambientmusicmod.utils.extensions.SystemProperties_getString
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getAppVersion
import com.kieronquinn.app.ambientmusicmod.utils.extensions.isXposedInstalled
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.collections.ArrayList

abstract class SettingsDeveloperOptionsDumpLogViewModel: BaseViewModel() {

    abstract val state: Flow<State>

    abstract fun onOutputPickerLaunch(activityResultLauncher: ActivityResultLauncher<String>)
    abstract fun onOutputSelected(outputUri: Uri)
    abstract fun cancelAndClose()
    abstract fun close()

    sealed class State {
        object OutputSelection: State()
        abstract class OutputState(open val outputUri: Uri): State()
        data class RequestingRoot(override val outputUri: Uri): OutputState(outputUri)
        data class DumpLogs(override val outputUri: Uri, val shell: Shell): OutputState(outputUri)
        object Done: State()
        object NoRoot: State()
        data class Error(override val outputUri: Uri): OutputState(outputUri)
    }

}

class SettingsDeveloperOptionsDumpLogViewModelImpl(private val context: Context): SettingsDeveloperOptionsDumpLogViewModel() {

    private val _state = MutableStateFlow<State>(State.OutputSelection)
    override val state = _state.asStateFlow().apply {
        viewModelScope.launch {
            collect {
                when(it){
                    is State.RequestingRoot -> requestRoot(it.outputUri)
                    is State.DumpLogs -> dumpLogs(it.outputUri, it.shell)
                }
            }
        }
    }

    private var dumpTask: Job? = null

    override fun onOutputPickerLaunch(activityResultLauncher: ActivityResultLauncher<String>) {
        viewModelScope.launch {
            val suggestedName = "ambient_music_mod_log_dump_${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())}.zip"
            activityResultLauncher.launch(suggestedName)
        }
    }

    override fun onOutputSelected(outputUri: Uri) {
        viewModelScope.launch {
            _state.emit(State.RequestingRoot(outputUri))
        }
    }

    private suspend fun requestRoot(outputUri: Uri) {
        withContext(Dispatchers.IO) {
            val shell = Shell.getShell()
            if(shell.isRoot) {
                _state.emit(State.DumpLogs(outputUri, shell))
            }else{
                _state.emit(State.NoRoot)
            }
        }
    }

    override fun cancelAndClose() {
        viewModelScope.launch {
            val state = _state.value
            if(state is State.OutputState) {
                dumpTask?.cancel()
                //Delete output file
                DocumentFile.fromSingleUri(context, state.outputUri)?.let {
                    if (it.exists()) it.delete()
                }
                navigation.navigate(NavigationEvent.NavigateUp())
            }
        }
    }

    override fun close() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateUp())
        }
    }

    private fun dumpLogs(outputUri: Uri, shell: Shell) {
        dumpTask = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val fileOutput = context.contentResolver.openOutputStream(outputUri)
                            ?: return@withContext
                    val output = ZipOutputStream(fileOutput)
                    val version = SystemProperties_getInt(MODULE_VERSION_CODE_PROP, -1)
                    val versionName = SystemProperties_getString(MODULE_VERSION_PROP, "")
                    val metadata = arrayOf(
                        "Installed module version $versionName ($version)",
                        "Pixel Ambient Services version ${context.packageManager.getAppVersion(PixelAmbientServices.PIXEL_AMBIENT_SERVICES_PACKAGE_NAME)}",
                        "Ambient Music Mod version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        "Device Personalization Services version ${context.packageManager.getAppVersion("com.google.android.as")}",
                        "Sound trigger version ${Installer.getMaxSoundTriggerVersion()}",
                        "Module enabled ${ModuleStateCheck.isModuleEnabled()}",
                        "Xposed installed ${context.isXposedInstalled()}",
                        "Last Module Check result ${settings.getModelLastResult} (${settings.getModelSupported.name})"
                    )
                    val soundTriggerLogs = ArrayList<String>().apply {
                        shell.newJob().to(this).apply {
                            add("logcat -d")
                        }.exec()
                    }.filter { it.toLowerCase(Locale.getDefault()).run { it.contains("soundtrigger") || contains("sound_trigger") } }
                    val pixelAmbientServicesLogs = ArrayList<String>().apply {
                        shell.newJob().to(this).apply {
                            add("logcat -d --pid \$(pidof ${PixelAmbientServices.PIXEL_AMBIENT_SERVICES_PACKAGE_NAME})")
                        }.exec()
                    }
                    val ambientMusicModLogs = ArrayList<String>().apply {
                        shell.newJob().to(this).apply {
                            add("logcat -d --pid \$(pidof ${BuildConfig.APPLICATION_ID})")
                        }.exec()
                    }
                    val soundTriggerPlatformInfo = File(SOUND_TRIGGER_PLATFORM_PATH).readBytes()
                    val soundTriggerPlatformInfoBackup = File(SOUND_TRIGGER_PLATFORM_PATH_BACKUP).readBytesOrNull()
                    val metadataEntry = ZipEntry("metadata.txt")
                    output.putNextEntry(metadataEntry)
                    output.write(metadata.joinToString("\n").toByteArray())
                    output.closeEntry()
                    val soundTriggerEntry = ZipEntry("soundtrigger_log.txt")
                    output.putNextEntry(soundTriggerEntry)
                    output.write(soundTriggerLogs.joinToString("\n").toByteArray())
                    output.closeEntry()
                    val pixelAmbientServicesEntry = ZipEntry("pixel_ambient_services_log.txt")
                    output.putNextEntry(pixelAmbientServicesEntry)
                    output.write(pixelAmbientServicesLogs.joinToString("\n").toByteArray())
                    output.closeEntry()
                    val ambientMusicModEntry = ZipEntry("ambient_music_mod_logs.txt")
                    output.putNextEntry(ambientMusicModEntry)
                    output.write(ambientMusicModLogs.joinToString("\n").toByteArray())
                    output.closeEntry()
                    val soundTriggerPlatformInfoEntry = ZipEntry("sound_trigger_platform_info.xml")
                    output.putNextEntry(soundTriggerPlatformInfoEntry)
                    output.write(soundTriggerPlatformInfo)
                    output.closeEntry()
                    val soundTriggerPlatformInfoEntryOriginal = ZipEntry("sound_trigger_platform_info_original.xml")
                    output.putNextEntry(soundTriggerPlatformInfoEntryOriginal)
                    output.write(soundTriggerPlatformInfoBackup)
                    output.closeEntry()
                    output.finish()
                    output.flush()
                    fileOutput.close()
                }.onFailure {
                    _state.emit(State.Error(outputUri))
                }.onSuccess {
                    _state.emit(State.Done)
                }
            }
        }
    }

    private fun File.readBytesOrNull(): ByteArray {
        if(!exists()) return "".toByteArray()
        return readBytes()
    }

}