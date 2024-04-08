package com.kieronquinn.app.ambientmusicmod.repositories

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.SizeF
import android.view.LayoutInflater
import android.view.View
import android.widget.RemoteViews
import android.widget.TextView
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.PACKAGE_NAME_PAM
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.providers.AmbientMusicModWidget41
import com.kieronquinn.app.ambientmusicmod.providers.AmbientMusicModWidget42
import com.kieronquinn.app.ambientmusicmod.providers.AmbientMusicModWidgetDynamic
import com.kieronquinn.app.ambientmusicmod.repositories.RecognitionRepository.RecognitionState
import com.kieronquinn.app.ambientmusicmod.service.AmbientMusicModForegroundService
import com.kieronquinn.app.ambientmusicmod.utils.extensions.autoClearAfterBy
import com.kieronquinn.app.ambientmusicmod.utils.extensions.broadcastReceiverAsFlow
import com.kieronquinn.app.ambientmusicmod.utils.extensions.ellipsizeToSize
import com.kieronquinn.app.ambientmusicmod.utils.extensions.firstNotNull
import com.kieronquinn.app.pixelambientmusic.model.RecognitionMetadata
import com.kieronquinn.app.pixelambientmusic.model.RecognitionSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
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
            ComponentName(BuildConfig.APPLICATION_ID, AmbientMusicModWidgetDynamic::class.java.name)
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

    private val widgets = widgetChanged.map {
        getSupportedWidgetComponents().map {
            Pair(it, appWidgetManager.getAppWidgetIds(it))
        }.flatMap {
            it.second.map { id ->
                val options = appWidgetManager.getAppWidgetOptions(id)
                AppWidget(id, it.first)
            }
        }
    }

    override fun notifyChanged() {
        scope.launch {
            widgetChanged.emit(System.currentTimeMillis())
        }
    }

    private fun setupWidgets() = scope.launch {
        combine(
            widgets,
            recognitionState.autoClearAfterBy {
                if(it is RecognitionState.Recognised) it.metadata.getDelayTime() else null
            },
            onDemandEnabled
        ) { widget, state, onDemand ->
            Triple(widget, state, onDemand)
        }.collect {
            it.first.sendLayouts(it.second, it.third)
        }
    }

    init {
        setupWidgets()
    }

    private val remoteViews41 by lazy {
        getRemoteView41()
    }

    private val remoteViews42 by lazy {
        getRemoteView42()
    }

    private fun List<AppWidget>.sendLayouts(
        state: RecognitionState?, onDemandEnabled: Boolean
    ) = forEach {
        try {
            appWidgetManager.updateAppWidget(it.id, it.getRemoteViews(state, onDemandEnabled))
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

    private val textPaint by lazy {
        layoutInflater.inflate(R.layout.widget_4_1, null)
            .findViewById<TextView>(R.id.widget_text).paint
    }

    @SuppressLint("NewApi")
    private fun getRemoteViewDynamic(
        state: RecognitionState?, onDemandEnabled: Boolean
    ): RemoteViews {
        return RemoteViews(
            mapOf(
                SizeF(250f, 40f) to remoteViews41
                    .applyState(state, onDemandEnabled, width41 * 0.5f),
                SizeF(250f, 140f) to remoteViews42
                    .applyState(state, onDemandEnabled, width42 * 0.5f)
            )
        )
    }

    private fun RemoteViews.applyState(
        state: RecognitionState?, onDemandEnabled: Boolean, width: Float
    ): RemoteViews = apply {
        val text = when(state){
            is RecognitionState.Recognised -> {
                val result = state.recognitionResult
                val before = result.trackName.ellipsizeToSize(textPaint, width)
                val after = result.artist.ellipsizeToSize(textPaint, width)
                resources.getString(R.string.widget_recognised, before, after)
            }
            is RecognitionState.Recording, is RecognitionState.Recognising -> {
                resources.getString(R.string.widget_recognising)
            }
            is RecognitionState.Failed, null -> {
                resources.getString(R.string.widget_not_recognised)
            }
            is RecognitionState.Error -> {
                if(state.errorReason == RecognitionState.ErrorReason.DISABLED){
                    resources.getString(R.string.widget_error_disabled)
                }else {
                    resources.getString(R.string.widget_error)
                }
            }
        }
        val iconIndex = when(state){
            is RecognitionState.Recognised -> IconViewIndex.RECOGNISED
            is RecognitionState.Recording, is RecognitionState.Recognising -> IconViewIndex.WAVEFORM
            is RecognitionState.Failed, is RecognitionState.Error, null -> IconViewIndex.NO_MUSIC
        }
        val onDemandVisibility = if(onDemandEnabled) View.VISIBLE else View.GONE
        setTextViewText(R.id.widget_text, text)
        setInt(R.id.widget_button_on_demand, "setVisibility", onDemandVisibility)
        setInt(
            R.id.widget_icon_recognised,
            "setVisibility",
            if(iconIndex == IconViewIndex.RECOGNISED) View.VISIBLE else View.GONE
        )
        setInt(
            R.id.widget_icon_no_music,
            "setVisibility",
            if(iconIndex == IconViewIndex.NO_MUSIC) View.VISIBLE else View.GONE
        )
        setInt(
            R.id.widget_icon_waveform,
            "setVisibility",
            if(iconIndex == IconViewIndex.WAVEFORM) View.VISIBLE else View.GONE
        )
        setOnClickPendingIntent(R.id.widget_button_nnfp, clickNnfpIntent)
        setOnClickPendingIntent(R.id.widget_button_on_demand, clickOnDemandIntent)
        setOnClickPendingIntent(R.id.widget_text, clickIntent)
    }

    private fun AppWidget.getRemoteViews(
        state: RecognitionState?, onDemandEnabled: Boolean
    ): RemoteViews? {
        return when(provider.className){
            AmbientMusicModWidget41::class.java.name -> {
                remoteViews41.applyState(state, onDemandEnabled, width41)
            }
            AmbientMusicModWidget42::class.java.name -> {
                remoteViews42.applyState(state, onDemandEnabled, width41)
            }
            AmbientMusicModWidgetDynamic::class.java.name -> {
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
                //Must be reconstructed as modifications unsupported
                getRemoteViewDynamic(state, onDemandEnabled)
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

    init {
        setupNnfpClicked()
        setupOnDemandClicked()
    }

    private data class AppWidget(val id: Int, val provider: ComponentName)

    enum class IconViewIndex {
        RECOGNISED, NO_MUSIC, WAVEFORM
    }

}