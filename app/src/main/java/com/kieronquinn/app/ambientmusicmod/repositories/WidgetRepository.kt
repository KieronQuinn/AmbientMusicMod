package com.kieronquinn.app.ambientmusicmod.repositories

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.util.SizeF
import android.view.LayoutInflater
import android.view.View
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.widget.RemoteViewsCompat.setImageViewColorFilter
import androidx.core.widget.RemoteViewsCompat.setProgressBarIndeterminateTintList
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.PACKAGE_NAME_PAM
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.providers.AmbientMusicModWidget41
import com.kieronquinn.app.ambientmusicmod.providers.AmbientMusicModWidget42
import com.kieronquinn.app.ambientmusicmod.providers.AmbientMusicModWidgetDynamic
import com.kieronquinn.app.ambientmusicmod.providers.AmbientMusicModWidgetMinimal
import com.kieronquinn.app.ambientmusicmod.repositories.RecognitionRepository.RecognitionState
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository.SettingsState
import com.kieronquinn.app.ambientmusicmod.service.AmbientMusicModForegroundService
import com.kieronquinn.app.ambientmusicmod.utils.extensions.autoClearAfterBy
import com.kieronquinn.app.ambientmusicmod.utils.extensions.broadcastReceiverAsFlow
import com.kieronquinn.app.ambientmusicmod.utils.extensions.dip
import com.kieronquinn.app.ambientmusicmod.utils.extensions.ellipsizeToSize
import com.kieronquinn.app.ambientmusicmod.utils.extensions.firstNotNull
import com.kieronquinn.app.ambientmusicmod.utils.extensions.wallpaperSupportsDarkText
import com.kieronquinn.app.pixelambientmusic.model.RecognitionMetadata
import com.kieronquinn.app.pixelambientmusic.model.RecognitionSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface WidgetRepository {

    fun notifyChanged()
    fun notifyRecognitionState(recognitionState: RecognitionState?)

}

