package com.kieronquinn.app.ambientmusicmod.ui.screens.recognition

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.navigation.RootNavigation
import com.kieronquinn.app.ambientmusicmod.model.recognition.Player
import com.kieronquinn.app.ambientmusicmod.repositories.RecognitionRepository
import com.kieronquinn.app.ambientmusicmod.repositories.RecognitionRepository.RecognitionState
import com.kieronquinn.app.ambientmusicmod.repositories.RecognitionRepository.RecognitionState.ErrorReason
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.WidgetRepository
import com.kieronquinn.app.ambientmusicmod.service.AmbientMusicModForegroundService
import com.kieronquinn.app.ambientmusicmod.utils.extensions.toByteArray
import com.kieronquinn.app.pixelambientmusic.model.RecognitionFailure
import com.kieronquinn.app.pixelambientmusic.model.RecognitionMetadata
import com.kieronquinn.app.pixelambientmusic.model.RecognitionResult
import com.kieronquinn.app.pixelambientmusic.model.RecognitionSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

abstract class RecognitionViewModel: ViewModel() {

    abstract val state: StateFlow<State?>
    abstract val playbackState: StateFlow<PlaybackState>

    abstract fun onStateChanged(state: State?)
    abstract fun setDialogVisible(visible: Boolean)

    abstract fun runRecognition(source: RecognitionSource)
    abstract fun onChipClicked(intent: Intent)

    abstract fun onPlaybackPlayStopClicked()
    abstract fun onPlaybackSaveClicked(launcher: ActivityResultLauncher<String>)
    abstract fun onPlaybackSaveLocationPicked(context: Context, uri: Uri)

    abstract suspend fun isOnDemandAvailable(): Boolean

    sealed class PlaybackState {
        object Stopped: PlaybackState()
        data class Playing(val progress: Float): PlaybackState()
    }

    sealed class RecogniseResult {
        data class Success(
            val recognitionResult: RecognitionResult,
            val metadata: RecognitionMetadata?
        ): RecogniseResult()
        data class Failed(val failure: RecognitionFailure): RecogniseResult()
        data class Error(val type: ErrorReason, val source: RecognitionSource): RecogniseResult()
    }

    sealed class State(
        @IdRes open val stateId: Int,
        @IdRes open val backToInitialTransitionId: Int
    ) {

        data class FabClosing(override val transitionId: Int): StateTransition(
            transitionId, R.id.fab, R.id.fab_to_initial
        )

        object Fab: State(R.id.fab, R.id.fab_to_initial)

        data class Initial(
            override val transitionId: Int,
            val isClosing: Boolean
        ): StateTransition(
            transitionId, R.id.fab, 0
        )

        data class SourcePicker(override val transitionId: Int): StateTransition(
            transitionId, R.id.source_picker, R.id.source_picker_to_initial
        )

        data class StartRecognising(
            override val transitionId: Int,
            val source: RecognitionSource
        ): StateTransition(
            transitionId, R.id.loading, R.id.loading_to_initial
        )

        data class Recording(
            override val transitionId: Int,
            val startTime: Long,
            val source: RecognitionSource
        ): StateTransition(
            transitionId, R.id.recording, R.id.recording_to_initial
        )

        data class Recognising(
            override val transitionId: Int,
            val source: RecognitionSource
        ): StateTransition(
            transitionId, R.id.recognising, R.id.recognising_to_initial
        )

        data class RecognisingIcon(
            override val transitionId: Int,
            val result: RecogniseResult
        ): StateTransition(
            transitionId, R.id.recognising_icon, R.id.recognising_icon_to_initial
        )

        data class Success(
            override val transitionId: Int,
            val result: RecogniseResult.Success,
            val players: List<Player>
        ): StateTransition(
            transitionId, R.id.recognition_success, R.id.success_to_initial
        )

        data class Failed(
            override val transitionId: Int,
            val result: RecogniseResult.Failed
        ): StateTransition(
            transitionId, R.id.recognition_failed, R.id.failed_to_initial
        )

        data class Error(
            override val transitionId: Int,
            val result: RecogniseResult.Error
        ): StateTransition(
            transitionId, R.id.recognition_failed, R.id.failed_to_initial
        )

        data class Playback(
            override val transitionId: Int,
            val audio: ShortArray
        ): StateTransition(
            transitionId, R.id.recognition_playback, R.id.playback_to_initial
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Playback

                if (transitionId != other.transitionId) return false
                if (!audio.contentEquals(other.audio)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = transitionId
                result = 31 * result + audio.contentHashCode()
                return result
            }
        }

        abstract class StateTransition(
            @IdRes open val transitionId: Int,
            @IdRes override val stateId: Int,
            @IdRes override val backToInitialTransitionId: Int
        ): State(stateId, backToInitialTransitionId)
    }

