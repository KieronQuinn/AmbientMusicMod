package com.kieronquinn.app.ambientmusicmod.ui.screens.recognition

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.media.musicrecognition.MusicRecognitionManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.button.MaterialButton
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.components.blur.BlurProvider
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentRecognitionBinding
import com.kieronquinn.app.ambientmusicmod.model.recognition.Player
import com.kieronquinn.app.ambientmusicmod.repositories.RecognitionRepository.RecognitionState.ErrorReason
import com.kieronquinn.app.ambientmusicmod.ui.activities.RecognitionModalActivity
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundDialogFragment
import com.kieronquinn.app.ambientmusicmod.ui.screens.recognition.RecognitionViewModel.*
import com.kieronquinn.app.ambientmusicmod.utils.extensions.*
import com.kieronquinn.app.pixelambientmusic.model.RecognitionFailureReason
import com.kieronquinn.app.pixelambientmusic.model.RecognitionSource
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class RecognitionFragment: BoundDialogFragment<FragmentRecognitionBinding>(FragmentRecognitionBinding::inflate) {

    companion object {
        private const val RECOGNITION_TIME = 8_000L
    }

    private val viewModel by viewModel<RecognitionViewModel>()
    private val blurProvider by inject<BlurProvider>()
    private val args by navArgs<RecognitionFragmentArgs>()
    private val isModal by lazy {
        requireActivity() is RecognitionModalActivity
    }

    private val saveLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) {
        if(it == null) return@registerForActivityResult
        viewModel.onPlaybackSaveLocationPicked(requireContext(), it)
    }

    private val fabMargin by lazy {
        resources.getDimension(R.dimen.bottom_nav_height_margins).toInt()
    }

    private val chipsAdapter by lazy {
        RecognitionChipsAdapter(
            emptyList(),
            viewModel::onChipClicked,
            binding.recognitionSuccess.recognitionSuccessChips
        )
    }

    private val motionLayout
        get() = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setDialogVisible(true)
        setupFakeFab()
        setupBackground()
        setupState()
        setupScrim()
        setupRecording()
        setupLoading()
        setupRecognitionIcon()
        setupFailed()
        setupSuccess()
        setupPlayback()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), theme) {
            override fun onBackPressed() {
                close()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if(!isModal){
            requireDialog().window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, false)
                it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
        }
    }

    private fun setupFakeFab() = with(binding.fakeFabBarrier){
        onApplyInsets { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.updateLayoutParams<ConstraintLayout.LayoutParams> {
                updateMargins(bottom = bottomInset + fabMargin)
            }
        }
        binding.fakeFab.backgroundTintList = ColorStateList.valueOf(monet.getPrimaryColor(context))
        updateLayoutParams<ConstraintLayout.LayoutParams> {
            updateMargins(bottom = fabMargin)
        }
    }

    private fun setupRecording() = with(binding.recognitionRecording) {
        val accent = monet.getAccentColor(root.context)
        recordingLottie.filterColour("*", "**", filter = accent)
        recordingProgress.setIndicatorColor(accent)
    }

    private fun setupLoading() = with(binding.recognitionLoading) {
        val accent = monet.getAccentColor(root.context)
        recognisingLoading.setIndicatorColor(accent)
    }

    private fun setupBackground() = with(binding.background){
        val background = monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
        backgroundTintList = ColorStateList.valueOf(background)
        binding.recognitionCircleBackground.root.backgroundTintList =
            ColorStateList.valueOf(background)
    }

    private fun setupScrim() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.root.progressCallback().collect {
            if(it.first == R.id.fab){
                setBackgroundScrimAlpha(it.third)
            }
            if(it.second == R.id.fab) {
                setBackgroundScrimAlpha(1f - it.third)
            }
        }
    }

    private fun setupRecognitionIcon() = with(binding.recognitionCircle.root){
        val accent = monet.getAccentColor(context)
        imageTintList = ColorStateList.valueOf(accent)
    }

    private fun setupFailed() = with(binding.recognitionFailed) {
        val accent = monet.getAccentColor(root.context)
        recognitionRetry.applyMonet()
        recognitionRetry.overrideRippleColor(accent)
        recognitionFailedPlayback.applyMonet()
        recognitionFailedPlayback.overrideRippleColor(accent)
    }

    private fun setupSuccess() = with(binding.recognitionSuccess) {
        val accent = monet.getAccentColor(root.context)
        recognitionSuccessPlayback.applyMonet()
        recognitionSuccessPlayback.overrideRippleColor(accent)
        recognitionSuccessChips.adapter = this@RecognitionFragment.chipsAdapter
        recognitionSuccessChips.layoutManager = FlexboxLayoutManager(context).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.CENTER
        }
    }

    private fun setupPlayback() = with(binding.recognitionPlayback) {
        val accent = monet.getAccentColor(root.context)
        recognitionPlaybackPlay.applyMonet()
        recognitionPlaybackPlay.overrideRippleColor(accent)
        recognitionPlaybackSave.applyMonet()
        recognitionPlaybackSave.overrideRippleColor(accent)
        recognitionPlaybackWaveform.waveColor = accent
    }

    override fun onDestroyView() {
        viewModel.setDialogVisible(false)
        setBackgroundScrimAlpha(0f)
        super.onDestroyView()
    }

    private fun setBackgroundScrimAlpha(alpha: Float) {
        val dialogWindow = dialog?.window ?: return
        val appWindow = activity?.window ?: return
        blurProvider.applyDialogBlur(dialogWindow, appWindow, alpha)
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            jumpToState(viewModel.state.value)
        }
    }

    private fun jumpToState(state: State?) = with(binding.root) {
        if(state == null || state == State.Fab) return@with
        if(state == viewModel.state.value) return@with
        setup()
        setBackgroundScrimAlpha(1f)
        jumpToState(state.stateId) //Don't change state
    }

    private fun MotionLayout.setup() {
        blockTouches()
    }

    private fun setupState() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        viewModel.state.collectLatest {
            when (it) {
                is State.Fab -> {
                    motionLayout.setup()
                    if(!isModal) {
                        motionLayout.jumpToState(R.id.fab)
                        motionLayout.awaitPost()
                        binding.fakeFab.alpha = 1f
                        viewModel.onStateChanged(State.Initial(R.id.fab_to_initial, false))
                    }else{
                        motionLayout.jumpToState(R.id.initial)
                        motionLayout.awaitPost()
                        setupInitial()
                    }
                }
                is State.StateTransition -> {
                    motionLayout.awaitPost()
                    motionLayout.runTransition(it.transitionId)
                }
                else -> {} //No-op
            }
            handleState(it)
        }
    }

    private suspend fun handleState(state: State?) {
        when(state) {
            null -> {
                dismiss()
            }
            is State.Fab -> {} //No-op
            is State.Initial -> {
                if(!state.isClosing) {
                    setupInitial()
                }
            }
            is State.SourcePicker -> {
                setupSourcePicker()
            }
            is State.StartRecognising -> {
                runStartRecognising(state)
            }
            is State.Recording -> {
                runRecording(state)
            }
            is State.Recognising -> {
                //No-op, handled by the ViewModel
            }
            is State.RecognisingIcon -> {
                runRecognisingIcon(state)
            }
            is State.Success -> {
                runSuccess(state)
            }
            is State.Failed -> {
                runFailed(state)
            }
            is State.Error -> {
                runError(state)
            }
            is State.Playback -> {
                runPlayback(state)
            }
            else -> {} //No-op
        }
    }

    private suspend fun setupInitial() {
        when(getStartState()){
            StartState.SOURCE_PICKER -> {
                viewModel.onStateChanged(State.SourcePicker(R.id.initial_to_source_picker))
            }
            StartState.RECOGNISE_NNFP -> {
                viewModel.onStateChanged(
                    State.StartRecognising(R.id.initial_to_loading, RecognitionSource.NNFP)
                )
            }
        }
    }

    private suspend fun getStartState(): StartState {
        return if(isModal){
            if(viewModel.isOnDemandAvailable()){
                StartState.SOURCE_PICKER
            }else{
                StartState.RECOGNISE_NNFP
            }
        }else args.startState
    }

    private fun setupSourcePicker() = with(binding.recognitionSourcePicker) {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            recognitionSourcePickerNnfp.onClicked().collect {
                if(viewModel.state.value !is State.SourcePicker) return@collect
                viewModel.onStateChanged(State.StartRecognising(
                    R.id.source_picker_to_loading,
                    RecognitionSource.NNFP
                ))
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            recognitionSourcePickerOnDemand.onClicked().collect {
                if(viewModel.state.value !is State.SourcePicker) return@collect
                viewModel.onStateChanged(State.StartRecognising(
                    R.id.source_picker_to_loading,
                    RecognitionSource.ON_DEMAND
                ))
            }
        }
    }

    private fun runStartRecognising(state: State.StartRecognising){
        //Prep the progress to start at 0
        binding.recognitionRecording.recordingProgress.progress = 0
        //Prep the lottie animation
        binding.recognitionRecording.recordingLottie.run {
            pauseAnimation()
            frame = 0
        }
        viewModel.runRecognition(state.source)
    }

    private suspend fun runRecording(state: State.Recording) = with(binding.recognitionRecording) {
        recordingLottie.run {
            playAnimation()
        }
        //Prep the icon screen to not have an icon
        binding.recognitionCircle.root.setImageDrawable(null)
        if(state.source == RecognitionSource.NNFP) {
            recordingProgress.isVisible = true
            val startTime = state.startTime
            val currentTime = System.currentTimeMillis() - startTime
            val currentProgress = (currentTime / RECOGNITION_TIME.toFloat() * 100).toInt()
            val remainingTime = RECOGNITION_TIME - currentTime
            recordingProgress.animateProgress(currentProgress, remainingTime).runAndJoin()
        }else{
            //OnDemand doesn't have a pre-determined time
            recordingProgress.isVisible = false
        }
    }

    private suspend fun runRecognisingIcon(state: State.RecognisingIcon) = with(binding.recognitionCircle.root) {
        val resource = if(state.result is RecogniseResult.Success){
            R.drawable.audioanim_animation
        }else{
            R.drawable.audioanim_no_music
        }
        when(state.result){
            is RecogniseResult.Success -> {
                val players = Player.getPlayers(
                    requireContext(),
                    state.result.recognitionResult.players,
                    state.result.recognitionResult.googleId
                )
                prepSuccess(state.result, players)
            }
            is RecogniseResult.Failed -> {
                prepFailed(state.result)
            }
            is RecogniseResult.Error -> {
                prepError(state.result)
            }
        }
        val drawable = ContextCompat.getDrawable(
            requireContext(), resource
        ) as AnimatedVectorDrawable
        setImageDrawable(drawable)
        drawable.start()
        delay(750L)
        val newState = when(state.result){
            is RecogniseResult.Success -> {
                val players = Player.getPlayers(
                    requireContext(),
                    state.result.recognitionResult.players,
                    state.result.recognitionResult.googleId
                )
                State.Success(R.id.recognising_icon_to_success, state.result, players)
            }
            is RecogniseResult.Failed -> {
                State.Failed(R.id.recognising_icon_to_failed, state.result)
            }
            is RecogniseResult.Error -> {
                State.Error(R.id.recognising_icon_to_failed, state.result)
            }
        }
        viewModel.onStateChanged(newState)
    }

    private fun prepSuccess(
        result: RecogniseResult.Success,
        players: List<Player>?
    ) = with(binding.recognitionSuccess) {
        val track = result.recognitionResult
        recognitionSuccessSong.text =
            getString(R.string.recognition_success_content, track.trackName, track.artist)
        chipsAdapter.items = players ?: emptyList()
        chipsAdapter.notifyDataSetChanged()
        recognitionSuccessPlayback.isVisible = result.recognitionResult.audio != null
    }

    private fun prepFailed(result: RecogniseResult.Failed) = with(binding.recognitionFailed) {
        val reasonTitle = when(result.failure.failureReason){
            is RecognitionFailureReason.NoMatch -> R.string.recognition_failed_reason_no_match
            is RecognitionFailureReason.SkippedOnCall -> R.string.recognition_failed_reason_skipped
            is RecognitionFailureReason.SkippedSystemUserNotInForeground -> R.string.recognition_failed_reason_skipped
            is RecognitionFailureReason.SkippedMusicPlaying -> R.string.recognition_failed_reason_skipped
            is RecognitionFailureReason.SkippedAudioRecordFailed -> R.string.recognition_failed_reason_skipped
            is RecognitionFailureReason.Busy -> R.string.recognition_failed_reason_skipped
            is RecognitionFailureReason.MusicRecognitionError -> R.string.recognition_failed_reason_generic
        }
        recognitionFailedTitle.setText(reasonTitle)
        val reasonContent = when(result.failure.failureReason){
            is RecognitionFailureReason.NoMatch -> R.string.recognition_failed_reason_content_no_match
            is RecognitionFailureReason.SkippedOnCall -> R.string.recognition_failed_reason_content_on_call
            is RecognitionFailureReason.SkippedSystemUserNotInForeground -> R.string.recognition_failed_reason_content_system_user
            is RecognitionFailureReason.SkippedMusicPlaying -> R.string.recognition_failed_reason_content_music_playing
            is RecognitionFailureReason.MusicRecognitionError -> result.failure.failureReason.getContent()
            is RecognitionFailureReason.SkippedAudioRecordFailed -> R.string.recognition_failed_reason_content_audio_record_failed
            is RecognitionFailureReason.Busy -> R.string.recognition_failed_reason_content_busy
        }
        recognitionFailedContent.setText(reasonContent)
        recognitionFailedPlayback.isVisible = result.failure.audio != null
    }

    private fun prepError(result: RecogniseResult.Error) = with(binding.recognitionFailed) {
        recognitionFailedTitle.setText(R.string.recognition_error_title)
        val reasonContent = when(result.type){
            ErrorReason.SHIZUKU_ERROR -> R.string.recognition_error_content_shizuku
            ErrorReason.TIMEOUT -> R.string.recognition_error_content_timeout
            ErrorReason.API_INCOMPATIBLE -> R.string.recognition_error_api_version
            ErrorReason.NEEDS_ROOT -> R.string.recognition_error_needs_root
            ErrorReason.DISABLED -> R.string.recognition_error_disabled
        }
        recognitionFailedContent.setText(reasonContent)
    }

    @StringRes
    private fun RecognitionFailureReason.MusicRecognitionError.getContent(): Int {
        return when(errorCode){
            MusicRecognitionManager.RECOGNITION_FAILED_NOT_FOUND -> {
                R.string.recognition_failed_reason_content_code_not_found
            }
            MusicRecognitionManager.RECOGNITION_FAILED_NO_CONNECTIVITY -> {
                R.string.recognition_failed_reason_content_code_no_connectivity
            }
            MusicRecognitionManager.RECOGNITION_FAILED_SERVICE_KILLED,
            MusicRecognitionManager.RECOGNITION_FAILED_SERVICE_UNAVAILABLE -> {
                R.string.recognition_failed_reason_content_code_service_unavailable
            }
            MusicRecognitionManager.RECOGNITION_FAILED_AUDIO_UNAVAILABLE -> {
                R.string.recognition_failed_reason_content_code_audio_unavailable
            }
            MusicRecognitionManager.RECOGNITION_FAILED_TIMEOUT -> {
                R.string.recognition_failed_reason_content_code_timeout
            }
            MusicRecognitionManager_RECOGNITION_FAILED_NEEDS_ROOT -> {
                R.string.recognition_failed_reason_content_root
            }
            else -> {
                R.string.recognition_failed_reason_content_code_unknown
            }
        }
    }

    private fun runSuccess(state: State.Success) = with(binding.recognitionSuccess) {
        root.bringToFront()
        binding.recognitionCircle.root.setImageResource(R.drawable.ic_recognition_circle_success)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            recognitionSuccessPlayback.onClicked().collect {
                if(viewModel.state.value !is State.Success) return@collect
                val audio = state.result.recognitionResult.audio ?: return@collect
                viewModel.onStateChanged(State.Playback(R.id.success_to_playback, audio))
            }
        }
        prepSuccess(state.result, state.players)
    }

    private fun runFailed(state: State.Failed) = with(binding.recognitionFailed) {
        root.bringToFront()
        binding.recognitionCircle.root.setImageResource(R.drawable.ic_recognition_circle_failed)
        prepFailed(state.result)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            recognitionRetry.onClicked().collect {
                if(viewModel.state.value !is State.Failed) return@collect
                viewModel.onStateChanged(State.StartRecognising(
                    R.id.failed_to_loading, state.result.failure.source
                ))
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            recognitionFailedPlayback.onClicked().collect {
                if(viewModel.state.value !is State.Failed) return@collect
                val audio = state.result.failure.audio ?: return@collect
                viewModel.onStateChanged(State.Playback(R.id.failed_to_playback, audio))
            }
        }
    }

    private fun runError(state: State.Error) = with(binding.recognitionFailed) {
        root.bringToFront()
        binding.recognitionCircle.root.setImageResource(R.drawable.ic_recognition_circle_failed)
        prepError(state.result)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            recognitionRetry.onClicked().collect {
                if(viewModel.state.value !is State.Error) return@collect
                viewModel.onStateChanged(State.StartRecognising(
                    R.id.failed_to_loading, state.result.source
                ))
            }
        }
    }

    private fun runPlayback(state: State.Playback) = with(binding.recognitionPlayback) {
        root.bringToFront()
        //Apply a gain just to the UI to make the waveform useful
        recognitionPlaybackWaveform.setRawData(state.audio.clone().applyGain(4f).toByteArray())
        recognitionPlaybackPlay.updatePlayingState(
            viewModel.playbackState.value is PlaybackState.Playing
        )
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            recognitionPlaybackPlay.onClicked().collect {
                if(viewModel.state.value !is State.Playback) return@collect
                viewModel.onPlaybackPlayStopClicked()
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            recognitionPlaybackSave.onClicked().collect {
                if(viewModel.state.value !is State.Playback) return@collect
                viewModel.onPlaybackSaveClicked(saveLauncher)
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.playbackState.collect {
                if (it is PlaybackState.Playing) {
                    recognitionPlaybackWaveform.progress = it.progress
                    recognitionPlaybackPlay.updatePlayingState(true)
                }else{
                    recognitionPlaybackWaveform.progress = 0f
                    recognitionPlaybackPlay.updatePlayingState(false)
                }
            }
        }
    }

    private var isButtonPlaying = false
    private fun MaterialButton.updatePlayingState(isPlaying: Boolean) {
        if(isPlaying == isButtonPlaying) return
        isButtonPlaying = isPlaying
        val icon = if(isPlaying){
            ContextCompat.getDrawable(context, R.drawable.avd_play_to_stop)
        }else{
            ContextCompat.getDrawable(context, R.drawable.avd_stop_to_play)
        }
        val text = if(isPlaying){
            R.string.recognition_playback_stop
        }else{
            R.string.recognition_playback_play
        }
        setText(text)
        setIcon(icon)
        (icon as AnimatedVectorDrawable).start()
    }

    private fun close() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        val state = viewModel.state.value
        if(state !is State.StateTransition || state.backToInitialTransitionId == 0){
            viewModel.onStateChanged(null)
            return@launchWhenResumed
        }
        viewModel.onStateChanged(State.Initial(state.backToInitialTransitionId, true))
        binding.root.onComplete().await { it == R.id.initial }
        viewModel.onStateChanged(State.FabClosing(R.id.initial_to_fab))
        binding.root.onComplete().await { it == R.id.fab }
        viewModel.onStateChanged(null)
    }

}