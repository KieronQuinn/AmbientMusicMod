package com.kieronquinn.app.ambientmusicmod.repositories

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import com.kieronquinn.app.ambientmusicmod.BuildConfig
import com.kieronquinn.app.ambientmusicmod.IShellProxy
import com.kieronquinn.app.ambientmusicmod.repositories.ShizukuServiceRepository.ShizukuServiceResponse
import com.kieronquinn.app.ambientmusicmod.repositories.ShizukuServiceRepository.ShizukuServiceResponse.FailureReason
import com.kieronquinn.app.ambientmusicmod.service.ShizukuService
import com.kieronquinn.app.ambientmusicmod.utils.extensions.suspendCancellableCoroutineWithTimeout
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface ShizukuServiceRepository {

    sealed class ShizukuServiceResponse<T> {
        data class Success<T>(val result: T): ShizukuServiceResponse<T>()
        data class Failed<T>(val reason: FailureReason): ShizukuServiceResponse<T>()

        enum class FailureReason {
            /**
             *  Shizuku is not bound, likely the user has not started it since rebooting
             */
            NO_BINDER,

            /**
             *  Permission to access Shizuku has not been granted
             */
            PERMISSION_DENIED,

            /**
             *  The service is not immediately available (only used in [runWithServiceIfAvailable])
             */
            NOT_AVAILABLE
        }

        /**
         *  Unwraps a result into either its value or null if it failed
         */
        fun unwrap(): T? {
            return (this as? Success)?.result
        }
    }

    val isReady: Flow<Boolean>
    suspend fun assertReady(): Boolean
    suspend fun <T> runWithService(block: (IShellProxy) -> T): ShizukuServiceResponse<T>
    fun <T> runWithServiceIfAvailable(block: (IShellProxy) -> T): ShizukuServiceResponse<T>
    fun disconnect()

}

class ShizukuServiceRepositoryImpl(context: Context): ShizukuServiceRepository {

    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
        private const val SHIZUKU_TIMEOUT = 2500L
    }

    private val shizukuComponent by lazy {
        ComponentName(context, ShizukuService::class.java)
    }

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(shizukuComponent).apply {
            daemon(false)
            debuggable(BuildConfig.DEBUG)
            version(BuildConfig.VERSION_CODE)
            processNameSuffix("shizuku")
        }
    }

    private var serviceConnection: ServiceConnection? = null
    private var service: IShellProxy? = null
    private val serviceLock = Mutex()
    private val runLock = Mutex()
    private val scope = MainScope()

    private val onConnectionChange = MutableStateFlow(System.currentTimeMillis())

    override val isReady = onConnectionChange.map {
        assertReady()
    }

    override suspend fun assertReady(): Boolean {
        val result = runWithService {
            it.ping()
        }
        return result is ShizukuServiceResponse.Success
    }

    override suspend fun <T> runWithService(
        block: (IShellProxy) -> T
    ): ShizukuServiceResponse<T> = runLock.withLock {
        service?.let {
            if(!it.safePing()){
                //Service has disconnected or died
                service = null
                serviceConnection = null
                return@let
            }
            return ShizukuServiceResponse.Success(block(it))
        }
        if(awaitShizuku() != true)
            return ShizukuServiceResponse.Failed(FailureReason.NO_BINDER)
        if(!requestPermission())
            return ShizukuServiceResponse.Failed(FailureReason.PERMISSION_DENIED)
        return ShizukuServiceResponse.Success(block(getService()))
    }

    override fun <T> runWithServiceIfAvailable(
        block: (IShellProxy) -> T
    ): ShizukuServiceResponse<T> {
        return service?.let {
            ShizukuServiceResponse.Success(block(it))
        } ?: ShizukuServiceResponse.Failed(FailureReason.NOT_AVAILABLE)
    }

    override fun disconnect() {
        serviceConnection?.let {
            Shizuku.unbindUserService(userServiceArgs, it, true)
        }
    }

    private suspend fun awaitShizuku() = suspendCancellableCoroutineWithTimeout<Boolean>(SHIZUKU_TIMEOUT) {
        var hasResumed = false
        if(Shizuku.pingBinder()) {
            if(!hasResumed) {
                hasResumed = true
                it.resume(true) //Already connected
            }
            return@suspendCancellableCoroutineWithTimeout
        }
        val listener = object: Shizuku.OnBinderReceivedListener {
            override fun onBinderReceived() {
                Shizuku.removeBinderReceivedListener(this)
                if(!hasResumed) {
                    hasResumed = true
                    it.resume(true)
                }
            }
        }
        Shizuku.addBinderReceivedListener(listener)
        it.invokeOnCancellation {
            Shizuku.removeBinderReceivedListener(listener)
        }
    }

    private suspend fun requestPermission() = suspendCancellableCoroutine<Boolean> {
        var hasResumed = false
        if(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            if(!hasResumed) {
                hasResumed = true
                it.resume(true) //Already granted
            }
            return@suspendCancellableCoroutine
        }
        val listener = object: Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if(requestCode != SHIZUKU_PERMISSION_REQUEST_CODE) return
                Shizuku.removeRequestPermissionResultListener(this)
                if(!hasResumed) {
                    hasResumed = true
                    it.resume(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
        it.invokeOnCancellation {
            Shizuku.removeRequestPermissionResultListener(listener)
        }
    }

    private suspend fun getService() = serviceLock.withLock {
        suspendCoroutine<IShellProxy> {
            var hasResumed = false
            val serviceConnection = object: ServiceConnection {
                override fun onServiceConnected(component: ComponentName, binder: IBinder) {
                    serviceConnection = this
                    val service = IShellProxy.Stub.asInterface(binder)
                    this@ShizukuServiceRepositoryImpl.service = service
                    scope.launch {
                        onConnectionChange.emit(System.currentTimeMillis())
                    }
                    if(!hasResumed){
                        hasResumed = true
                        it.resume(service)
                    }
                }

                override fun onServiceDisconnected(component: ComponentName) {
                    serviceConnection = null
                    service = null
                    scope.launch {
                        onConnectionChange.emit(System.currentTimeMillis())
                    }
                }
            }
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        }
    }

    private fun IShellProxy.safePing(): Boolean {
        return try {
            ping()
        }catch (e: RemoteException){
            false
        }
    }

}