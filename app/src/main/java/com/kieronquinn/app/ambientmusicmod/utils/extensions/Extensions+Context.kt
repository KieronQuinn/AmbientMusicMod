package com.kieronquinn.app.ambientmusicmod.utils.extensions

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Application
import android.app.IApplicationThread
import android.app.IServiceConnection
import android.content.*
import android.content.pm.LauncherApps
import android.content.res.Configuration
import android.database.ContentObserver
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Log
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDateTime

@SuppressLint("StaticFieldLeak")
private var context: Context? = null

@SuppressLint("PrivateApi")
fun getActivityThreadContext(): Context {
    if (context == null) {
        // Fetching ActivityThread on the main thread is no longer required on API 18+
        // See: https://cs.android.com/android/platform/frameworks/base/+/66a017b63461a22842b3678c9520f803d5ddadfc
        context = Class.forName("android.app.ActivityThread")
            .getMethod("currentApplication")
            .invoke(null) as Context
    }
    return context!!
}

@SuppressLint("PrivateApi")
fun getActivityThreadApplication(): Application {
    return Class.forName("android.app.ActivityThread")
        .getMethod("currentApplication")
        .invoke(null) as Application
}

@SuppressLint("DiscouragedPrivateApi")
fun ContextWrapper.getBase(): Context {
    return ContextWrapper::class.java.getDeclaredField("mBase")
        .apply { isAccessible = true }
        .get(this) as Context
}

@SuppressLint("DiscouragedPrivateApi")
fun ContextWrapper.setBase(context: Context) {
    ContextWrapper::class.java.getDeclaredField("mBase")
        .apply { isAccessible = true }
        .set(this, context)
}

private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
private const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"

fun Context.getAccessibilityIntent(accessibilityService: Class<out AccessibilityService>): Intent {
    return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val bundle = Bundle()
        val componentName = ComponentName(packageName, accessibilityService.name).flattenToString()
        bundle.putString(EXTRA_FRAGMENT_ARG_KEY, componentName)
        putExtra(EXTRA_FRAGMENT_ARG_KEY, componentName)
        putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle)
    }
}

fun Context.broadcastReceiverAsFlow(vararg actions: String) = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(intent)
        }
    }
    actions.forEach {
        registerReceiver(receiver, IntentFilter(it))
    }
    awaitClose {
        unregisterReceiver(receiver)
    }
}

fun Context.batterySaverEnabled() = callbackFlow<Boolean> {
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(powerManager.isPowerSaveMode)
        }
    }
    trySend(powerManager.isPowerSaveMode)
    registerReceiver(receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
    awaitClose {
        unregisterReceiver(receiver)
    }
}

fun Context.getString(packageName: String, name: String, inPackage: Boolean = false): String? {
    val resources = if(inPackage){
        createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY).resources
    }else resources
    val identifier = resources.getIdentifier(name, "string", packageName)
    if(identifier == 0) return null
    return resources.getString(identifier)
}

fun Context.getServiceDispatcher(
    serviceConnection: ServiceConnection, flags: Int
): IServiceConnection {
    val mainThreadHandler = Context::class.java.getMethod("getMainThreadHandler")
        .invoke(this) as Handler
    return Context::class.java.getMethod(
        "getServiceDispatcher",
        ServiceConnection::class.java,
        Handler::class.java,
        Integer.TYPE
    ).invoke(context, serviceConnection, mainThreadHandler, flags) as IServiceConnection
}

fun Context.getApplicationThread(): IApplicationThread {
    return Context::class.java.getMethod("getIApplicationThread")
        .invoke(this) as IApplicationThread
}

fun Context.getActivityToken(): IBinder? {
    return Context::class.java.getMethod("getActivityToken").invoke(this) as? IBinder
}

