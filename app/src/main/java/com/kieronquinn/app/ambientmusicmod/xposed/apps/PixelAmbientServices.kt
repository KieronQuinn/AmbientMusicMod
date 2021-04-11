package com.kieronquinn.app.ambientmusicmod.xposed.apps

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AndroidAppHelper
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioRecord
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.documentfile.provider.DocumentFile
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.app.ui.activities.AmbientActivity
import com.kieronquinn.app.ambientmusicmod.components.AmbientSharedPreferences
import com.kieronquinn.app.ambientmusicmod.components.AmbientSharedPreferences.Companion.DEFAULT_ENABLED
import com.kieronquinn.app.ambientmusicmod.components.AmbientSharedPreferences.Companion.DEFAULT_JOB_TIME
import com.kieronquinn.app.ambientmusicmod.components.AmbientSharedPreferences.Companion.DEFAULT_RECORD_GAIN
import com.kieronquinn.app.ambientmusicmod.components.AmbientSharedPreferences.Companion.DEFAULT_RUN_WHEN_WOKEN
import com.kieronquinn.app.ambientmusicmod.components.AmbientSharedPreferences.Companion.DEFAULT_SHOW_ALBUM_ART
import com.kieronquinn.app.ambientmusicmod.components.superpacks.Superpacks
import com.kieronquinn.app.ambientmusicmod.xposed.XposedAppHook
import com.kieronquinn.app.ambientmusicmod.constants.AMBIENT_MUSIC_MODEL_UUID
import com.kieronquinn.app.ambientmusicmod.model.preference.Preference
import com.kieronquinn.app.ambientmusicmod.model.preference.PreferenceScreen
import com.kieronquinn.app.ambientmusicmod.model.proto.HistoryEntryProto
import com.kieronquinn.app.ambientmusicmod.model.recognition.RecognitionResult
import com.kieronquinn.app.ambientmusicmod.xposed.phenotype.PhenotypeOverrides
import com.kieronquinn.app.ambientmusicmod.utils.*
import com.kieronquinn.app.ambientmusicmod.utils.extensions.*
import com.kieronquinn.app.ambientmusicmod.utils.picasso.RoundedCornersTransform
import com.kieronquinn.app.ambientmusicmod.xposed.debug.XLog
import com.kieronquinn.app.ambientmusicmod.xposed.wrappers.SoundTriggerDetectionService
import com.kieronquinn.app.ambientmusicmod.xposed.wrappers.SoundTriggerManager
import com.squareup.picasso.Picasso
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.*
import java.io.File

@SuppressLint("WrongConstant")
class PixelAmbientServices: XposedAppHook() {

    override val packageName = PIXEL_AMBIENT_SERVICES_PACKAGE_NAME
    override val appName = "Pixel Ambient Services"

