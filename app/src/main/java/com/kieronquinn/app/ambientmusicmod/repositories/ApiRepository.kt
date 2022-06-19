package com.kieronquinn.app.ambientmusicmod.repositories

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.kieronquinn.app.ambientmusicmod.PACKAGE_NAME_PAM
import com.kieronquinn.app.ambientmusicmod.repositories.ApiRepository.Companion.API_VERSION_TAG
import com.kieronquinn.app.ambientmusicmod.repositories.ApiRepository.Companion.COMPATIBLE_APIS
import com.kieronquinn.app.ambientmusicmod.utils.extensions.onPackageChanged
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ApiRepository {

    companion object : KoinComponent {

        /**
         *  Supported remote APIs for AMM <> PAM communication. This should always be the current
         *  version + 1, and each version update should maintain backwards compatibility with at
         *  least one previous version. This allows the user to have the chance to update without
         *  seeing any errors, but allows future versions to not have to support too old an APK.
         */
        internal val COMPATIBLE_APIS = arrayOf(1, 2)

        internal const val API_VERSION_TAG = "com.kieronquinn.app.ambientmusicmod.API_VERSION"

        fun assertCompatibility(): Boolean {
            val repository by inject<ApiRepository>()
            return repository.assertCompatibility()
        }
    }

    /**
     *  Assert that the installed version of Pixel Ambient Music is compatible with this version
     *  of Ambient Music Mod.
     */
    fun assertCompatibility(): Boolean

}

class ApiRepositoryImpl(private val context: Context) : ApiRepository {

    companion object {
        private const val TAG = "ApiRepository"
    }

    private val scope = MainScope()
    private val packageManager = context.packageManager
    private var remoteApiVersion: Int? = null

    override fun assertCompatibility(): Boolean {
        val remoteApi = getRemoteApiVersion() ?: return false //Not installed = auto fail
        return COMPATIBLE_APIS.contains(remoteApi).also {
            if(!it) Log.e(
                TAG,
                "Incompatible API version found: $remoteApi. Acceptable: [${
                    COMPATIBLE_APIS.joinToString(", ")
                }]"
            )
        }
    }

    private fun getRemoteApiVersion(): Int? {
        remoteApiVersion?.let {
            return it
        }
        val info = try {
            packageManager.getApplicationInfo(PACKAGE_NAME_PAM, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
        return info.metaData.getInt(API_VERSION_TAG).also {
            remoteApiVersion = it
        }
    }

    private fun setupPackageListener() = scope.launch {
        context.onPackageChanged(PACKAGE_NAME_PAM, false).collect {
            //Trigger the next call to reload the API version due to a possible change
            remoteApiVersion = null
        }
    }

    init {
        setupPackageListener()
    }

}