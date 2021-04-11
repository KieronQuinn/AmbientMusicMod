package com.kieronquinn.app.ambientmusicmod.app.ui.installer.outputpicker

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import com.kieronquinn.app.ambientmusicmod.constants.BUILD_MODULE_VERSION
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

abstract class InstallerOutputPickerViewModel: BaseViewModel() {

    abstract fun onChooseClicked(createDocument: ActivityResultLauncher<String>)
    abstract fun onOutputPicked(uri: Uri)

}

class InstallerOutputPickerViewModelImpl: InstallerOutputPickerViewModel() {

    companion object {
        private const val BASE_MODULE_FILENAME = "ambient_music_mod_${BUILD_MODULE_VERSION}_%s.zip"
    }

    override fun onChooseClicked(createDocument: ActivityResultLauncher<String>) {
        createDocument.launch(getFilename())
    }

    override fun onOutputPicked(uri: Uri) {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(InstallerOutputPickerFragmentDirections.actionInstallerOutputPickerFragmentToInstallerBuildFragment(uri)))
        }
    }

    /**
     *  As Scoped Storage is a total shitshow and will add the (1), (2), (3) etc AFTER the .zip extension, breaking Magisk, we have to make a unique enough filename
     *  Thanks for this Google, super mature API to enforce on everyone.
     */
    private fun getFilename(): String {
        val timestampFormat = DateTimeFormatter.ofPattern("ddMMyy_HHmmss")
        val timestamp = timestampFormat.format(LocalDateTime.now())
        return String.format(BASE_MODULE_FILENAME, timestamp)
    }

}