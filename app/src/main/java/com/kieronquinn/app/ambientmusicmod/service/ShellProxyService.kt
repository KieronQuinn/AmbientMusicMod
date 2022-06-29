package com.kieronquinn.app.ambientmusicmod.service

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.musicrecognition.RecognitionRequest
import android.os.IBinder
import com.kieronquinn.app.ambientmusicmod.IMicrophoneDisabledStateCallback
import com.kieronquinn.app.ambientmusicmod.IRecognitionCallback
import com.kieronquinn.app.ambientmusicmod.IShellProxy
import com.kieronquinn.app.ambientmusicmod.repositories.ShizukuServiceRepository
import com.kieronquinn.app.ambientmusicmod.repositories.ShizukuServiceRepository.ShizukuServiceResponse
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getActivityToken
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getApplicationThread
import org.koin.android.ext.android.inject

/**
 *  Proxy service that passes on [IShellProxy] requests to the Shizuku service and returns the
 *  response, if available. If the service is not available, an exception is thrown.
 *
 *  **It is the responsibility of the caller to minimise the risk of the Shizuku service being
 *  unavailable when using this service. Since the service is only used as a result of calls from
 *  AMM, the app simply makes sure that Shizuku is ready before calling out to PAM.**
 */
class ShellProxyService: Service() {

    private val service = ShellProxyServiceImpl()
    private val shizuku by inject<ShizukuServiceRepository>()

    override fun onBind(intent: Intent): IBinder {
        return service
    }

    /**
     *  Runs with the service or throws an exception if it is unavailable. Exceptions are absorbed
     *  in ASI, and are the responsibility of the calling app to handle
     */
    private fun <T> runWithService(block: (IShellProxy) -> T): T {
        return when (val result = shizuku.runWithServiceIfAvailable(block)) {
            is ShizukuServiceResponse.Success -> result.result
            is ShizukuServiceResponse.Failed -> {
                throw RuntimeException("Service is unavailable (${result.reason.name})")
            }
        }
    }

    private inner class ShellProxyServiceImpl: IShellProxy.Stub() {
        override fun AudioRecord_create(
            attributes: AudioAttributes?,
            audioFormat: AudioFormat?,
            sessionId: Int,
            bufferSizeInBytes: Int
        ) {
            runWithService {
                it.AudioRecord_create(attributes, audioFormat, sessionId, bufferSizeInBytes)
            }
        }

        override fun AudioRecord_startRecording() {
            runWithService {
                it.AudioRecord_startRecording()
            }
        }

        override fun AudioRecord_release() {
            runWithService {
                it.AudioRecord_release()
            }
        }

        override fun AudioRecord_read(
            audioData: ByteArray?,
            offsetInShorts: Int,
            sizeInShorts: Int
        ): Int {
            return runWithService {
                it.AudioRecord_read(audioData, offsetInShorts, sizeInShorts)
            }
        }

        override fun AudioRecord_getFormat(): AudioFormat {
            return runWithService {
                it.AudioRecord_getFormat()
            }
        }

        override fun AudioRecord_getBufferSizeInFrames(): Int {
            return runWithService {
                it.AudioRecord_getBufferSizeInFrames()
            }
        }

        override fun AudioRecord_getSampleRate(): Int {
            return runWithService {
                it.AudioRecord_getSampleRate()
            }
        }

        override fun MusicRecognitionManager_beginStreamingSearch(
            request: RecognitionRequest?,
            callback: IRecognitionCallback?
        ) {
            val thread = getApplicationThread().asBinder()
            val token = getActivityToken()
            runWithService {
                it.MusicRecognitionManager_beginStreamingSearchWithThread(
                    request,
                    callback,
                    thread,
                    token
                )
            }
        }

        override fun ping(): Boolean {
            return runWithService {
                it.ping()
            }
        }

        override fun isMicrophoneDisabled(): Boolean {
            return runWithService {
                it.isMicrophoneDisabled
            }
        }

        override fun addMicrophoneDisabledListener(callback: IMicrophoneDisabledStateCallback): String {
            return runWithService {
                it.addMicrophoneDisabledListener(callback)
            }
        }

        override fun removeMicrophoneDisabledListener(id: String) {
            return runWithService {
                it.removeMicrophoneDisabledListener(id)
            }
        }

        override fun getSystemUIPackageName(): String {
            throw SecurityException("Not exposed to external access")
        }

        override fun dismissKeyguard(callback: IBinder?, message: String?) {
            throw SecurityException("Not exposed to external access")
        }

        override fun isCompatible(): Boolean {
            throw SecurityException("Not exposed to external access")
        }

        override fun isRoot(): Boolean {
            throw SecurityException("Not exposed to external access")
        }

        override fun grantAccessibilityPermission() {
            throw SecurityException("Not exposed to external access")
        }

        override fun setOwnerInfo(info: String?) {
            throw SecurityException("Not exposed to external access")
        }

        override fun forceStopNowPlaying() {
            throw SecurityException("Not exposed to external access")
        }

        override fun MusicRecognitionManager_beginStreamingSearchWithThread(
            request: RecognitionRequest?,
            callback: IRecognitionCallback?,
            thread: IBinder?,
            token: IBinder?
        ) {
            throw SecurityException("Not exposed to external access")
        }

        override fun destroy() {
            //No-op at this level
        }
    }

}