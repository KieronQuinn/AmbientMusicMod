package com.kieronquinn.app.ambientmusicmod.repositories

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.audio.ambientmusic.HistoryData
import com.kieronquinn.app.ambientmusicmod.repositories.RecognitionRepository.RecognitionState
import com.kieronquinn.app.ambientmusicmod.repositories.RecognitionRepository.RecognitionState.ErrorReason
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository.SettingsState
import com.kieronquinn.app.ambientmusicmod.utils.extensions.safeQuery
import com.kieronquinn.app.ambientmusicmod.utils.extensions.safeRegisterContentObserver
import com.kieronquinn.app.pixelambientmusic.IRecognitionCallback
import com.kieronquinn.app.pixelambientmusic.IRecognitionService
import com.kieronquinn.app.pixelambientmusic.model.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface RecognitionRepository {

    sealed class RecognitionState {
        data class Recording(val source: RecognitionSource): RecognitionState()
        data class Recognising(val source: RecognitionSource): RecognitionState()
        data class Recognised(
            val recognitionResult: RecognitionResult,
            val metadata: RecognitionMetadata?
        ): RecognitionState()
        data class Error(val errorReason: ErrorReason): RecognitionState()
        data class Failed(val recognitionFailure: RecognitionFailure): RecognitionState()

        enum class ErrorReason {
            SHIZUKU_ERROR, TIMEOUT, API_INCOMPATIBLE, NEEDS_ROOT, DISABLED
        }
    }

    val recognitionDialogShowing: Flow<Boolean>
    val recogniseFabClick: Flow<Unit>

    fun requestRecognition(includeAudio: Boolean = false): Flow<RecognitionState>
    fun requestOnDemandRecognition(): Flow<RecognitionState>
    fun getLatestRecognition(): Flow<LastRecognisedSong?>

    suspend fun onRecogniseFabClicked()
    suspend fun setRecognitionDialogShowing(showing: Boolean)

}

class RecognitionRepositoryImpl(
    private val ambientServiceRepository: AmbientServiceRepository,
    private val shizukuServiceRepository: ShizukuServiceRepository,
    context: Context
): RecognitionRepository, KoinComponent {

    companion object {
        private const val RECOGNITION_CALLBACK_TIMEOUT = 2500L
        private val URI_HISTORY = Uri.Builder()
            .scheme("content")
            .authority("com.google.android.as.pam.ambientmusic.historyprovider")
            .path("recognizedsongs")
            .build()
        private const val COLUMN_HISTORY_TIMESTAMP = "timestamp"
        private const val COLUMN_HISTORY_HISTORY_ENTRY = "history_entry"
    }

    private val contentResolver = context.contentResolver

    private suspend fun getService() = ambientServiceRepository.getService()

    private val remoteSettings by inject<RemoteSettingsRepository>()

    private fun runRecognition(
        source: RecognitionSource,
        includeAudio: Boolean,
        requestBlock: (IRecognitionService) -> Unit
    ) = callbackFlow {
        if(!shizukuServiceRepository.assertReady()) {
            trySend(RecognitionState.Error(ErrorReason.SHIZUKU_ERROR))
            close()
            return@callbackFlow
        }
        shizukuServiceRepository.runWithService { it.isCompatible }.unwrap()?.let {
            if(!it){
                trySend(RecognitionState.Error(ErrorReason.NEEDS_ROOT))
                close()
                return@callbackFlow
            }
        }
        val settings = remoteSettings.getRemoteSettings().first()
        if(settings !is SettingsState.Available || !settings.mainEnabled){
            trySend(RecognitionState.Error(ErrorReason.DISABLED))
            close()
        }
        var hasStarted = false
        async {
            delay(RECOGNITION_CALLBACK_TIMEOUT)
            if(!hasStarted){
                trySend(RecognitionState.Error(ErrorReason.TIMEOUT))
                close()
            }
        }
        val callback = object: IRecognitionCallback.Stub() {
            override fun onRecordingStarted() {
                hasStarted = true
                trySend(RecognitionState.Recording(source))
            }

            override fun onRecognitionStarted() {
                hasStarted = true
                trySend(RecognitionState.Recognising(source))
            }

            override fun onRecognitionSucceeded(
                result: RecognitionResult,
                metadata: RecognitionMetadata?
            ) {
                hasStarted = true
                trySend(RecognitionState.Recognised(result, metadata))
                close()
            }

            override fun onRecognitionFailed(result: RecognitionFailure) {
                hasStarted = true
                trySend(RecognitionState.Failed(result))
                close()
            }
        }
        val metadata = RecognitionCallbackMetadata(source, includeAudio)
        val service = getService() ?: run {
            hasStarted = true
            trySend(RecognitionState.Error(ErrorReason.API_INCOMPATIBLE))
            close()
            return@callbackFlow
        }
        val callbackId = service.addRecognitionCallback(callback, metadata)
        requestBlock(service)
        awaitClose {
            callbackId?.let {
                //We need to disconnect regardless, even if the flow scope has gone
                GlobalScope.launch {
                    getService()?.removeRecognitionCallback(it)
                }
            }
        }
    }

    override val recogniseFabClick = MutableSharedFlow<Unit>()
    override val recognitionDialogShowing = MutableSharedFlow<Boolean>()

    override suspend fun onRecogniseFabClicked() {
        recogniseFabClick.emit(Unit)
    }

    override suspend fun setRecognitionDialogShowing(showing: Boolean) {
        recognitionDialogShowing.emit(showing)
    }

    override fun requestRecognition(includeAudio: Boolean) = runRecognition(
        RecognitionSource.NNFP, includeAudio
    ) {
        it.requestRecognition()
    }

    override fun requestOnDemandRecognition() = runRecognition(
        RecognitionSource.ON_DEMAND, false //ON_DEMAND does not include audio
    ) {
        it.requestOnDemandRecognition()
    }

    private fun loadLatestRecognition(): LastRecognisedSong? {
        val cursor = contentResolver.safeQuery(
            URI_HISTORY,
            arrayOf(COLUMN_HISTORY_TIMESTAMP, COLUMN_HISTORY_HISTORY_ENTRY),
            null,
            null,
            "$COLUMN_HISTORY_TIMESTAMP DESC"
        )
        if(cursor == null || cursor.count == 0 || cursor.isAfterLast) return null
        cursor.moveToFirst()
        val timestamp = cursor.getLong(0)
        val historyEntry = cursor.getBlob(1)
        if(timestamp == 0L || historyEntry == null) return null
        val entry = HistoryData.Item.parseFrom(historyEntry)
        return LastRecognisedSong(
            entry.track.title,
            entry.track.artist,
            timestamp,
            if(entry.source == "ON_DEMAND") RecognitionSource.ON_DEMAND else RecognitionSource.NNFP
        )
    }

    override fun getLatestRecognition(): Flow<LastRecognisedSong?> = callbackFlow {
        val observer = object: ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(loadLatestRecognition())
            }
        }
        contentResolver.safeRegisterContentObserver(
            URI_HISTORY,
            true,
            observer
        )
        trySend(loadLatestRecognition())
        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }

}