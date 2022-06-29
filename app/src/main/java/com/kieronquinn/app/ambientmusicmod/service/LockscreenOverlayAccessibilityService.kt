package com.kieronquinn.app.ambientmusicmod.service

import android.app.KeyguardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.PowerManager
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.databinding.OverlayNowPlayingBinding
import com.kieronquinn.app.ambientmusicmod.databinding.OverlayNowPlayingClassicBinding
import com.kieronquinn.app.ambientmusicmod.model.lockscreenoverlay.LockscreenOverlayStyle
import com.kieronquinn.app.ambientmusicmod.model.lockscreenoverlay.OverlayState
import com.kieronquinn.app.ambientmusicmod.model.lockscreenoverlay.stateEquals
import com.kieronquinn.app.ambientmusicmod.repositories.AccessibilityRepository
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository.SettingsState
import com.kieronquinn.app.ambientmusicmod.repositories.SettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.ShizukuServiceRepository
import com.kieronquinn.app.ambientmusicmod.utils.accessibility.LifecycleAccessibilityService
import com.kieronquinn.app.ambientmusicmod.utils.extensions.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.android.ext.android.inject

class LockscreenOverlayAccessibilityService : LifecycleAccessibilityService() {

    companion object {
        private var MESSAGE_HANDLER: Handler? = null
        private var LAST_STATE: OverlayState? = null

        private val WINDOW_MANAGER_FLAGS = arrayOf(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )

        fun sendState(state: OverlayState) {
            LAST_STATE = state
            MESSAGE_HANDLER?.let {
                it.sendMessage(Message.obtain(it, 0, state))
            }
        }
    }

    private val settings by inject<SettingsRepository>()
    private val remoteSettings by inject<RemoteSettingsRepository>()
    private val shizukuService by inject<ShizukuServiceRepository>()
    private val accessibility by inject<AccessibilityRepository>()

    private val accessibilityWindow = MutableStateFlow<AccessibilityWindow?>(null)
    private val lockscreenOverlayState = MutableStateFlow<OverlayState?>(null)
    private var currentBinding: OverlayBinding? = null

    private val systemUiPackage = shizukuService.isReady.mapLatest {
        shizukuService.runWithService { it.systemUIPackageName }.unwrap() ?: "com.android.systemui"
    }

    private val lockScreenText = systemUiPackage.mapLatest {
        getString(it, "accessibility_desc_lock_screen", true)
    }

    private val darkWallpaperText by lazy {
        wallpaperSupportsDarkText()
            .stateIn(lifecycleScope, SharingStarted.Eagerly, null)
    }

    private val overlayTextColour = settings.lockscreenOverlayColour.asFlow()
        .stateIn(lifecycleScope, SharingStarted.Eagerly, null)

    private val overlayCustomTextColour = settings.lockscreenOverlayCustomColour.asFlow()
        .stateIn(lifecycleScope, SharingStarted.Eagerly, null)

    private val mainSwitch = combine(
        remoteSettings.getRemoteSettings(lifecycleScope).filterNotNull(),
        accessibility.enabled
    ) { remote, overlay ->
        remote is SettingsState.Available && remote.mainEnabled && overlay
    }

    /**
     *  Whether the current window is SystemUI & the window name matches [lockScreenText].
     *  If [SettingsRepository.lockscreenOverlayEnhanced] is not enabled, this will always emit true.
     */
    private val accessibilityWindowIsLockscreen = combine(
        settings.lockscreenOverlayEnhanced.asFlow(),
        accessibilityWindow,
        systemUiPackage,
        lockScreenText
    ) { enhanced, window, systemui, lockscreen ->
        if(!enhanced) return@combine true
        window?.packageName == systemui && window.windowName == lockscreen
    }

