package com.kieronquinn.app.ambientmusicmod.components.musicrecognition

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.IActivityManager
import android.app.IApplicationThread
import android.app.IServiceConnection
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaMetadata
import android.media.musicrecognition.IMusicRecognitionManagerCallback
import android.media.musicrecognition.IMusicRecognitionService
import android.media.musicrecognition.IMusicRecognitionServiceCallback
import android.media.musicrecognition.MusicRecognitionManager.*
import android.media.musicrecognition.RecognitionRequest
import android.os.*
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import com.kieronquinn.app.ambientmusicmod.PACKAGE_NAME_GSB
import com.kieronquinn.app.ambientmusicmod.utils.context.ShellContext
import com.kieronquinn.app.ambientmusicmod.utils.extensions.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import rikka.shizuku.SystemServiceHelper
import java.io.IOException
import java.io.OutputStream
import kotlin.coroutines.resume

/**
 *  Root re-implementation of MusicRecognitionManager, directly binding the Google App's music
 *  recognition service and interacting with it.
 *
 *  Requires audio recording via the `HOTWORD` mic to work.
 */
@RequiresApi(Build.VERSION_CODES.S)
class RootMusicRecognitionManager(private val context: Context, userId: Int) {

    companion object {
        private const val DEBUG = false
        private const val TAG = "RootMRM"
        private const val SERVICE_CONNECT_TIMEOUT = 2500L

        // Number of bytes per sample of audio (which is a short).
        private const val BYTES_PER_SAMPLE = 2
        private const val MAX_STREAMING_SECONDS = 24

        private const val MUSIC_RECOGNITION_MANAGER_ATTRIBUTION_TAG =
            "MusicRecognitionManagerService"
    }

    private val appOpsManager = context.createAttributionContext(
        MUSIC_RECOGNITION_MANAGER_ATTRIBUTION_TAG
    ).getSystemService(AppOpsManager::class.java)

    private val attributionMessage = String.format(
        "MusicRecognitionManager.invokedByUid.%s", userId
    )

    private val musicRecognitionIntent by lazy {
        Intent("android.service.musicrecognition.MUSIC_RECOGNITION").apply {
            `package` = PACKAGE_NAME_GSB
        }
    }

    private val activityManager by lazy {
        val proxy = SystemServiceHelper.getSystemService("activity")
        IActivityManager.Stub.asInterface(proxy)
    }

    private var musicRecognitionBinder: IBinder? = null
    private var musicRecognitionServiceConnection: IServiceConnection? = null
    private val recognitionServiceLock = Mutex()

    suspend fun runStreamingSearch(
        lifecycleScope: CoroutineScope,
        request: RecognitionRequest,
        callback: IMusicRecognitionManagerCallback,
        thread: IBinder,
        token: IBinder?
    ) {
        val serviceInfo = getServiceInfo()
        if(serviceInfo == null){
            callback.onRecognitionFailed(RECOGNITION_FAILED_SERVICE_UNAVAILABLE)
            return
        }
        val service = getMusicRecognitionService(IApplicationThread.Stub.asInterface(thread), token)
        if(service == null){
            callback.onRecognitionFailed(RECOGNITION_FAILED_SERVICE_UNAVAILABLE)
            return
        }
        val clientPipe = ParcelFileDescriptor_createPipe()
        if(clientPipe == null){
            callback.onRecognitionFailed(RECOGNITION_FAILED_AUDIO_UNAVAILABLE)
            return
        }
        val serviceCallback = MusicRecognitionServiceCallback(callback)
        val audioSink = clientPipe.second
        val clientRead = clientPipe.first
        val attributionTag = service.getAttributionTag()
        lifecycleScope.streamAudio(serviceInfo, attributionTag, request, callback, audioSink)
        service.onAudioStreamStarted(clientRead, request.audioFormat, serviceCallback)
    }

