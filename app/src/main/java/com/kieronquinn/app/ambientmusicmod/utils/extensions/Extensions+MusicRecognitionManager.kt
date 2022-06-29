package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.media.musicrecognition.IMusicRecognitionAttributionTagCallback
import android.media.musicrecognition.IMusicRecognitionService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

const val MusicRecognitionManager_RECOGNITION_FAILED_NEEDS_ROOT: Int = -2

suspend fun IMusicRecognitionService.getAttributionTag() = suspendCancellableCoroutine<String> {
    getAttributionTag(object: IMusicRecognitionAttributionTagCallback.Stub() {
        override fun onAttributionTag(attributionTag: String) {
            it.resume(attributionTag)
        }
    })
}