    enum class StartState {
        SOURCE_PICKER, RECOGNISE_NNFP
    }

}

class RecognitionViewModelImpl(
    private val recognitionRepository: RecognitionRepository,
    private val navigation: RootNavigation,
    private val widgetRepository: WidgetRepository,
    private val remoteSettingsRepository: RemoteSettingsRepository,
    context: Context
): RecognitionViewModel(), AudioTrack.OnPlaybackPositionUpdateListener {

    private var audioTrack: AudioTrack? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override val playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Stopped)
    override val state = MutableStateFlow<State?>(State.Fab)

    private var recognitionJob: Job? = null

    override fun onStateChanged(state: State?) {
        viewModelScope.launch {
            this@RecognitionViewModelImpl.state.emit(state)
        }
    }

    override fun setDialogVisible(visible: Boolean) {
        viewModelScope.launch {
            recognitionRepository.setRecognitionDialogShowing(visible)
        }
    }

    override fun runRecognition(source: RecognitionSource) {
        recognitionJob?.cancel()
        recognitionJob = viewModelScope.launch {
            if(source == RecognitionSource.NNFP){
                recognitionRepository.requestRecognition(true).collect {
                    if(handleRecognitionState(it, source)) {
                        cancel()
                    }
                }
            }else{
                recognitionRepository.requestOnDemandRecognition().collect {
                    if(handleRecognitionState(it, source)) {
                        cancel()
                    }
                }
            }
        }
    }

    override fun onChipClicked(intent: Intent) {
        viewModelScope.launch {
            navigation.navigate(intent)
        }
    }

    private suspend fun handleRecognitionState(
        state: RecognitionState,
        source: RecognitionSource
    ): Boolean {
        widgetRepository.notifyRecognitionState(state)
        return when(state){
            is RecognitionState.Recording -> {
                if(this.state.value !is State.StartRecognising) {
                    return this.state.value !is State.Recording //Prevent double recording cancelling
                }
                this.state.emit(
                    State.Recording(R.id.loading_to_recording, System.currentTimeMillis(), source)
                )
                false
            }
            is RecognitionState.Recognising -> {
                if(source == RecognitionSource.NNFP) {
                    if (this.state.value !is State.Recording) {
                        return this.state.value !is State.Recognising //Prevent double recognising cancelling
                    }
                    this.state.emit(State.Recognising(R.id.recording_to_recognising, source))
                }else{
                    if(this.state.value !is State.StartRecognising) return true
                    this.state.emit(
                        State.Recording(R.id.loading_to_recording, System.currentTimeMillis(), source)
                    )
                }
                false
            }
            is RecognitionState.Recognised -> {
                //Send recognition to service
                AmbientMusicModForegroundService.sendManualRecognition(state)
                if(source == RecognitionSource.NNFP) {
                    if (this.state.value !is State.Recognising) return true
                    val result = RecogniseResult.Success(
                        state.recognitionResult,
                        state.metadata
                    )
                    this.state.emit(
                        State.RecognisingIcon(
                            R.id.recognising_to_recognising_icon,
                            result
                        )
                    )
                }else{
                    if(this.state.value !is State.Recording) return true
                    val result = RecogniseResult.Success(state.recognitionResult, state.metadata)
                    this.state.emit(
                        State.RecognisingIcon(R.id.recording_to_recognising_icon, result)
                    )
                }
                true
            }
            is RecognitionState.Failed -> {
                if(source == RecognitionSource.NNFP){
                    if(this.state.value !is State.Recognising) return true
                    val result = RecogniseResult.Failed(state.recognitionFailure)
                    this.state.emit(
                        State.RecognisingIcon(R.id.recognising_to_recognising_icon, result)
                    )
                }else{
                    if(this.state.value !is State.Recording) return true
                    val result = RecogniseResult.Failed(state.recognitionFailure)
                    this.state.emit(
                        State.RecognisingIcon(R.id.recording_to_recognising_icon, result)
                    )
                }
                true
            }
            is RecognitionState.Error -> {
                val result = RecogniseResult.Error(state.errorReason, source)
                val route = when(this.state.value){
                    is State.StartRecognising -> R.id.loading_to_recognising_icon
                    is State.Recording -> R.id.recording_to_recognising_icon
                    is State.Recognising -> R.id.recognising_to_recognising_icon
                    else -> return true
                }
                this.state.emit(
                    State.RecognisingIcon(route, result)
                )
                true
            }
        }
    }

    override fun onPlaybackPlayStopClicked() {
        viewModelScope.launch {
            if(playbackState.value !is PlaybackState.Playing){
                loadAndPlayAudioTrack()
            }else{
                stopAndReleaseAudioTrack()
                playbackState.emit(PlaybackState.Stopped)
            }
        }
    }

    private suspend fun loadAndPlayAudioTrack() {
        val audioData = (state.value as? State.Playback)?.audio ?: return
        val audioSessionId = audioManager.generateAudioSessionId()
        val audioTrack = AudioTrack.Builder().apply {
            setAudioAttributes(AudioAttributes.Builder().apply {
                setUsage(AudioAttributes.USAGE_MEDIA)
                setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            }.build())
            setTransferMode(AudioTrack.MODE_STATIC)
            setAudioFormat(getAudioFormat())
            setSessionId(audioSessionId)
            setBufferSizeInBytes(audioData.size)
        }.build().also {
            it.positionNotificationPeriod = (it.bufferSizeInFrames / 100f).roundToInt()
            it.notificationMarkerPosition = it.bufferSizeInFrames
            it.write(audioData, 0, audioData.size)
            it.setPlaybackPositionUpdateListener(this)
        }
        audioTrack.play()
        this.audioTrack = audioTrack
        playbackState.emit(PlaybackState.Playing(0f))
    }

    private fun getAudioFormat(): AudioFormat {
        return AudioFormat.Builder().apply {
            setSampleRate(16000)
            setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        }.build()
    }

    private fun stopAndReleaseAudioTrack() {
        audioTrack?.let {
            try {
                it.stop()
            }catch (e: IllegalStateException){
                //Already stopped
            }
            it.flush()
            it.release()
            audioTrack = null
        }
    }

    override fun onCleared() {
        //Need to release the track regardless
        stopAndReleaseAudioTrack()
        super.onCleared()
    }

    override fun onPlaybackSaveClicked(launcher: ActivityResultLauncher<String>) {
        val time = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())
        val suggestedName = "ambient_music_input_$time.pcm"
        launcher.launch(suggestedName)
    }

    override fun onPlaybackSaveLocationPicked(context: Context, uri: Uri) {
        viewModelScope.launch {
            val audio = (state.value as? State.Playback)?.audio ?: return@launch
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(audio.toByteArray())
                it.flush()
            }
            Toast.makeText(context, R.string.recognition_playback_saved_toast, Toast.LENGTH_LONG)
                .show()
        }
    }

    override fun onMarkerReached(track: AudioTrack?) {
        track?.stop()
        track?.flush()
        viewModelScope.launch {
            playbackState.emit(PlaybackState.Stopped)
        }
    }

    override fun onPeriodicNotification(track: AudioTrack?) {
        viewModelScope.launch {
            if(playbackState.value !is PlaybackState.Playing) return@launch
            val progress = (playbackState.value as? PlaybackState.Playing)?.progress?.toInt() ?: 0
            playbackState.emit(PlaybackState.Playing((progress + 1).toFloat()))
        }
    }

    override suspend fun isOnDemandAvailable(): Boolean {
        return remoteSettingsRepository.getOnDemandSupportedAndEnabled().first()
    }

}