    /**
     * Streams audio based on given request to the given audioSink. Notifies callback of errors.
     *
     * @param recognitionRequest the recognition request specifying audio parameters.
     * @param clientCallback the callback to notify on errors.
     * @param audioSink the sink to which to stream audio to.
     */
    private fun CoroutineScope.streamAudio(
        serviceInfo: ServiceInfo,
        @Nullable attributionTag: String,
        @NonNull recognitionRequest: RecognitionRequest,
        clientCallback: IMusicRecognitionManagerCallback,
        audioSink: ParcelFileDescriptor
    ) = launch {
        val maxAudioLengthSeconds: Int =
            recognitionRequest.maxAudioLengthSeconds.coerceAtMost(MAX_STREAMING_SECONDS)
        if (maxAudioLengthSeconds <= 0) {
            // TODO(b/192992319): A request to stream 0s of audio can be used to initialize the
            //  music recognition service implementation, hence not reporting an error here.
            // The TODO for Android T is to move this functionality into an init() API call.
            Log_i("No audio requested. Closing stream.")
            try {
                audioSink.close()
                clientCallback.onAudioStreamClosed()
            } catch (e: IOException) {
                Log_e("Problem closing stream.", e)
            } catch (ignored: RemoteException) {
                // Ignored.
            }
            return@launch
        }
        try {
            startRecordAudioOp(serviceInfo, attributionTag)
        } catch (e: SecurityException) {
            // A security exception can occur if the MusicRecognitionService (receiving the audio)
            // does not (or does no longer) hold the necessary permissions to record audio.
            Log_e("RECORD_AUDIO op not permitted on behalf of service")
            try {
                clientCallback.onRecognitionFailed(RECOGNITION_FAILED_AUDIO_UNAVAILABLE)
            } catch (ignored: RemoteException) {
                // Ignored.
            }
            return@launch
        }
        val audioRecord: AudioRecord = createAudioRecord(recognitionRequest, maxAudioLengthSeconds)
        try {
            ParcelFileDescriptor.AutoCloseOutputStream(audioSink).use { fos ->
                streamAudio(recognitionRequest, maxAudioLengthSeconds, audioRecord, fos)
            }
        } catch (e: IOException) {
            Log_e("Audio streaming stopped.", e)
        } finally {
            finishRecordAudioOp(serviceInfo, attributionTag)
            audioRecord.release()
            try {
                clientCallback.onAudioStreamClosed()
            } catch (ignored: RemoteException) {
                // Ignored.
            }
        }
    }

    /** Performs the actual streaming from audioRecord into outputStream.  */
    @Throws(IOException::class)
    private fun streamAudio(
        @NonNull recognitionRequest: RecognitionRequest,
        maxAudioLengthSeconds: Int, audioRecord: AudioRecord, outputStream: OutputStream
    ) {
        val halfSecondBufferSize = audioRecord.bufferSizeInFrames / maxAudioLengthSeconds
        val byteBuffer = ByteArray(halfSecondBufferSize)
        var bytesRead = 0
        var totalBytesRead = 0
        var ignoreBytes: Int = recognitionRequest.ignoreBeginningFrames * BYTES_PER_SAMPLE
        audioRecord.startRecording()
        while (bytesRead >= 0 && (totalBytesRead < audioRecord.bufferSizeInFrames * BYTES_PER_SAMPLE) && musicRecognitionBinder != null) {
            bytesRead = audioRecord.read(byteBuffer, 0, byteBuffer.size)
            if (bytesRead > 0) {
                totalBytesRead += bytesRead
                // If we are ignoring the first x bytes, update that counter.
                if (ignoreBytes > 0) {
                    ignoreBytes -= bytesRead
                    // If we've dipped negative, we've skipped through all ignored bytes
                    // and then some.  Write out the bytes we shouldn't have skipped.
                    if (ignoreBytes < 0) {
                        outputStream.write(byteBuffer, bytesRead + ignoreBytes, -ignoreBytes)
                    }
                } else {
                    outputStream.write(byteBuffer)
                }
            }
        }
        Log_i(String.format("Streamed %s bytes from audio record", totalBytesRead))
    }

    /**
     * Tracks that the RECORD_AUDIO operation started (attributes it to the service receiving the
     * audio).
     */
    private fun startRecordAudioOp(serviceInfo: ServiceInfo, attributionTag: String?) {
        val status: Int = appOpsManager.startProxyOp(
            AppOpsManager.permissionToOp(Manifest.permission.RECORD_AUDIO)!!,
            serviceInfo.applicationInfo.uid,
            serviceInfo.packageName,
            attributionTag,
            attributionMessage
        )
        // The above should already throw a SecurityException. This is just a fallback.
        if (status != AppOpsManager.MODE_ALLOWED) {
            throw SecurityException(String.format(
                "Failed to obtain RECORD_AUDIO permission (status: %d) for "
                        + "receiving service: %s", status, serviceInfo.name))
        }
        Log_i(String.format(
                "Starting audio streaming. Attributing to %s (%d) with tag '%s'",
                serviceInfo.packageName, serviceInfo.applicationInfo.uid, attributionTag
            )
        )
    }

    /** Tracks that the RECORD_AUDIO operation finished.  */
    private fun finishRecordAudioOp(serviceInfo: ServiceInfo, attributionTag: String?) {
        appOpsManager.finishProxyOp(
            AppOpsManager.permissionToOp(Manifest.permission.RECORD_AUDIO)!!,
            serviceInfo.applicationInfo.uid,
            serviceInfo.packageName,
            attributionTag
        )
    }

