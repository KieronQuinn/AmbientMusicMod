package com.kieronquinn.app.ambientmusicmod.repositories

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import com.kieronquinn.app.ambientmusicmod.PACKAGE_NAME_PAM
import com.kieronquinn.app.pixelambientmusic.IRecognitionService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface AmbientServiceRepository {

    suspend fun getService(): IRecognitionService?

}

class AmbientServiceRepositoryImpl(
    private val context: Context
): AmbientServiceRepository {

    private var service: IRecognitionService? = null
    private var serviceConnection: ServiceConnection? = null
    private val serviceLock = Mutex()

    private val serviceIntent by lazy {
        Intent("com.kieronquinn.app.pixelambientmusic.RECOGNITION_SERVICE").apply {
            `package` = PACKAGE_NAME_PAM
        }
    }

    override suspend fun getService() = serviceLock.withLock {
        if(!ApiRepository.assertCompatibility()) return@withLock null
        service?.let {
            if(!it.safePing()) return@let
            return@withLock it
        }
        suspendCoroutine {
            var hasResumed = false
            val serviceConnection = object: ServiceConnection {
                override fun onServiceConnected(component: ComponentName, binder: IBinder) {
                    serviceConnection = this
                    val service = IRecognitionService.Stub.asInterface(binder)
                    this@AmbientServiceRepositoryImpl.service = service
                    if(!hasResumed) {
                        hasResumed = true
                        it.resume(service)
                    }
                }

                override fun onServiceDisconnected(component: ComponentName) {
                    serviceConnection = null
                    service = null
                }
            }
            context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun IRecognitionService.safePing(): Boolean {
        return try {
            ping()
        }catch (e: RemoteException){
            false
        }
    }

}