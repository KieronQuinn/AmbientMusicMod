package com.kieronquinn.app.ambientmusicmod.app.ui.settings.manualtrigger

import android.content.*
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.base.BaseBottomSheetDialogFragment
import com.kieronquinn.app.ambientmusicmod.app.ui.settings.manualtrigger.troubleshooting.SettingsManualTriggerTroubleshootingBottomSheetFragment
import com.kieronquinn.app.ambientmusicmod.databinding.BsManualTriggerBinding
import com.kieronquinn.app.ambientmusicmod.model.recognition.RecognitionResponse
import com.kieronquinn.app.ambientmusicmod.model.recognition.RecognitionResult
import com.kieronquinn.app.ambientmusicmod.model.recognition.toTroubleshootingType
import com.kieronquinn.app.ambientmusicmod.utils.autoCleared
import com.kieronquinn.app.ambientmusicmod.utils.extensions.SecureBroadcastReceiver
import com.kieronquinn.app.ambientmusicmod.utils.extensions.keepScreenOn
import com.kieronquinn.app.ambientmusicmod.xposed.apps.PixelAmbientServices
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.roundToInt

class SettingsManualTriggerBottomSheetFragment: BaseBottomSheetDialogFragment() {

    private val viewModel by viewModel<SettingsManualTriggerBottomSheetViewModel>()
    private var binding by autoCleared<BsManualTriggerBinding>()
    private var dialog by autoCleared<MaterialDialog>()

    private val recognitionStartedIntentFilter = IntentFilter(PixelAmbientServices.INTENT_ACTION_RECOGNITION_STARTED)
    private val recognitionResponseIntentFilter = IntentFilter(PixelAmbientServices.INTENT_ACTION_RECOGNITION_RESULT)

    private val musicAvd by lazy {
        ContextCompat.getDrawable(requireContext(), R.drawable.audioanim_animation) as AnimatedVectorDrawable
    }

    private val startReceiver = SecureBroadcastReceiver { _, _ ->
        Log.d("ModelResponse", "start received")
        viewModel.onStartReceived()
    }

    private val resultReceiver = SecureBroadcastReceiver { _, intent ->
        Log.d("ModelResponse", "result received")
        viewModel.onResultReceived(intent)
    }