    companion object {
        const val INTENT_ACTION_GET_MODEL_STATE = "com.google.intelligence.sense.ACTION_GET_MODEL_STATE"
        const val INTENT_ACTION_GET_MODEL_STATE_MANUAL = "com.google.intelligence.sense.ACTION_GET_MODEL_STATE_MANUAL"
        const val INTENT_ACTION_RECOGNITION_RESULT = "com.google.intelligence.sense.RECOGNITION_RESULT"
        const val INTENT_ACTION_RECOGNITION_STARTED = "com.google.intelligence.sense.RECOGNITION_STARTED"
        const val INTENT_ACTION_SEND_SUPERPACKS = "com.google.intelligence.sense.SEND_SUPERPACKS"
        const val INTENT_ACTION_SEND_SUPERPACKS_START = "com.google.intelligence.sense.SEND_SUPERPACKS_START"
        const val INTENT_ACTION_SEND_SUPERPACKS_COMPLETE = "com.google.intelligence.sense.SEND_SUPERPACKS_COMPLETE"
        const val INTENT_ACTION_SEND_SUPERPACKS_CANCEL = "com.google.intelligence.sense.SEND_SUPERPACKS_CANCEL"
        const val INTENT_ACTION_REQUEST_SUPERPACKS_VERSION = "com.google.intelligence.sense.REQUEST_SUPERPACKS_VERSION"
        const val INTENT_ACTION_RESPONSE_SUPERPACKS_VERSION = "com.google.intelligence.sense.RESPONSE_SUPERPACKS_VERSION"
        const val INTENT_RECOGNITION_RESULT_EXTRA_RESULT = "result"
        const val INTENT_RESPONSE_SUPERPACKS_VERSION_EXTRA_VERSION = "version"
        const val INTENT_GET_MODEL_STATE_EXTRA_OUTPUT_URI = "outputUri"
        const val INTENT_SEND_SUPERPACKS_EXTRA_OUTPUT_URI = "outputUri"
        const val PIXEL_AMBIENT_SERVICES_PACKAGE_NAME = "com.google.intelligence.sense"
        const val GOOGLE_SEARCH_PACKAGE_NAME = "com.google.android.googlequicksearchbox"

        val YOUTUBE_MUSIC_URL_PATTERN = "https://music.youtube.com/watch\\?v=(.*)&feature=gws_kp_track".toRegex()

        val standardSettingsIntent by lazy {
            Intent("com.google.intelligence.sense.NOW_PLAYING_SETTINGS").apply {
                `package` = PIXEL_AMBIENT_SERVICES_PACKAGE_NAME
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        val nowPlayingHistoryIntent by lazy {
            Intent("com.google.intelligence.sense.NOW_PLAYING_HISTORY").apply {
                `package` = PIXEL_AMBIENT_SERVICES_PACKAGE_NAME
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    //Settings
    private var enabled = DEFAULT_ENABLED
    private var recordGain = DEFAULT_RECORD_GAIN
    private var runWhenWoken = DEFAULT_RUN_WHEN_WOKEN
    private var jobTime = DEFAULT_JOB_TIME
    private var showAlbumArt = DEFAULT_SHOW_ALBUM_ART
    private var isManualTrigger = false
    private var outputUri: Uri? = null

    //Sound Trigger instance
    private lateinit var soundTriggerManager: SoundTriggerManager

    //Superpacks send job for cancelling if required
    private var superpacksSendJob: Job? = null

    private val modelTriggerReceiver = SecureBroadcastReceiver { context, intent ->
        if(!enabled){
            XLog.d("Rejecting model trigger broadcast as enabled=$enabled")
            return@SecureBroadcastReceiver
        }
        XLog.d("Received model trigger broadcast")

        //Don't re-run during a manual trigger, but reset it so next time it can run if we're in a broken state
        if(isManualTrigger){
            isManualTrigger = false
            return@SecureBroadcastReceiver
        }

        getSoundTriggerService()?.getModelState(AMBIENT_MUSIC_MODEL_UUID)
    }

    private val modelTriggerManualReceiver = SecureBroadcastReceiver { context, intent ->
        if(!enabled){
            XLog.d("Rejecting manual trigger broadcast as enabled=$enabled")
            return@SecureBroadcastReceiver
        }
        XLog.d("Received model trigger manual broadcast")
        isManualTrigger = true
        intent?.getParcelableExtra<Uri>(INTENT_GET_MODEL_STATE_EXTRA_OUTPUT_URI)?.let {
            outputUri = it
        }

        getSoundTriggerService()?.getModelState(AMBIENT_MUSIC_MODEL_UUID)
    }

    private val settingsChangedReceiver = SecureBroadcastReceiver { _, _ ->
        XLog.d("Received settings changed broadcast")
        loadSettings()
        phenotypeOverrides.refreshDynamicPhenotypeOverrides()
    }

    private val sendSuperpacksBroadcastReceiver = SecureBroadcastReceiver { _, intent ->
        val outputUri = intent?.getParcelableExtra<Uri>(INTENT_GET_MODEL_STATE_EXTRA_OUTPUT_URI) ?: return@SecureBroadcastReceiver
        sendSuperpacks(outputUri)
    }

    private val sendSuperpacksCancelBroadcastReceiver = SecureBroadcastReceiver { _, _ ->
        sendSuperpacksCancel()
    }

    private val requestSuperpacksVersion = SecureBroadcastReceiver { context, _ ->
        sendSuperpacksVersion(context)
    }

    private fun getSoundTriggerService(classLoader: ClassLoader? = null): SoundTriggerManager? {
        return when {
            this::soundTriggerManager.isInitialized -> {
                return soundTriggerManager
            }
            classLoader != null -> {
                soundTriggerManager = SoundTriggerManager(context.getSystemService("soundtrigger"), classLoader)
                soundTriggerManager
            }
            else -> null
        }
    }

    private val phenotypeOverrides by lazy {
        PhenotypeOverrides(sharedPrefs)
    }

    /**
     * Called when the app is initially hooked, calls down to separated methods for different types of hooks
     */
    override fun onAppHooked(lpparam: XC_LoadPackage.LoadPackageParam) {
        XLog.d("Ambient Hooked!")
        hookAudioRecord()
        hookPermissionFailures(lpparam)
        hookApplication(lpparam)
        hookPhenotype(lpparam)
        hookDetectionService(lpparam)
        hookModelResponse(lpparam)
        hookBroadcastReceiver(lpparam)
        hookIndicationBroadcast(lpparam)
        hookEnabledSetting(lpparam)
        hookAskGoogle(lpparam)
        hookAmbientSettings(lpparam)
        hookLevelDb(lpparam)
        hookAlbumArt(lpparam)
    }

    /**
     *  XPOSED HOOK DEFINITIONS FOR Pixel Ambient Services
     */

    /**
     *  Hooks the main Application class for Pixel Ambient Services to register broadcasts to ping (run) the model, update settings & more
     */
    private fun hookApplication(lpparam: XC_LoadPackage.LoadPackageParam){
        val applicationClass = "com.google.intelligence.sense.SenseApplication"
        XposedHelpers.findAndHookMethod(applicationClass, lpparam.classLoader, "onCreate", MethodHook(afterHookedMethod = {
            val context = it.thisObject as Context
            Picasso.setSingletonInstance(Picasso.Builder(context).build())
            //Logging
            XLog.attach(context)
            getSoundTriggerService(lpparam.classLoader)
            setupBroadcastReceivers(context)
            //Load initial settings
            loadSettings()
            phenotypeOverrides.refreshDynamicPhenotypeOverrides()
        }))
    }

    /**
     *  Hooks the "Phenotypes" (Google server controlled flags) to override some to enable services & tweak model sensitivity
     */
    @ObfuscatedNames("search for <Must call PhenotypeFlag.init() first>")
    private fun hookPhenotype(lpparam: XC_LoadPackage.LoadPackageParam){
        val phenotypeClass = "cuz"
        XposedHelpers.findAndHookMethod(phenotypeClass, lpparam.classLoader, "c", MethodHook(beforeHookedMethod = {
            val key = XposedHelpers.getObjectField(it.thisObject, "e") as String
            XLog.d("Get $key")
            phenotypeOverrides.getOverridenPhenotype(key)?.let {  overridenValue ->
                XLog.d("Overriding $key to $overridenValue")
                it.result = overridenValue
            }
        }))
    }

    /**
     *  Hooks the AudioRecord's read(short[], int, int) method to apply a gain to the audio input if required
     */
    private fun hookAudioRecord() {
        XposedHelpers.findAndHookMethod(AudioRecord::class.java, "read", ShortArray::class.java, Integer.TYPE, Integer.TYPE, MethodHook(afterHookedMethod = {
            val shortArray = it.args[0] as ShortArray
            val gainedShort = if(recordGain != 0f) AudioUtils.applyGain(shortArray, recordGain) else shortArray
            it.args[0] = gainedShort
            val result = it.result
            XLog.d("Read $result short array size ${shortArray.size}")
            val currentOutputUri = outputUri
            if(isManualTrigger && currentOutputUri != null) {
                writeAudioRecordToUri(gainedShort, currentOutputUri)
            }
        }))
    }

    /**
     *  Hooks and replaces a call to getCurrentUser to 0. Despite the app having the priv-app permission INTERACT_ACROSS_USERS, this was failing for some reason
     *  TODO figure out why this is failing & fix it or find a workaround to get the current user as this will break multi-user options
     */
    private fun hookPermissionFailures(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod(ActivityManager::class.java, "getCurrentUser", MethodReplacement {
            it.result = 0
            0
        })
        XposedHelpers.findAndHookMethod("android.app.ContextImpl", lpparam.classLoader, "checkSelfPermission", String::class.java, MethodHook {
            XLog.d("Hooked checkSelfPermission with permission ${it.args[0]}")
            if(it.args[0] == "com.google.android.ambientindication.permission.AMBIENT_INDICATION"){
                it.result = 0
            }
        })
    }

    /**
     *  Hooks the getting of the model response to allow interception and sending of it on to the settings if required
     */
    @ObfuscatedNames("search for <Null recognitionResult>, method returning this.a")
    private fun hookModelResponse(lpparam: XC_LoadPackage.LoadPackageParam){
        XposedHelpers.findAndHookMethod("eom", lpparam.classLoader, "a",  MethodHook(afterHookedMethod = {
            if(isManualTrigger || BuildConfig.DEBUG) {
                val response = it.result
                @ObfuscatedNames("first method returning a long")
                val recognitionDelay = XposedHelpers.callMethod(it.thisObject, "d") as Long
                //Map to a more readable structure
                val recognitionResult = RecognitionResult.fromObfuscated(response).apply {
                    retryTime = recognitionDelay
                }
                XLog.d("$recognitionResult with recognitionDelay $recognitionDelay")
                sendRecognitionBroadcast(recognitionResult)
                //Reset the manual trigger state
                isManualTrigger = false
            }
        }))
    }

    private fun hookDetectionService(lpparam: XC_LoadPackage.LoadPackageParam){
        XposedHelpers.findAndHookMethod(SoundTriggerDetectionService.getClass(lpparam.classLoader), "onBind", Intent::class.java, MethodHook {
            if(isManualTrigger) {
                sendRecognitionStartedBroadcast()
            }
        })
    }

    private fun hookBroadcastReceiver(lpparam: XC_LoadPackage.LoadPackageParam){
        @ObfuscatedNames("search for <Received %s, fetching model state.>")
        XposedHelpers.findAndHookMethod("eik", lpparam.classLoader, "onReceive", Context::class.java, Intent::class.java, MethodReplacement {
            if(!enabled) {
                XLog.d("Rejecting wake model trigger broadcast as enabled=$enabled")
                return@MethodReplacement null
            }
            val intent = it.args[1] as Intent
            XLog.d("Received model trigger broadcast of action ${intent.action} runWhenWoken=$runWhenWoken")
            if(intent.action == Intent.ACTION_SCREEN_ON && !runWhenWoken){
                //Wake up action, disable if the user has chosen to
                XLog.d("Run when woken disabled, returning")
                return@MethodReplacement null
            }else{
                XLog.d("Run when woken enabled, continuing")
                @ObfuscatedNames("only field")
                val a = XposedHelpers.getObjectField(it.thisObject,"a")
                @ObfuscatedNames("only method call")
                XposedHelpers.callMethod(a, "f")
            }
        })
    }

    /**
     *  Replaces the Ambient Indication broadcasts which require the "com.google.android.ambientindication.permission.AMBIENT_INDICATION"
     *  permission with secure broadcasts that do not.
     *
     *  Note to other mod developers: If you intend to show Now Playing songs in SystemUI, it is *your responsibility* to check that
     *  the broadcast has come from Pixel Ambient Services. Check how SecureBroadcastReceiver works for an implementation of that.
     */
    private fun hookIndicationBroadcast(lpparam: XC_LoadPackage.LoadPackageParam){
        @ObfuscatedNames("search for <com.android.systemui>")
        val indicationClass = XposedHelpers.findClass("azb", lpparam.classLoader)
        @ObfuscatedNames("method with intent argument")
        XposedHelpers.findAndHookMethod(indicationClass, "a", Intent::class.java, MethodReplacement {
            val context = XposedHelpers.getObjectField(it.thisObject, "b") as Context
            val intent = it.args[0] as Intent
            val packageNamesList = (XposedHelpers.getStaticObjectField(indicationClass, "a") as Array<String>).toMutableList().apply {
                if(!this.contains(BuildConfig.APPLICATION_ID)) add(BuildConfig.APPLICATION_ID)
            }
            packageNamesList.forEach { pName ->
                intent.`package` = pName
                context.sendSecureBroadcast(intent)
            }
            null
        })
    }

    private fun hookEnabledSetting(lpparam: XC_LoadPackage.LoadPackageParam){
        @ObfuscatedNames("search for class using R.string <ambient_music_aod_notification_switch>")
        XposedHelpers.findAndHookMethod("ehq", lpparam.classLoader, "b", MethodReplacement {
            XLog.d("Setting check called, returning $enabled")
            it.result = enabled
            enabled
        })
    }

    private fun hookAmbientSettings(lpparam: XC_LoadPackage.LoadPackageParam){
        val preferenceScreenClass = XposedHelpers.findClass("androidx.preference.PreferenceScreen", lpparam.classLoader)
        var modAppLinkTitle: String? = null
        @ObfuscatedNames("search for use of <pref_ambient_music>")
        XposedHelpers.findAndHookMethod("za", lpparam.classLoader, "a", Context::class.java, preferenceScreenClass, MethodHook(afterHookedMethod = {
            val moduleContext = it.args[0] as Context
            val isDarkTheme = moduleContext.isDarkTheme()
            val modAppContext = moduleContext.createPackageContext(BuildConfig.APPLICATION_ID, Context.CONTEXT_IGNORE_SECURITY)
            modAppLinkTitle = modAppContext.getString(R.string.mod_app_link_title)
            val modAppLinkContent = modAppContext.getString(R.string.mod_app_link_content)
            val modAppLinkIcon = ContextCompat.getDrawable(modAppContext, if(isDarkTheme) R.drawable.ic_music_dark else R.drawable.ic_music_light)
            val preferenceScreen = PreferenceScreen(it.result, lpparam.classLoader)
            val obfuscatedSwitchPref = preferenceScreen.findPreference("ambient_music_aod_notification_switch")
            val mainSwitchPref = Preference(obfuscatedSwitchPref, lpparam.classLoader, context)
            mainSwitchPref.setVisible(false)
            val obfuscatedPreference = XposedHelpers.findClass("androidx.preference.Preference", lpparam.classLoader).getConstructor(Context::class.java).newInstance(context)
            val linkPreference = Preference(obfuscatedPreference, lpparam.classLoader, context).apply {
                setTitle(modAppLinkTitle ?: "")
                setSummary(modAppLinkContent)
                if(modAppLinkIcon != null) setIcon(modAppLinkIcon)
            }
            preferenceScreen.addPreference(1, linkPreference)
        }))
        @ObfuscatedNames("search <android.settings.APP_NOTIFICATION_SETTINGS>, find call of the overridden method")
        XposedHelpers.findAndHookMethod("androidx.preference.Preference", lpparam.classLoader, "l", MethodHook {
            @ObfuscatedNames("only context field in Preference")
            val context = XposedHelpers.getObjectField(it.thisObject, "j") as Context
            val preference = Preference(it.thisObject, lpparam.classLoader, context)
            if(preference.getTitle() == modAppLinkTitle){
                launchModApp(context)
            }
        })
    }

    /**
     *  Hooks calls to broken leveldb implementation for album art in Now Playing History and loads compatible thumbs from YouTube Music instead
     */
    private fun hookAlbumArt(lpparam: XC_LoadPackage.LoadPackageParam){
        @ObfuscatedNames("find method below, first param type")
        val elz = XposedHelpers.findClass("elz", lpparam.classLoader)
        @ObfuscatedNames("search <SongGroupViewHolder>, method with param types Context, <above object>, View")
        XposedHelpers.findAndHookMethod("ekr", lpparam.classLoader, "a", Context::class.java, elz, View::class.java, MethodHook(afterHookedMethod = {
            if(!showAlbumArt) return@MethodHook
            @ObfuscatedNames("only long field in found class above")
            val timestamp = XposedHelpers.callMethod(it.args[1], "a") as Long
            val view = it.args[2] as View
            val imageView = (view.parent as LinearLayout).children.first { it is ImageView }
            setAlbumArtImage(context, imageView as ImageView, timestamp)
        }))

        @ObfuscatedNames("search <SongActionsDialogFragment>, method with param types LayoutInflater, ViewGroup, Bundle")
        XposedHelpers.findAndHookMethod("ekh", lpparam.classLoader,"a", LayoutInflater::class.java, ViewGroup::class.java, Bundle::class.java, MethodHook(afterHookedMethod = {
            if(!showAlbumArt) return@MethodHook
            @ObfuscatedNames("field referenced within method to get timestamp / data")
            val songContainer = XposedHelpers.getObjectField(it.thisObject, "ad")
            @ObfuscatedNames("only long field in found class above")
            val timestamp = XposedHelpers.callMethod(songContainer, "a") as Long
            val rootView = it.result as View
            val imageId = rootView.context.resources.getIdentifier("song_actions_album_art", "id", PIXEL_AMBIENT_SERVICES_PACKAGE_NAME)
            val imageView = rootView.findViewById<ImageView>(imageId)
            setAlbumArtImage(imageView.context, imageView, timestamp)
        }))
    }

    /**
     *  Force enables the 'Ask Google' option in History and allows the (optional) notification launch Assistant option
     *  Only disables if Google app isn't installed
     */
    private fun hookAskGoogle(lpparam: XC_LoadPackage.LoadPackageParam){
        @ObfuscatedNames("search <android.soundsearch.extra.RECOGNIZED_TRACK_MID>, method referencing googlequicksearchbox")
        XposedHelpers.findAndHookMethod("ehw", lpparam.classLoader, "a", PackageManager::class.java, MethodReplacement {
            val packageManager = it.args[0] as PackageManager
            val isInstalled = packageManager.isAppInstalled(GOOGLE_SEARCH_PACKAGE_NAME)
            it.result = isInstalled
            isInstalled
        })
    }

    private fun hookLevelDb(lpparam: XC_LoadPackage.LoadPackageParam){
        XposedHelpers.findAndHookConstructor("com.google.intelligence.sense.leveldb.LevelDbTable", lpparam.classLoader, Long::class.java, MethodHook {
            Log.d("XAL", "LevelDB init ${it.args[0]}")
        })
        XposedHelpers.findAndHookMethod("com.google.intelligence.sense.leveldb.LevelDbTable", lpparam.classLoader, "a", String::class.java, MethodHook {
            Log.d("XAL", "LevelDB open ${it.args[0]}")
        })
        XposedHelpers.findAndHookMethod("com.google.intelligence.sense.leveldb.LevelDbTable", lpparam.classLoader, "a", ByteArray::class.java, MethodHook {
            val bytes = it.args[0] as ByteArray
            Log.d("XAL", "LevelDB get ${Base64.encodeToString(bytes, Base64.DEFAULT)}")
        })

    }

    /**
     *  END XPOSED HOOKS
     */

    private fun launchModApp(context: Context){
        Intent().apply {
            component = ComponentName(BuildConfig.APPLICATION_ID, AmbientActivity::class.java.name)
            `package` = BuildConfig.APPLICATION_ID
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }.also {
            context.startActivity(it)
        }
    }

    private fun setAlbumArtImage(context: Context, imageView: ImageView, timestamp: Long) = GlobalScope.launch {
        val placeholderGet = {
            val placeholderRes = imageView.context.resources.getIdentifier("album_art_placeholder", "drawable", PIXEL_AMBIENT_SERVICES_PACKAGE_NAME)
            ContextCompat.getDrawable(context, placeholderRes)
        }
        val sqliteFile = imageView.context.getDatabasePath("history_db")
        val database = SQLiteDatabase.openDatabase(sqliteFile.absolutePath, null, 0)
        val query = database.rawQuery("SELECT history_entry FROM recognition_history WHERE timestamp=$timestamp", null)
        query.moveToFirst()
        if(query.isAfterLast){
            query.close()
            imageView.setImageDrawable(placeholderGet.invoke())
            imageView.tag = null
            return@launch
        }
        val historyBlob = query.getBlob(0)
        val historyEntry = HistoryEntryProto.HistoryEntry.parseFrom(historyBlob)
        val streamingOptions = historyEntry.historyTrack.streamingOptionList
        val youtubeOption =
            streamingOptions.firstOrNull { option -> option.url.startsWith("https://music.youtube.com") }
        var youtubeId: String? = null
        youtubeOption?.url?.let { url ->
            YOUTUBE_MUSIC_URL_PATTERN.find(url)?.groupValues?.get(1)?.let { id ->
                youtubeId = id
            }
        } ?: run {
            query.close()
            imageView.setImageDrawable(placeholderGet.invoke())
            imageView.tag = null
            return@launch
        }
        withContext(Dispatchers.Main) {
            if(youtubeId != null && imageView.tag != youtubeId) {
                Picasso.get().load("https://img.youtube.com/vi/$youtubeId/maxresdefault.jpg")
                    .centerCrop()
                    .fit()
                    .error(placeholderGet.invoke() ?: ColorDrawable(Color.TRANSPARENT))
                    .placeholder(placeholderGet.invoke() ?: ColorDrawable(Color.TRANSPARENT))
                    .transform(RoundedCornersTransform())
                    .into(imageView)
                imageView.tag = youtubeId
            }else{
                imageView.setImageDrawable(placeholderGet.invoke())
                imageView.tag = null
            }
        }
        query.close()
    }

    private fun setupBroadcastReceivers(context: Context) = with(context) {
        XLog.d("Registering receivers")
        registerReceiver(settingsChangedReceiver, IntentFilter(AmbientSharedPreferences.INTENT_ACTION_SETTINGS_CHANGED))
        registerReceiver(modelTriggerReceiver, IntentFilter(INTENT_ACTION_GET_MODEL_STATE))
        registerReceiver(modelTriggerManualReceiver, IntentFilter(INTENT_ACTION_GET_MODEL_STATE_MANUAL))
        registerReceiver(sendSuperpacksBroadcastReceiver, IntentFilter(INTENT_ACTION_SEND_SUPERPACKS))
        registerReceiver(sendSuperpacksCancelBroadcastReceiver, IntentFilter(INTENT_ACTION_SEND_SUPERPACKS_CANCEL))
        registerReceiver(requestSuperpacksVersion, IntentFilter(INTENT_ACTION_REQUEST_SUPERPACKS_VERSION))
    }

    private fun loadSettings() {
        enabled = sharedPrefs.enabled
        recordGain = sharedPrefs.recordGain
        runWhenWoken = sharedPrefs.runWhenWoken
        jobTime = sharedPrefs.jobTime
        showAlbumArt = sharedPrefs.showAlbumArt
        XLog.d("Settings: enabled $enabled recordGain $recordGain runWhenWoken $runWhenWoken jobTime $jobTime")
    }

    private fun sendRecognitionBroadcast(recognitionResult: RecognitionResult) {
        Intent(INTENT_ACTION_RECOGNITION_RESULT).apply {
            `package` = BuildConfig.APPLICATION_ID
        }.apply {
            putExtra(INTENT_RECOGNITION_RESULT_EXTRA_RESULT, recognitionResult)
        }.also {
            context.sendSecureBroadcast(it)
        }
    }

    private fun sendRecognitionStartedBroadcast() {
        XLog.d("Sending started broadcast")
        Intent(INTENT_ACTION_RECOGNITION_STARTED).apply {
            `package` = BuildConfig.APPLICATION_ID
        }.also {
            context.sendSecureBroadcast(it)
        }
    }

    private fun sendSuperpacks(outputUri: Uri){
        val context = AndroidAppHelper.currentApplication() as Context
        superpacksSendJob?.cancel()
        superpacksSendJob = GlobalScope.launch {
            withContext(Dispatchers.IO){
                val contentResolver = context.contentResolver
                val outputStream = contentResolver.openOutputStream(outputUri) ?: return@withContext
                context.sendSecureBroadcast(Intent(INTENT_ACTION_SEND_SUPERPACKS_START))
                val superpacksDirectory = File(context.filesDir, "superpacks")
                if(superpacksDirectory.exists()) {
                    superpacksDirectory.zipDirectory(outputStream, 0)
                }
                outputStream.close()
                context.sendSecureBroadcast(Intent(INTENT_ACTION_SEND_SUPERPACKS_COMPLETE))
            }
        }
    }

    private fun sendSuperpacksCancel(){
        superpacksSendJob?.cancel()
    }

    private fun sendSuperpacksVersion(context: Context){
        val version = Superpacks.getSuperpackVersion(context, Superpacks.SUPERPACK_AMBIENT_MUSIC_INDEX)
        Log.d("XASuperpacks", "sendSuperpacksVersion $version")
        context.sendSecureBroadcast(Intent(INTENT_ACTION_RESPONSE_SUPERPACKS_VERSION).apply {
            putExtra(INTENT_RESPONSE_SUPERPACKS_VERSION_EXTRA_VERSION, version)
        })
    }

}