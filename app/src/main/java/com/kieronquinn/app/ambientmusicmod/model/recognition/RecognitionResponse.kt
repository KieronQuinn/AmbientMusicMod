package com.kieronquinn.app.ambientmusicmod.model.recognition

import android.util.Log
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.manualtrigger.troubleshooting.SettingsManualTriggerTroubleshootingBottomSheetFragment
import com.kieronquinn.app.ambientmusicmod.utils.ObfuscatedNames

@ObfuscatedNames("search for <UNKNOWN_RECOGNITION_STATUS>")
enum class RecognitionResponse {
    UNKNOWN_RECOGNITION_STATUS, MUSIC_RECOGNIZED, NOT_MUSIC, MUSIC_UNRECOGNIZED
}

fun Any.toRecognitionResponse(): RecognitionResponse {
    val asEnum = this as java.lang.Enum<*>
    return RecognitionResponse.valueOf(asEnum.name())
}

fun RecognitionResponse.toTroubleshootingType(): SettingsManualTriggerTroubleshootingBottomSheetFragment.TroubleshootingType? {
    return when(this){
        RecognitionResponse.MUSIC_RECOGNIZED -> null
        RecognitionResponse.MUSIC_UNRECOGNIZED -> SettingsManualTriggerTroubleshootingBottomSheetFragment.TroubleshootingType.TYPE_NO_MUSIC
        RecognitionResponse.NOT_MUSIC -> SettingsManualTriggerTroubleshootingBottomSheetFragment.TroubleshootingType.TYPE_NOT_MUSIC
        RecognitionResponse.UNKNOWN_RECOGNITION_STATUS -> SettingsManualTriggerTroubleshootingBottomSheetFragment.TroubleshootingType.TYPE_UNKNOWN
    }
}