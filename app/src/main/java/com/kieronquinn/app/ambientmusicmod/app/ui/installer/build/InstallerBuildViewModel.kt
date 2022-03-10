package com.kieronquinn.app.ambientmusicmod.app.ui.installer.build

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.installer.Installer
import com.kieronquinn.app.ambientmusicmod.components.installer.SoundTriggerPlatformXML
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class InstallerBuildViewModel: BaseViewModel() {

    abstract val installState: Flow<InstallState>

    abstract fun start(uri: Uri)
    abstract fun onCloseClicked()

    sealed class InstallState {
        object Idle: InstallState()
        data class CreatingModule(val outputUri: Uri): InstallState()
        data class ZippingModule(val outputUri: Uri): InstallState()
        data class CopyingModule(val outputUri: Uri): InstallState()
        data class Done(val outputFilename: String): InstallState()
        data class Error(val outputUri: Uri, val errorReason: ErrorReason): InstallState()
    }

    sealed class ErrorReason {
        data class CreateFailure(val reason: SoundTriggerPlatformXML.CurrentXMLType): ErrorReason()
        object ZipFailure: ErrorReason()
        object CopyFailure: ErrorReason()
    }

}

class InstallerBuildViewModelImpl(private val context: Context): InstallerBuildViewModel() {

    private val _installState = MutableStateFlow<InstallState>(InstallState.Idle)
    override val installState = _installState.asStateFlow().apply {
        viewModelScope.launch {
            collect {
                when(it){
                    is InstallState.CreatingModule -> createModule(it.outputUri)
                    is InstallState.ZippingModule -> zipModule(it.outputUri)
                    is InstallState.CopyingModule -> copyModule(it.outputUri)
                    is InstallState.Error -> runCleanup(it.outputUri)
                    is InstallState.Done -> runCleanup()
                }
            }

        }
    }

    private fun runCleanup(outputUri: Uri? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                Installer.cleanup(context, outputUri)
            }
        }
    }

    override fun start(uri: Uri){
        viewModelScope.launch {
            if(_installState.value is InstallState.Idle){
                _installState.emit(InstallState.CreatingModule(uri))
            }
        }
    }

    override fun onCloseClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateUp(R.id.installerFragment))
        }
    }

    private fun createModule(outputUri: Uri) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            when (val result = Installer.createModule(context)) {
                is Installer.XMLCreationResult.Success -> _installState.emit(InstallState.ZippingModule(outputUri))
                is Installer.XMLCreationResult.Failed -> _installState.emit(InstallState.Error(outputUri, ErrorReason.CreateFailure(result.reason)))
            }
        }
    }

    private fun zipModule(outputUri: Uri) = viewModelScope.launch {
        withContext(Dispatchers.IO){
            if(Installer.zipModule(context)){
                _installState.emit(InstallState.CopyingModule(outputUri))
            }else{
                _installState.emit(InstallState.Error(outputUri, ErrorReason.ZipFailure))
            }
        }
    }

    private fun copyModule(outputUri: Uri) = viewModelScope.launch {
        withContext(Dispatchers.IO){
            if(Installer.copyModuleToOutput(context, outputUri)){
                val documentFile = DocumentFile.fromSingleUri(context, outputUri)!!
                _installState.emit(InstallState.Done(documentFile.name ?: ""))
            }else{
                _installState.emit(InstallState.Error(outputUri, ErrorReason.CopyFailure))
            }
        }
    }

}