    private suspend fun getMusicRecognitionService(
        thread: IApplicationThread,
        token: IBinder?
    ) = recognitionServiceLock.withLock {
        suspendCancellableCoroutineWithTimeout<IMusicRecognitionService>(SERVICE_CONNECT_TIMEOUT) { resume ->
            var hasResumed = false
            musicRecognitionBinder?.let {
                if(hasResumed || !it.pingBinder()) return@let
                resume.resume(IMusicRecognitionService.Stub.asInterface(it))
                hasResumed = true
                return@suspendCancellableCoroutineWithTimeout
            }
            var dispatcher: IServiceConnection? = null
            val serviceConnection = object: ServiceConnection {
                override fun onServiceConnected(component: ComponentName, binder: IBinder) {
                    musicRecognitionBinder = binder
                    musicRecognitionServiceConnection = dispatcher
                    if(!hasResumed) {
                        resume.resume(IMusicRecognitionService.Stub.asInterface(binder))
                    }
                    hasResumed = true
                }

                override fun onServiceDisconnected(component: ComponentName) {
                    musicRecognitionServiceConnection = null
                    musicRecognitionBinder = null
                }
            }
            dispatcher = context.getServiceDispatcher(serviceConnection, 0)
            activityManager.bindServiceInstanceCompat(
                context,
                dispatcher,
                thread,
                token,
                musicRecognitionIntent,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    /** Establishes an audio stream from the DSP audio source. */
    @SuppressLint("MissingPermission")
    private fun createAudioRecord(
        recognitionRequest: RecognitionRequest,
        maxAudioLengthSeconds: Int
    ): AudioRecord {
        val sampleRate: Int = recognitionRequest.audioFormat.sampleRate
        val bufferSize: Int = getBufferSizeInBytes(sampleRate, maxAudioLengthSeconds)
        val shellContext = ShellContext(context, true)
        //We need to replace the attributes as we can't access the regular mic
        val attributes = AudioAttributes.Builder().apply {
            AudioAttributes.Builder::class.java.getMethod("setInternalCapturePreset", Integer.TYPE)
                .invoke(this, 0x7CF)
        }.build()
        return AudioRecord::class.java.getDeclaredConstructor(
            AudioAttributes::class.java, //attributes
            AudioFormat::class.java, // format
            Integer.TYPE, // bufferSizeInBytes
            Integer.TYPE, // sessionId
            Context::class.java, // context
            Integer.TYPE // maxSharedAudioHistoryMs
        ).apply {
            isAccessible = true
        }.newInstance(
            attributes,
            recognitionRequest.audioFormat,
            bufferSize,
            recognitionRequest.captureSession,
            shellContext,
            0
        )
    }

    /**
     * Returns the number of bytes required to store `bufferLengthSeconds` of audio sampled at
     * `sampleRate` Hz, using the format returned by DSP audio capture.
     */
    private fun getBufferSizeInBytes(sampleRate: Int, bufferLengthSeconds: Int): Int {
        return BYTES_PER_SAMPLE * sampleRate * bufferLengthSeconds
    }

    private fun destroyService() {
        musicRecognitionServiceConnection?.let { activityManager.unbindService(it) }
        musicRecognitionBinder = null
    }

    /** Removes remote objects from the bundle.  */
    private fun sanitizeBundle(@Nullable bundle: Bundle?) {
        if (bundle == null) {
            return
        }
        for (key in bundle.keySet()) {
            val o = bundle[key]
            if (o is Bundle) {
                sanitizeBundle(o as Bundle?)
            } else if (o is IBinder || o is ParcelFileDescriptor) {
                bundle.remove(key)
            }
        }
    }

    private fun getServiceInfo(): ServiceInfo? {
        return try {
            context.packageManager.resolveService(musicRecognitionIntent, 0)?.serviceInfo
        }catch (e: PackageManager.NameNotFoundException){
            null
        }
    }

    /**
     * Callback invoked by [android.service.musicrecognition.MusicRecognitionService] to pass
     * back the music search result.
     */
    inner class MusicRecognitionServiceCallback(private val clientCallback: IMusicRecognitionManagerCallback) :
        IMusicRecognitionServiceCallback.Stub() {

        override fun onRecognitionSucceeded(result: MediaMetadata, extras: Bundle) {
            try {
                sanitizeBundle(extras)
                clientCallback.onRecognitionSucceeded(result, extras)
            } catch (ignored: RemoteException) {
                // Ignored.
            }
            destroyService()
        }

        override fun onRecognitionFailed(@RecognitionFailureCode failureCode: Int) {
            try {
                clientCallback.onRecognitionFailed(failureCode)
            } catch (ignored: RemoteException) {
                // Ignored.
            }
            destroyService()
        }

    }

    private fun Log_i(text: String) {
        if(!DEBUG) return
        Log.i(TAG, text)
    }

    private fun Log_e(text: String) {
        if(!DEBUG) return
        Log.e(TAG, text)
    }

    private fun Log_e(text: String, exception: Exception) {
        if(!DEBUG) return
        Log.e(TAG, text, exception)
    }

}