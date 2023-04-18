package com.kieronquinn.app.ambientmusicmod.ui.screens.lockscreen.position

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentLockscreenPositionBinding
import com.kieronquinn.app.ambientmusicmod.databinding.OverlayNowPlayingBinding
import com.kieronquinn.app.ambientmusicmod.databinding.OverlayNowPlayingClassicBinding
import com.kieronquinn.app.ambientmusicmod.model.lockscreenoverlay.LockscreenOverlayStyle
import com.kieronquinn.app.ambientmusicmod.ui.base.BoundFragment
import com.kieronquinn.app.ambientmusicmod.utils.extensions.measureSize
import com.kieronquinn.app.ambientmusicmod.utils.extensions.whenResumed
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class LockScreenPositionFragment: BoundFragment<FragmentLockscreenPositionBinding>(FragmentLockscreenPositionBinding::inflate) {

    private val viewModel by viewModel<LockScreenPositionViewModel>()
    private val args by navArgs<LockScreenPositionFragmentArgs>()
    private var overlayView: View? = null

    private val yPos by lazy {
        MutableStateFlow(viewModel.initialYPos)
    }

    private val windowManager by lazy {
        requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val screenHeight by lazy {
        windowManager.defaultDisplay.height
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.addOverlay()
        setupReset()
        setupBack()
        setupSave()
        Toast.makeText(requireContext(), R.string.lockscreen_position_toast, Toast.LENGTH_LONG)
            .show()
    }

    private fun setupReset() {
        binding.root.setOnLongClickListener {
            resetYPosition()
            true
        }
    }

    private fun setupBack() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object: OnBackPressedCallback(true)
            {
                override fun handleOnBackPressed() {
                    viewModel.onBackPressed()
                }
            }
        )
    }

    private fun setupSave() = whenResumed {
        yPos.debounce(250L).collectLatest {
            viewModel.commit(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        overlayView = null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun FrameLayout.addOverlay() {
        val binding = when(args.overlayStyle){
            LockscreenOverlayStyle.NEW -> {
                OverlayNowPlayingBinding.inflate(layoutInflater).apply {
                    nowPlayingText.text = getString(R.string.item_nowplaying_header_preview)
                    nowPlayingText.setTextColor(Color.WHITE)
                    nowPlayingIcon.imageTintList = ColorStateList.valueOf(Color.WHITE)
                    (nowPlayingIcon.drawable as AnimatedVectorDrawable).start()
                }
            }
            LockscreenOverlayStyle.CLASSIC -> {
                OverlayNowPlayingClassicBinding.inflate(layoutInflater).apply {
                    nowPlayingText.text = getString(R.string.item_nowplaying_header_preview)
                    nowPlayingText.setTextColor(Color.WHITE)
                    nowPlayingIcon.imageTintList = ColorStateList.valueOf(Color.WHITE)
                    (nowPlayingIcon.drawable as AnimatedVectorDrawable).start()
                }
            }
        }
        overlayView = binding.root
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val height = binding.root.measureSize(windowManager).second
        val rawYPos = (windowManager.defaultDisplay.height / 2f) + yPos.value
        binding.root.y = rawYPos - (height / 2f)
        binding.root.setOnTouchListener { view, motionEvent ->
            onTouch(view, motionEvent)
            true
        }
        addView(binding.root, layoutParams)
    }

    private fun resetYPosition() {
        val overlay = overlayView ?: return
        val height = overlay.measureSize(windowManager).second
        val rawYPos = (windowManager.defaultDisplay.height / 2f)
        overlay.y = rawYPos - (height / 2f)
        whenResumed {
            yPos.emit(0)
        }
    }

    private fun onTouch(view: View, motionEvent: MotionEvent) {
        if(motionEvent.action == MotionEvent.ACTION_MOVE){
            view.y = view.y + motionEvent.y
            whenResumed {
                yPos.emit(overlayView?.centerY() ?: return@whenResumed)
            }
        }
    }

    private fun setFullscreen(enabled: Boolean) {
        val controller = WindowCompat.getInsetsController(
            requireActivity().window, requireActivity().window.decorView
        )
        if(enabled){
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }else{
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onResume() {
        super.onResume()
        setFullscreen(true)
    }

    override fun onPause() {
        super.onPause()
        setFullscreen(false)
        //Allow saving after closing
        GlobalScope.launch {
            viewModel.commit(yPos.value)
        }
    }

    private fun View.centerY(): Int {
        return (this.y - (this.measuredHeight / 2f) - (screenHeight / 2f)).toInt()
    }

}