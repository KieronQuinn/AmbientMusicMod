package com.kieronquinn.app.ambientmusicmod.app.ui.settings.manualtrigger.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.NavigationEvent
import com.kieronquinn.app.ambientmusicmod.components.settings.BaseViewModel
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getAudioFormat
import com.kieronquinn.app.ambientmusicmod.utils.extensions.getTempInputFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.IllegalStateException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

abstract class SettingsManualTriggerPlaybackBottomSheetViewModel: BaseViewModel() {

    abstract val state: Flow<State>
    abstract val loadState: Flow<LoadState>
    abstract val progress: Flow<Float>
    abstract val shouldShowOverflow: Flow<Boolean>

    abstract fun playStop()
    abstract fun release()
    abstract fun onSaveToFileClicked(activityResultLauncher: ActivityResultLauncher<String>)
    abstract fun onSaveToFileDocumentPicked(uri: Uri)
    abstract fun onFileInfoClicked()
    abstract fun getDeveloperModeState()

    sealed class State {
        object Stopped: State()
        object Playing: State()
    }

    sealed class LoadState {
        object Loading: LoadState()
        data class Loaded(val audioTrack: AudioTrack, val rawBytes: ByteArray): LoadState() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Loaded

                if (audioTrack != other.audioTrack) return false
                if (!rawBytes.contentEquals(other.rawBytes)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = audioTrack.hashCode()
                result = 31 * result + rawBytes.contentHashCode()
                return result
            }
        }
    }

}

class SettingsManualTriggerPlaybackBottomSheetViewModelImpl(private val context: Context): SettingsManualTriggerPlaybackBottomSheetViewModel(),
    AudioTrack.OnPlaybackPositionUpdateListener {

    private val audioFile = context.getTempInputFile()

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val _state = MutableStateFlow<State>(State.Stopped)
    override val state: Flow<State> = _state.asStateFlow()

    private val _progress = MutableSharedFlow<Float>()
    override val progress: Flow<Float> = _progress.asSharedFlow()

    private var localProgress: Int = 0

    private val _loadState = MutableStateFlow<LoadState>(LoadState.Loading).apply {
        viewModelScope.launch {
            collect {
                if(it is LoadState.Loading){
                    //Load audio file
                    val audioTrack = withContext(Dispatchers.IO){
                        loadAudioFile()
                    }
                    emit(LoadState.Loaded(audioTrack.second, audioTrack.first))
                }
            }
        }
    }

    override val loadState: Flow<LoadState> = _loadState.asStateFlow()

    private val developerModeEnabled = MutableSharedFlow<Boolean>()
    override val shouldShowOverflow: Flow<Boolean> = combine(loadState, developerModeEnabled){ ls: LoadState, enabled: Boolean ->
        ls is LoadState.Loaded && enabled
    }

    override fun playStop() {
        if(_state.value == State.Stopped){
            start()
        }else{
            stop()
        }
    }

    private fun start() = viewModelScope.launch {
        if(_loadState.value !is LoadState.Loaded) return@launch
        localProgress = 0
        _progress.emit(0f)
        (_loadState.value as LoadState.Loaded).audioTrack.run {
            reloadStaticData()
            play()
        }
        _state.emit(State.Playing)
    }

    private fun stop() = viewModelScope.launch {
        if(_loadState.value !is LoadState.Loaded) return@launch
        _state.emit(State.Stopped)
        localProgress = 0
        _progress.emit(0f)
        (_loadState.value as LoadState.Loaded).audioTrack.run {
            stop()
            flush()
        }
    }

    private fun loadAudioFile(): Pair<ByteArray, AudioTrack> {
        val audioSessionId = audioManager.generateAudioSessionId()
        val audioBytes = audioFile.readBytes()
        val audioTrack = AudioTrack.Builder().apply {
            setAudioAttributes(AudioAttributes.Builder().apply {
                setUsage(AudioAttributes.USAGE_MEDIA)
                setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            }.build())
            setTransferMode(AudioTrack.MODE_STATIC)
            setAudioFormat(getAudioFormat(true))
            setSessionId(audioSessionId)
            setBufferSizeInBytes(audioBytes.size)
        }.build().also {
            it.positionNotificationPeriod = (it.bufferSizeInFrames / 100f).roundToInt()
            it.notificationMarkerPosition = it.bufferSizeInFrames
            it.write(audioBytes, 0, audioBytes.size)
            it.setPlaybackPositionUpdateListener(this)
        }
        return Pair(audioBytes, audioTrack)
    }

    override fun onMarkerReached(track: AudioTrack?) {
        viewModelScope.launch {
            track?.stop()
            track?.flush()
            _state.emit(State.Stopped)
            localProgress = 0
            _progress.emit(0f)
        }
    }

    override fun onPeriodicNotification(track: AudioTrack?) {
        viewModelScope.launch {
            if(_state.value !is State.Playing) return@launch
            localProgress++
            _progress.emit(localProgress.toFloat())
        }
    }

    override fun release() {
        viewModelScope.launch {
            if(_loadState.value !is LoadState.Loaded) return@launch
            (_loadState.value as LoadState.Loaded).audioTrack.run {
                try {
                    stop()
                    flush()
                    release()
                }catch (e: IllegalStateException){
                    //Audio Track uninitialised
                }
            }
        }
    }

    override fun onSaveToFileClicked(activityResultLauncher: ActivityResultLauncher<String>) {
        viewModelScope.launch {
            if(_loadState.value !is LoadState.Loaded) return@launch
            val suggestedName = "ambient_music_input_${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())}.pcm"
            activityResultLauncher.launch(suggestedName)
        }
    }

    override fun getDeveloperModeState() {
        viewModelScope.launch {
            developerModeEnabled.emit(settings.developerModeEnabled)
        }
    }

    override fun onFileInfoClicked() {
        viewModelScope.launch {
            navigation.navigate(NavigationEvent.NavigateByDirections(SettingsManualTriggerPlaybackBottomSheetFragmentDirections.actionSettingsManualTriggerPlaybackBottomSheetFragmentToSettingsManualTriggerPlaybackFileInfoBottomSheetFragment()))
        }
    }

    override fun onSaveToFileDocumentPicked(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO){
                val audioBytes = (_loadState.value as? LoadState.Loaded)?.rawBytes ?: return@withContext false
                context.contentResolver.openOutputStream(uri)?.run {
                    write(audioBytes)
                    close()
                }
                true
            }
            if(result) Toast.makeText(context, context.getString(R.string.bs_manual_trigger_playback_write_to_file_saved), Toast.LENGTH_LONG).show()
        }

    }

}