class WidgetRepositoryImpl(
    context: Context,
    settings: SettingsRepository,
    remoteSettingsRepository: RemoteSettingsRepository,
    private val recognitionRepository: RecognitionRepository
): WidgetRepository {

    private val packageManager = context.packageManager
    private val appWidgetManager =
        context.getSystemService(Context.APPWIDGET_SERVICE) as AppWidgetManager

    private val resources = context.resources
    private var recognitionJob: Job? = null
    private val layoutInflater = LayoutInflater.from(context)

    private val scope = MainScope()

    private val bufferTime = settings.recognitionBuffer.asFlow()
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val delayTime = combine(
        settings.recognitionPeriod.asFlow(),
        settings.recognitionBuffer.asFlow()
    ) { time, buffer ->
        time.period + buffer.time
    }.stateIn(scope, SharingStarted.Eagerly, null)

    companion object {
        private val WIDGETS_ANDROID_12 = arrayOf(
            ComponentName(BuildConfig.APPLICATION_ID, AmbientMusicModWidgetDynamic::class.java.name),
            ComponentName(BuildConfig.APPLICATION_ID, AmbientMusicModWidgetMinimal::class.java.name),
        )
        private val WIDGETS_ANDROID_11 = arrayOf(
            ComponentName(BuildConfig.APPLICATION_ID, AmbientMusicModWidget41::class.java.name),
            ComponentName(BuildConfig.APPLICATION_ID, AmbientMusicModWidget42::class.java.name)
        )

        private const val INTENT_ACTION_REQUEST_RECOGNITION =
            "${BuildConfig.APPLICATION_ID}.action.REQUEST_RECOGNITION"

        private const val INTENT_ACTION_REQUEST_ON_DEMAND_RECOGNITION =
            "${BuildConfig.APPLICATION_ID}.action.REQUEST_ON_DEMAND_RECOGNITION"
    }

    private fun getSupportedWidgetComponents(): Array<ComponentName> {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            WIDGETS_ANDROID_12
        }else WIDGETS_ANDROID_11
    }

    private fun getUnsupportedWidgetComponents(): Array<ComponentName> {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            WIDGETS_ANDROID_11
        }else WIDGETS_ANDROID_12
    }

    private fun setupWidgetComponents(){
        val setEnabled = { component: ComponentName, enabled: Boolean ->
            val state = if(enabled){
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }else{
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            packageManager.setComponentEnabledSetting(
                component,
                state,
                PackageManager.DONT_KILL_APP
            )
        }
        val toEnable = getSupportedWidgetComponents()
        val toDisable = getUnsupportedWidgetComponents()
        toEnable.forEach { setEnabled(it, true) }
        toDisable.forEach { setEnabled(it, false) }
    }

    init {
        setupWidgetComponents()
    }

    private val widgetChanged = MutableStateFlow(System.currentTimeMillis())
    private val recognitionState = MutableStateFlow<RecognitionState?>(null)
    private val enabled = remoteSettingsRepository.getRemoteSettings(scope).filterNotNull().map {
        it is SettingsState.Available && it.mainEnabled
    }
    private val onDemandEnabled = remoteSettingsRepository.getOnDemandSupportedAndEnabled()

    private val width41 = resources.getDimension(R.dimen.widget_4_1_text_width)
    private val width42 = resources.getDimension(R.dimen.widget_4_2_text_width)

    private val clickNnfpIntent = PendingIntent.getBroadcast(
        context,
        1001,
        Intent(INTENT_ACTION_REQUEST_RECOGNITION).apply { `package` = BuildConfig.APPLICATION_ID },
        PendingIntent.FLAG_IMMUTABLE
    )

    private val clickOnDemandIntent = PendingIntent.getBroadcast(
        context,
        1002,
        Intent(INTENT_ACTION_REQUEST_ON_DEMAND_RECOGNITION)
            .apply { `package` = BuildConfig.APPLICATION_ID },
        PendingIntent.FLAG_IMMUTABLE
    )

    private val clickIntent = PendingIntent.getActivity(
        context,
        1003,
        Intent("com.google.intelligence.sense.NOW_PLAYING_HISTORY").apply {
            `package` = PACKAGE_NAME_PAM
        },
        PendingIntent.FLAG_IMMUTABLE
    )

    private val nnfpClicked = context.broadcastReceiverAsFlow(INTENT_ACTION_REQUEST_RECOGNITION)
    private val onDemandClicked =
        context.broadcastReceiverAsFlow(INTENT_ACTION_REQUEST_ON_DEMAND_RECOGNITION)
    private val shadow = settings.lockscreenOverlayShadowEnabled.asFlow()

    private val widgets = widgetChanged.map {
        getSupportedWidgetComponents().map {
            Pair(it, appWidgetManager.getAppWidgetIds(it))
        }.flatMap {
            it.second.map { id ->
                val options = appWidgetManager.getAppWidgetOptions(id)
                val width = context.dip(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH))
                AppWidget(id, it.first, width)
            }
        }
    }

    override fun notifyChanged() {
        scope.launch {
            widgetChanged.emit(System.currentTimeMillis())
        }
    }

    private val textColour = combine(
        context.wallpaperSupportsDarkText(),
        settings.lockscreenOverlayColour.asFlow(),
        settings.lockscreenOverlayCustomColour.asFlow()
    ) { darkWallpaperText, widgetTextColour, widgetCustomTextColour ->
        val automatic = if(darkWallpaperText) Color.BLACK else Color.WHITE
        when(widgetTextColour) {
            SettingsRepository.OverlayTextColour.AUTOMATIC -> automatic
            SettingsRepository.OverlayTextColour.BLACK -> Color.BLACK
            SettingsRepository.OverlayTextColour.WHITE -> Color.WHITE
            SettingsRepository.OverlayTextColour.CUSTOM -> {
                if(widgetCustomTextColour == Int.MAX_VALUE) automatic else widgetCustomTextColour
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    private val style = combine(
        textColour,
        shadow
    ) { colour, shadow ->
        Pair(colour, shadow)
    }

    private val enabledState = combine(
        enabled,
        onDemandEnabled
    ) { enabled, onDemand ->
        Pair(enabled, onDemand)
    }

    private fun setupWidgets() = scope.launch {
        combine(
            widgets,
            recognitionState.autoClearAfterBy {
                if(it is RecognitionState.Recognised) it.metadata.getDelayTime() else null
            },
            enabledState,
            style
        ) { widget, state, enabled, style ->
            WidgetState(enabled.first, widget, state, enabled.second, style.second)
        }.collect {
            it.widgets.sendLayouts(it.enabled, it.state, it.onDemandEnabled, it.shadow)
        }
    }

    init {
        setupWidgets()
    }

    private suspend fun List<AppWidget>.sendLayouts(
        enabled: Boolean,
        state: RecognitionState?,
        onDemandEnabled: Boolean,
        shadow: Boolean
    ) = forEach {
        try {
            appWidgetManager.updateAppWidget(
                it.id,
                it.getRemoteViews(enabled, state, onDemandEnabled, shadow)
            )
        }catch (e: Throwable) {
            //Suppress, shouldn't happen unless system has lagged
        }
    }

    private fun getRemoteView41(): RemoteViews {
        return RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget_4_1)
    }

    private fun getRemoteView42(): RemoteViews {
        return RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget_4_2)
    }

    private fun getRemoteViewMinimal(shadow: Boolean): RemoteViews {
        return if(shadow) {
            RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget_minimal_shadow)
        }else{
            RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget_minimal_no_shadow)
        }
    }

    private val textPaint by lazy {
        layoutInflater.inflate(R.layout.widget_4_1, null)
            .findViewById<TextView>(R.id.widget_text).paint
    }

    private val textPaintMinimal by lazy {
        layoutInflater.inflate(R.layout.widget_minimal_shadow, null)
            .findViewById<TextView>(R.id.widget_text).paint
    }

    @SuppressLint("NewApi")
    private suspend fun getRemoteViewDynamic(
        enabled: Boolean,
        state: RecognitionState?,
        onDemandEnabled: Boolean
    ): RemoteViews {
        return RemoteViews(
            mapOf(
                SizeF(250f, 40f) to getRemoteView41()
                    .applyState(enabled, state, onDemandEnabled, false, width41 * 0.5f),
                SizeF(250f, 140f) to getRemoteView42()
                    .applyState(enabled, state, onDemandEnabled, false, width42 * 0.5f)
            )
        )
    }

    private suspend fun RemoteViews.applyState(
        enabled: Boolean,
        state: RecognitionState?,
        onDemandEnabled: Boolean,
        minimal: Boolean,
        width: Float
    ): RemoteViews = apply {
        val textPaint = if(minimal) textPaintMinimal else textPaint
        val textColour = textColour.firstNotNull()
        val text = when {
            !enabled -> resources.getString(R.string.widget_error_disabled)
            state is RecognitionState.Recognised -> {
                val result = state.recognitionResult
                val before = result.trackName.ellipsizeToSize(textPaint, width)
                val after = result.artist.ellipsizeToSize(textPaint, width)
                resources.getString(R.string.widget_recognised, before, after)
            }
            state is RecognitionState.Recording || state is RecognitionState.Recognising -> {
                resources.getString(R.string.widget_recognising)
            }
            state is RecognitionState.Failed || state == null -> {
                resources.getString(R.string.widget_not_recognised)
            }
            state is RecognitionState.Error -> {
                if(state.errorReason == RecognitionState.ErrorReason.DISABLED){
                    resources.getString(R.string.widget_error_disabled)
                }else {
                    resources.getString(R.string.widget_error)
                }
            }
            else  -> {
                resources.getString(R.string.widget_not_recognised)
            }
        }
        if(minimal) {
            val iconIndex = when {
                !enabled -> null
                state is RecognitionState.Recognised -> IconViewIndex.RECOGNISED
                state is RecognitionState.Failed && state.recognitionFailure.source == RecognitionSource.ON_DEMAND -> {
                    IconViewIndex.NO_MUSIC
                }
                onDemandEnabled -> IconViewIndex.ON_DEMAND
                else -> null //Show nothing otherwise
            }
            val minimalText = when {
                !enabled -> ""
                state is RecognitionState.Recognised -> text
                state is RecognitionState.Recognising && state.source == RecognitionSource.ON_DEMAND -> {
                    resources.getString(R.string.lockscreen_overlay_ondemand_searching)
                }
                state is RecognitionState.Failed && state.recognitionFailure.source == RecognitionSource.ON_DEMAND -> {
                    resources.getString(R.string.lockscreen_overlay_ondemand_no_result)
                }
                else -> "" //Show nothing otherwise
            }
            if(iconIndex == null) {
                setInt(R.id.widget_root, "setVisibility", View.GONE)
            }else{
                setInt(R.id.widget_root, "setVisibility", View.VISIBLE)
                setTextViewText(R.id.widget_text, minimalText)
                setTextColor(R.id.widget_text, textColour)
                setProgressBarIndeterminateTint(R.id.widget_icon_recognised, textColour)
                setProgressBarIndeterminateTint(R.id.widget_icon_no_music, textColour)
                setImageViewColorFilter(R.id.widget_button_on_demand, textColour)
                setInt(R.id.widget_button_on_demand, "setColorFilter", textColour)
                setInt(
                    R.id.widget_text,
                    "setVisibility",
                    if (minimalText.isNotEmpty()) View.VISIBLE else View.GONE
                )
                setInt(
                    R.id.widget_icon_recognised,
                    "setVisibility",
                    if (iconIndex == IconViewIndex.RECOGNISED) View.VISIBLE else View.GONE
                )
                setInt(
                    R.id.widget_icon_no_music,
                    "setVisibility",
                    if (iconIndex == IconViewIndex.NO_MUSIC) View.VISIBLE else View.GONE
                )
                setInt(
                    R.id.widget_button_on_demand,
                    "setVisibility",
                    if (iconIndex == IconViewIndex.ON_DEMAND) View.VISIBLE else View.GONE
                )
                if(state is RecognitionState.Recognised) {
                    setOnClickPendingIntent(R.id.widget_root, clickIntent)
                }else if (onDemandEnabled) {
                    setOnClickPendingIntent(R.id.widget_root, clickOnDemandIntent)
                    setOnClickPendingIntent(R.id.widget_button_on_demand, clickOnDemandIntent)
                }
            }
        }else {
            val iconIndex = when {
                !enabled -> IconViewIndex.NO_MUSIC
                state is RecognitionState.Recognised -> IconViewIndex.RECOGNISED
                state is RecognitionState.Recording || state is RecognitionState.Recognising -> {
                    IconViewIndex.WAVEFORM
                }
                state is RecognitionState.Failed || state is RecognitionState.Error || state == null -> {
                    IconViewIndex.NO_MUSIC
                }
                else -> IconViewIndex.NO_MUSIC
            }
            val onDemandVisibility = if (onDemandEnabled) View.VISIBLE else View.GONE
            setTextViewText(R.id.widget_text, text)
            setInt(R.id.widget_button_on_demand, "setVisibility", onDemandVisibility)
            setInt(
                R.id.widget_icon_recognised,
                "setVisibility",
                if (iconIndex == IconViewIndex.RECOGNISED) View.VISIBLE else View.GONE
            )
            setInt(
                R.id.widget_icon_no_music,
                "setVisibility",
                if (iconIndex == IconViewIndex.NO_MUSIC) View.VISIBLE else View.GONE
            )
            setInt(
                R.id.widget_icon_waveform,
                "setVisibility",
                if (iconIndex == IconViewIndex.WAVEFORM) View.VISIBLE else View.GONE
            )
            setOnClickPendingIntent(R.id.widget_button_nnfp, clickNnfpIntent)
            setOnClickPendingIntent(R.id.widget_button_on_demand, clickOnDemandIntent)
            setOnClickPendingIntent(R.id.widget_text, clickIntent)
        }
    }

    private suspend fun AppWidget.getRemoteViews(
        enabled: Boolean,
        state: RecognitionState?,
        onDemandEnabled: Boolean,
        shadow: Boolean
    ): RemoteViews? {
        return when(provider.className){
            AmbientMusicModWidget41::class.java.name -> {
                getRemoteView41().applyState(enabled, state, onDemandEnabled, false, width41)
            }
            AmbientMusicModWidget42::class.java.name -> {
                getRemoteView42().applyState(enabled, state, onDemandEnabled, false, width42)
            }
            AmbientMusicModWidgetMinimal::class.java.name -> {
                val minimalWidth = width * 0.5f
                Log.d("WR", "Available width: $minimalWidth")
                getRemoteViewMinimal(shadow)
                    .applyState(enabled, state, onDemandEnabled, true, minimalWidth)
            }
            AmbientMusicModWidgetDynamic::class.java.name -> {
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
                //Must be reconstructed as modifications unsupported
                getRemoteViewDynamic(enabled, state, onDemandEnabled)
            }
            else ->  return null
        }
    }

    override fun notifyRecognitionState(recognitionState: RecognitionState?) {
        scope.launch {
            this@WidgetRepositoryImpl.recognitionState.emit(recognitionState)
        }
    }

    private fun startRecognition(source: RecognitionSource) {
        recognitionJob?.cancel()
        recognitionJob = scope.launch {
            val flow = if(source == RecognitionSource.NNFP){
                recognitionRepository.requestRecognition()
            }else{
                recognitionRepository.requestOnDemandRecognition()
            }
            flow.collect {
                recognitionState.emit(it)
                if(it is RecognitionState.Recognised){
                    AmbientMusicModForegroundService.sendManualRecognition(it)
                }
            }
        }
    }

    private fun setupNnfpClicked() = scope.launch {
        nnfpClicked.collect {
            startRecognition(RecognitionSource.NNFP)
        }
    }

    private fun setupOnDemandClicked() = scope.launch {
        onDemandClicked.collect {
            startRecognition(RecognitionSource.ON_DEMAND)
        }
    }

    private suspend fun RecognitionMetadata?.getDelayTime(): Long {
        return this?.remainingTime?.let {
            it + bufferTime.firstNotNull().time
        } ?: delayTime.firstNotNull()
    }

    private fun RemoteViews.setProgressBarIndeterminateTint(id: Int, colour: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setProgressBarIndeterminateTintList(id, ColorStateList.valueOf(colour))
        }
    }

    init {
        setupNnfpClicked()
        setupOnDemandClicked()
    }

    private data class AppWidget(val id: Int, val provider: ComponentName, val width: Int)

    private data class WidgetState(
        val enabled: Boolean,
        val widgets: List<AppWidget>,
        val state: RecognitionState?,
        val onDemandEnabled: Boolean,
        val shadow: Boolean
    )

    enum class IconViewIndex {
        RECOGNISED, NO_MUSIC, WAVEFORM, ON_DEMAND
    }

}