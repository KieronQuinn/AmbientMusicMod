package com.kieronquinn.app.ambientmusicmod.app.ui.settings.lockscreenoverlay.position

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.FragmentLockscreenOverlayPositionBinding
import com.kieronquinn.app.ambientmusicmod.utils.autoCleared
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.max
import kotlin.math.min

class SettingsLockscreenOverlayPositionFragment :
    Fragment(R.layout.fragment_lockscreen_overlay_position) {

    private val viewModel by viewModel<SettingsLockscreenOverlayPositionViewModel>()
    private var binding by autoCleared<FragmentLockscreenOverlayPositionBinding>()

    private val viewWidth: Float
        get() = binding.root.measuredWidth.toFloat()

    private val viewHeight: Float
        get() = binding.root.measuredHeight.toFloat()

    private val containerHeight: Float
        get() = binding.fragmentLockscreenOverlayPositionContainer.root.measuredHeight.toFloat()

    private val containerWidth: Float
        get() = binding.fragmentLockscreenOverlayPositionContainer.root.measuredWidth.toFloat()

    private val centerX: Float
        get() = (viewWidth / 2f) - (containerWidth / 2f)

    private val maxY by lazy {
        viewHeight - containerHeight
    }

    private val maxX by lazy {
        viewWidth - containerWidth
    }

    private val defaultPosition by lazy {
        val bottomPadding = resources.getDimension(R.dimen.activity_padding)
        val bottomHeight = viewHeight - bottomPadding - containerHeight
        Pair(centerX, bottomHeight)
    }

    private val dragListener = View.OnDragListener { view, event ->
        when (event.action) {
            DragEvent.ACTION_DRAG_ENDED -> {
                val halfWidth = view.measuredWidth / 2f
                val halfHeight = view.measuredHeight / 2f
                val x = max(min(event.x - halfWidth, maxX), 0f)
                val y = max(min(event.y - halfHeight, maxY), 0f)
                viewModel.setPosition(x, y)
            }
            DragEvent.ACTION_DRAG_STARTED -> {
                view.alpha = 0f
            }
        }
        true
    }

    private val touchListener = View.OnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            val dragShadowBuilder = View.DragShadowBuilder(v)
            v.startDragAndDrop(null, dragShadowBuilder, null, 0)
        }
        true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLockscreenOverlayPositionBinding.bind(view)
        with(binding.fragmentLockscreenOverlayPositionContainer) {
            overlayLockscreenText.text = getString(R.string.settings_lockscreen_overlay_sample_text)
            (overlayLockscreenIcon.drawable as? AnimatedVectorDrawable)?.start()
            root.setOnDragListener(dragListener)
            root.setOnTouchListener(touchListener)
            root.post {
                lifecycleScope.launch {
                    viewModel.position.collect {
                        val position = it ?: defaultPosition
                        binding.fragmentLockscreenOverlayPositionContainer.root.run {
                            x = position.first
                            y = position.second
                            alpha = 1f
                        }
                    }
                }
            }
        }
        with(binding){
            settingsLockscreenOverlaySampleCenterHorizontal.setOnClickListener {
                viewModel.centerHorizontally(centerX)
            }
            settingsLockscreenOverlaySampleReset.setOnClickListener {
                viewModel.resetPosition()
            }
        }
    }

    fun onBackPressed(){
        viewModel.save()
    }

}