    override fun onMaterialDialogCreated(materialDialog: MaterialDialog, savedInstanceState: Bundle?) = materialDialog.apply {
        dialog = this
        title(R.string.settings_test_listen)
        customView(R.layout.bs_manual_trigger)
        isCancelable = false
        noAutoDismiss()
        positiveButton(android.R.string.cancel){
            it.dismiss()
        }
        binding = BsManualTriggerBinding.bind(getCustomView())
        lifecycleScope.launch {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        keepScreenOn = true
        requireContext().registerReceiver(startReceiver, recognitionStartedIntentFilter)
        requireContext().registerReceiver(resultReceiver, recognitionResponseIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        keepScreenOn = false
        requireContext().unregisterReceiver(startReceiver)
        requireContext().unregisterReceiver(resultReceiver)
    }

    private fun handleState(state: SettingsManualTriggerBottomSheetViewModel.State) = when(state) {
        is SettingsManualTriggerBottomSheetViewModel.State.AwaitingStart, SettingsManualTriggerBottomSheetViewModel.State.Started -> {
            binding.bsManualTriggerStatus.text = getString(R.string.bs_manual_trigger_state_awaiting_start)
            binding.bsManualTriggerStatusSecondary.visibility = View.INVISIBLE
            binding.bsManualTriggerProgressSpin.visibility = View.VISIBLE
            binding.bsManualTriggerImageview.visibility = View.INVISIBLE
            binding.bsManualTriggerLottie.visibility = View.INVISIBLE
        }
        is SettingsManualTriggerBottomSheetViewModel.State.Running -> {
            binding.bsManualTriggerStatus.text = getString(R.string.bs_manual_trigger_state_listening)
            binding.bsManualTriggerStatusSecondary.visibility = View.INVISIBLE
            binding.bsManualTriggerProgressSpin.visibility = View.INVISIBLE
            binding.bsManualTriggerImageview.visibility = View.INVISIBLE
            binding.bsManualTriggerLottie.run {
                visibility = View.VISIBLE
                playAnimation()
            }
        }
        is SettingsManualTriggerBottomSheetViewModel.State.AwaitingResult -> {
            binding.bsManualTriggerStatus.text = getString(R.string.bs_manual_trigger_state_awaiting_response)
            binding.bsManualTriggerStatusSecondary.visibility = View.INVISIBLE
            binding.bsManualTriggerProgressSpin.visibility = View.VISIBLE
            binding.bsManualTriggerLottie.visibility = View.INVISIBLE
            binding.bsManualTriggerImageview.visibility = View.INVISIBLE
        }
        is SettingsManualTriggerBottomSheetViewModel.State.Complete -> {
            setButtonClose()
            if(state.outputExists) addPlaybackButton()
            val isRecognised = state.recognitionResult.recognitionResponse == RecognitionResponse.MUSIC_RECOGNIZED
            val troubleshootingType = state.recognitionResult.recognitionResponse.toTroubleshootingType()
            if(!isRecognised && troubleshootingType != null) addTroubleshootingButton(troubleshootingType)
            binding.bsManualTriggerStatusSecondary.visibility = View.INVISIBLE
            binding.bsManualTriggerProgressSpin.visibility = View.INVISIBLE
            binding.bsManualTriggerImageview.run {
                visibility = View.VISIBLE
                if(isRecognised) {
                    setImageDrawable(musicAvd)
                    musicAvd.start()
                }else{
                    setImageResource(R.drawable.ic_unknown)
                }
            }
            binding.bsManualTriggerLottie.visibility = View.INVISIBLE
            handleRecognitionResult(state.recognitionResult)
        }
        is SettingsManualTriggerBottomSheetViewModel.State.Error -> {
            setButtonClose()
            addTroubleshootingButton(state.errorType.toTroubleshootingType())
            binding.bsManualTriggerStatusSecondary.visibility = View.INVISIBLE
            binding.bsManualTriggerProgressSpin.visibility = View.INVISIBLE
            binding.bsManualTriggerImageview.run {
                visibility = View.VISIBLE
                setImageResource(R.drawable.ic_error)
            }
            binding.bsManualTriggerLottie.visibility = View.INVISIBLE
            handleError(state.errorType)
        }
    }

    private fun setButtonClose(){
        dialog.positiveButton(R.string.close){
            it.dismiss()
        }
    }

    private fun addPlaybackButton(){
        dialog.neutralButton(R.string.bs_manual_trigger_state_complete_tertiary){
            viewModel.onPlaybackClicked()
        }
    }

    private fun addTroubleshootingButton(troubleshootingType: SettingsManualTriggerTroubleshootingBottomSheetFragment.TroubleshootingType) {
        dialog.negativeButton(R.string.bs_manual_trigger_troubleshooting){
            viewModel.onTroubleshootingClicked(troubleshootingType)
        }
    }

    private fun handleRecognitionResult(recognitionResult: RecognitionResult) = when(recognitionResult) {
        is RecognitionResult.MusicRecognitionResult -> {
            val trackMetadata = recognitionResult.trackMetadata
            binding.bsManualTriggerStatusSecondary.text = getString(R.string.bs_manual_trigger_state_response_music_desc, trackMetadata.track, trackMetadata.artist)
            binding.bsManualTriggerStatus.text = getString(R.string.bs_manual_trigger_state_response_music)
            binding.bsManualTriggerStatusSecondary.visibility = View.VISIBLE
        }
        is RecognitionResult.GenericResult -> {
            handleRecognitionResponse(recognitionResult.recognitionResponse)
            binding.bsManualTriggerStatusSecondary.text = getString(R.string.bs_manual_trigger_state_error_desc)
            binding.bsManualTriggerStatusSecondary.visibility = View.VISIBLE
        }
        is Error -> {
            handleRecognitionResponse(recognitionResult.recognitionResponse)
        }
        else -> null
    }

    private fun handleRecognitionResponse(recognitionResponse: RecognitionResponse) = when {
        recognitionResponse == RecognitionResponse.MUSIC_UNRECOGNIZED -> binding.bsManualTriggerStatus.text = getString(R.string.bs_manual_trigger_state_response_no_music)
        recognitionResponse == RecognitionResponse.NOT_MUSIC -> binding.bsManualTriggerStatus.text = getString(R.string.bs_manual_trigger_state_response_not_music)
        else -> binding.bsManualTriggerStatus.text = getString(R.string.bs_manual_trigger_state_response_unknown)
    }

    private fun handleError(errorType: SettingsManualTriggerBottomSheetViewModel.ErrorType) = when {
        errorType == SettingsManualTriggerBottomSheetViewModel.ErrorType.FAILED_TO_START -> binding.bsManualTriggerStatus.text = getString(R.string.bs_manual_trigger_state_error_not_started)
        errorType == SettingsManualTriggerBottomSheetViewModel.ErrorType.NO_RESPONSE -> binding.bsManualTriggerStatus.text = getString(R.string.bs_manual_trigger_state_error_no_response)
        errorType == SettingsManualTriggerBottomSheetViewModel.ErrorType.BAD_RESPONSE -> binding.bsManualTriggerStatus.text = getString(R.string.bs_manual_trigger_state_error_bad_response)
        else -> null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.deleteCachedInput()
    }

}