    private val overlayState by lazy {
        combine(
            mainSwitch,
            lockscreenOverlayState.filterNotNull(),
            keyguardShowing,
            accessibilityWindowIsLockscreen
        ) { main, state, visible, window ->
            if(!main) return@combine OverlayState.Hidden
            if(visible && window){
                state
            }else OverlayState.Hidden
        }.distinctUntilChanged { old, new -> new.stateEquals(old) }
    }

    private val layoutInflater by lazy {
        LayoutInflater.from(this)
    }

    private val textMaxWidth by lazy {
        resources.getDimension(R.dimen.overlay_text_max_width)
    }

    private val textMaxWidthHalf by lazy {
        resources.getDimension(R.dimen.overlay_text_max_width_half)
    }

    private val viewLock = Mutex()

    private val keyguardShowing by lazy {
        MutableStateFlow(isLockscreenVisible())
    }

    private val keyguardManager by lazy {
        getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }

    private val powerManager by lazy {
        getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    private val windowManager by lazy {
        getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val messageHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            lifecycleScope.launchWhenCreated {
                lockscreenOverlayState.emit(msg.obj as OverlayState)
            }
        }
    }

    private val layoutParams by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WINDOW_MANAGER_FLAGS.or(),
            PixelFormat.TRANSLUCENT
        ).apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
    }

    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launchWhenCreated {
            accessibility.onAccessibilityStarted()
        }
        LAST_STATE?.let {
            LAST_STATE = null
            if(!it.isValid()) return@let
            lifecycleScope.launchWhenCreated {
                lockscreenOverlayState.emit(it)
            }
        }
        MESSAGE_HANDLER = messageHandler
        setupLockscreenState()
    }

    override fun onDestroy() {
        MESSAGE_HANDLER = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if(event.packageName == BuildConfig.APPLICATION_ID) return //Prevent self-triggers
        lifecycleScope.launchWhenCreated {
            accessibilityWindow.emit(
                AccessibilityWindow(
                    event.packageName?.toString() ?: return@launchWhenCreated,
                    event.text?.firstOrNull() ?: return@launchWhenCreated
                )
            )
        }
    }

    override fun onInterrupt() {
        //No-op
    }

    private fun setupLockscreenState() = lifecycleScope.launchWhenCreated {
        onKeyguardStateChanged().collectLatest {
            keyguardShowing.emit(isLockscreenVisible())
        }
    }

    /**
     *  Returns if the Lock Screen is visible, in theory - notification shade or other apps
     *  may be in front of it.
     */
    private fun isLockscreenVisible(): Boolean {
        return powerManager.isInteractive && keyguardManager.isKeyguardLocked
    }

    private fun Array<Int>.or(): Int {
        var flag = 0
        forEach {
            flag = flag or it
        }
        return flag
    }

    private sealed class OverlayBinding(
        open val binding: ViewBinding,
        val style: LockscreenOverlayStyle
    ) {

        data class New(override val binding: OverlayNowPlayingBinding): OverlayBinding(
            binding, LockscreenOverlayStyle.NEW
        ) {
            override val title = binding.nowPlayingText
            override val icon = binding.nowPlayingIcon
        }

        data class Classic(override val binding: OverlayNowPlayingClassicBinding): OverlayBinding(
            binding, LockscreenOverlayStyle.CLASSIC
        ) {
            override val title = binding.nowPlayingText
            override val icon = binding.nowPlayingIcon
        }

        abstract val title: TextView
        abstract val icon: ImageView

    }

    private suspend fun getOverlayTextColour(darkText: Boolean): Int {
        val automatic = if(darkText) Color.BLACK else Color.WHITE
        return when(overlayTextColour.firstNotNull()) {
            SettingsRepository.OverlayTextColour.AUTOMATIC -> automatic
            SettingsRepository.OverlayTextColour.BLACK -> Color.BLACK
            SettingsRepository.OverlayTextColour.WHITE -> Color.WHITE
            SettingsRepository.OverlayTextColour.CUSTOM -> {
                val colour = overlayCustomTextColour.firstNotNull()
                if(colour == Int.MAX_VALUE) automatic else colour
            }
        }
    }

    private suspend fun setupAndAttachView(state: OverlayState, darkText: Boolean, yPos: Int) = viewLock.withLock {
        val colour = getOverlayTextColour(darkText)
        val style = when (state) {
            is OverlayState.Hidden -> {
                detachViewLocked()
                currentBinding = null
                return@withLock
            }
            is OverlayState.Shown -> {
                state.style
            }
            is OverlayState.IconOnly -> {
                state.style
            }
        }
        val current = currentBinding?.let {
            if(it.style != style) {
                detachViewLocked()
                return@let createBindingForStyle(style)
            }
            it
        } ?: createBindingForStyle(style)
        currentBinding = current
        val icon = when(state){
            is OverlayState.Shown -> state.icon
            is OverlayState.IconOnly -> state.icon
            else -> null
        }?.let {
            ContextCompat.getDrawable(this, it)
        }
        val paint = current.title.paint
        val text = when(state){
            is OverlayState.Shown -> {
                if(state.contentAfterBullet != null){
                    val before = state.content.ellipsizeToSize(paint, textMaxWidthHalf)
                    val after = state.contentAfterBullet.ellipsizeToSize(paint, textMaxWidthHalf)
                    getString(R.string.lockscreen_overlay_song, before, after)
                }else{
                    state.content.ellipsizeToSize(paint, textMaxWidth)
                }
            }
            else -> null
        }
        current.icon.setImageDrawable(icon)
        current.icon.imageTintList = ColorStateList.valueOf(colour)
        current.title.isVisible = text != null
        current.title.text = text
        current.title.setTextColor(colour)
        layoutParams.y = yPos
        try {
            windowManager.addView(current.binding.root, layoutParams)
        }catch (e: IllegalStateException){
            //Already added
        }
        if(icon is AnimatedVectorDrawable){
            icon.start()
        }
        val onClick = when(state){
            is OverlayState.Shown -> state.onClick
            is OverlayState.IconOnly -> state.onClick
            else -> null
        }
        if(onClick != null) {
            lifecycleScope.launchWhenCreated {
                current.binding.root.onClicked().collect {
                    onClick()
                }
            }
        }else{
            current.binding.root.setOnClickListener(null)
        }
    }

    private suspend fun detachView() = viewLock.withLock {
        detachViewLocked()
    }

    private fun detachViewLocked() {
        val binding = currentBinding ?: return
        try {
            windowManager.removeViewImmediate(binding.binding.root)
        }catch (e: IllegalArgumentException){
            //Already removed
        }
    }

    private fun setupOverlayView() = lifecycleScope.launchWhenCreated {
        overlayState.collect { state ->
            val darkText = darkWallpaperText.firstNotNull()
            if(state is OverlayState.Hidden){
                detachView()
            }else{
                val yPos = when(state){
                    is OverlayState.Shown -> state.yPos
                    is OverlayState.IconOnly -> state.yPos
                    else -> throw RuntimeException("Invalid state")
                }
                setupAndAttachView(state, darkText, yPos)
            }
        }
    }

    private fun createBindingForStyle(style: LockscreenOverlayStyle): OverlayBinding {
        return when(style){
            LockscreenOverlayStyle.NEW -> {
                OverlayBinding.New(OverlayNowPlayingBinding.inflate(layoutInflater))
            }
            LockscreenOverlayStyle.CLASSIC -> {
                OverlayBinding.Classic(OverlayNowPlayingClassicBinding.inflate(layoutInflater))
            }
        }
    }

    init {
        setupOverlayView()
    }



    private fun OverlayState.isValid(): Boolean {
        if(this !is OverlayState.Shown) return true
        return System.currentTimeMillis() < endTime
    }

    private data class AccessibilityWindow(val packageName: String, val windowName: CharSequence?)

}