fun Context.formatDateTime(localDateTime: LocalDateTime): String {
    val dateFormat = DateFormat.getDateFormat(this)
    val timeFormat = DateFormat.getTimeFormat(this)
    val date = localDateTime.toDate()
    return "${dateFormat.format(date)} ${timeFormat.format(date)}"
}

fun <T> Context.contentResolverAsTFlow(uri: Uri, block: () -> T): Flow<T> = callbackFlow {
    val observer = object: ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            trySend(block())
        }
    }
    trySend(block())
    contentResolver.safeRegisterContentObserver(uri, true, observer)
    awaitClose {
        contentResolver.unregisterContentObserver(observer)
    }
}

fun Context.contentReceiverAsFlow(uri: Uri) = contentResolverAsTFlow(uri) {}

val Context.isDarkMode: Boolean
    get() {
        return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_UNDEFINED -> false
            else -> false
        }
    }

fun Context.isLandscape(): Boolean {
    return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

fun Context.getNetworkCapabilities() = callbackFlow {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val sendCapabilities = {
        val currentCapabilities = connectivityManager.activeNetwork?.let {
            connectivityManager.getNetworkCapabilities(it)?.toNetworkCapability()
        } ?: RemoteSettingsRepository.NetworkCapability(hasInternet = false, unmetered = false)
        trySend(currentCapabilities)
    }
    val listener = object: ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            trySend(networkCapabilities.toNetworkCapability())
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            sendCapabilities()
        }

        override fun onUnavailable() {
            super.onUnavailable()
            sendCapabilities()
        }

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            sendCapabilities()
        }
    }
    connectivityManager.registerDefaultNetworkCallback(listener)
    sendCapabilities()
    awaitClose {
        connectivityManager.unregisterNetworkCallback(listener)
    }
}

fun Context.isCharging(): Flow<Boolean> = callbackFlow {
    val receiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0)
        }
    }
    registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    awaitClose {
        unregisterReceiver(receiver)
    }
}

fun Context.onPackageChanged(changingPackageName: String, sendInitial: Boolean = true) = callbackFlow {
    val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val listener = object: LauncherApps.Callback() {

        private fun notifyIfRequired(user: UserHandle?, vararg packageNames: String?) {
            if(Process.myUserHandle() == user && packageNames.contains(changingPackageName)){
                trySend(Unit)
            }
        }

        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            notifyIfRequired(user, packageName)
        }

        override fun onPackageAdded(packageName: String, user: UserHandle) {
            notifyIfRequired(user, packageName)
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            notifyIfRequired(user, packageName)
        }

        override fun onPackagesAvailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean
        ) {
            notifyIfRequired(user, *packageNames)
        }

        override fun onPackagesUnavailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean
        ) {
            notifyIfRequired(user, *packageNames)
        }

    }
    if(sendInitial) trySend(Unit)
    launcherApps.registerCallback(listener)
    awaitClose {
        launcherApps.unregisterCallback(listener)
    }
}.flowOn(Dispatchers.Main)

private fun NetworkCapabilities.toNetworkCapability(): RemoteSettingsRepository.NetworkCapability {
    val hasInternet = hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    val isUnmetered = hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    return RemoteSettingsRepository.NetworkCapability(hasInternet, isUnmetered)
}

// From https://stackoverflow.com/a/55280832
@ColorInt
fun Context.getColorResCompat(@AttrRes id: Int): Int {
    val resolvedAttr = TypedValue()
    this.theme.resolveAttribute(id, resolvedAttr, true)
    val colorRes = resolvedAttr.run { if (resourceId != 0) resourceId else data }
    return ContextCompat.getColor(this, colorRes)
}

private const val COMPONENT_GSA_ON_DEMAND =
    "com.google.android.googlequicksearchbox/com.google.android.apps.search.soundsearch.service.SoundSearchService"

fun Context.isOnDemandConfigValueSet(): Boolean {
    return getString(
        "android",
        "config_defaultMusicRecognitionService"
    ) == COMPONENT_GSA_ON_DEMAND
}