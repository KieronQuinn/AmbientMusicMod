package com.kieronquinn.app.ambientmusicmod.repositories

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.google.gson.JsonIOException
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.kieronquinn.app.ambientmusicmod.R
import com.kieronquinn.app.ambientmusicmod.model.backup.*
import com.kieronquinn.app.ambientmusicmod.repositories.BackupRestoreRepository.*
import com.kieronquinn.app.ambientmusicmod.repositories.RemoteSettingsRepository.SettingsState
import com.kieronquinn.app.ambientmusicmod.utils.extensions.map
import com.kieronquinn.app.ambientmusicmod.utils.extensions.safeQuery
import com.kieronquinn.app.pixelambientmusic.model.SettingsStateChange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.parcelize.Parcelize
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipException
import kotlin.math.roundToInt

interface BackupRestoreRepository {

    fun createBackup(uri: Uri): Flow<BackupState>
    fun restoreBackup(uri: Uri, restoreOptions: RestoreOptions): Flow<RestoreState>

    @Parcelize
    data class RestoreOptions(
        val restoreHistory: Boolean,
        val clearHistory: Boolean,
        val clearLinear: Boolean,
        val restoreLinear: Boolean,
        val restoreFavourites: Boolean,
        val clearFavourites: Boolean,
        val restoreSettings: Boolean
    ): Parcelable

    sealed class BackupState(@StringRes val title: Int) {
        object Loading: BackupState(R.string.loading)
        object LoadingHistory: BackupState(R.string.backup_loading_history)
        object LoadingFavourites: BackupState(R.string.backup_loading_favourites)
        object LoadingLinear: BackupState(R.string.backup_loading_linear)
        object LoadingSettings: BackupState(R.string.backup_loading_settings)
        object WritingBackup: BackupState(R.string.backup_loading_writing)
        data class BackupComplete(val result: BackupResult): BackupState(result.title)
    }

    sealed class RestoreState(@StringRes val title: Int) {
        object LoadingBackup: RestoreState(R.string.restore_loading_backup)
        object ClearingHistory: RestoreState(R.string.restore_clearing_history)
        data class RestoringHistory(val progress: Int): RestoreState(R.string.restore_restoring_history)
        object ClearingFavourites: RestoreState(R.string.restore_clearing_favourites)
        data class RestoringFavourites(val progress: Int): RestoreState(R.string.restore_restoring_favourites)
        object ClearingLinear: RestoreState(R.string.restore_clearing_linear)
        data class RestoringLinear(val progress: Int): RestoreState(R.string.restore_restoring_linear)
        object RestoringSettings: RestoreState(R.string.restore_restoring_settings)
        data class RestoreComplete(val result: RestoreResult): RestoreState(result.title)
    }

    enum class BackupResult(
        @DrawableRes
        val icon: Int,
        @StringRes
        val title: Int,
        @StringRes
        val content: Int
    ) {
        SUCCESS(
            R.drawable.ic_check_circle,
            R.string.backup_success_title,
            R.string.backup_success_content
        ),
        FAILED_TO_CONNECT(
            R.drawable.ic_error_circle,
            R.string.backup_failed_to_connect_title,
            R.string.backup_failed_to_connect_content
        ),
        FAILED_TO_WRITE(
            R.drawable.ic_error_circle,
            R.string.backup_failed_to_write_title,
            R.string.backup_failed_to_write_content
        )
    }

    enum class RestoreResult(
        @DrawableRes
        val icon: Int,
        @StringRes
        val title: Int,
        @StringRes
        val content: Int
    ) {
        SUCCESS(
            R.drawable.ic_check_circle,
            R.string.restore_success_title,
            R.string.restore_success_content
        ),
        FAILED_TO_OPEN(
            R.drawable.ic_error_circle,
            R.string.restore_failed_to_open_title,
            R.string.restore_failed_to_open_content
        ),
        FAILED_TO_READ(
            R.drawable.ic_error_circle,
            R.string.restore_failed_to_read_title,
            R.string.restore_failed_to_read_content
        ),
        FAILED_VERSION_INCOMPATIBLE(
            R.drawable.ic_error_circle,
            R.string.restore_failed_incompatible_title,
            R.string.restore_failed_incompatible_content
        )
    }

}

class BackupRestoreRepositoryImpl(
    context: Context,
    private val settingsRepository: SettingsRepository,
    private val deviceConfigRepository: DeviceConfigRepository,
    private val remoteSettingsRepository: RemoteSettingsRepository
): BackupRestoreRepository {

    companion object {
        private const val SCHEME = "content"
        private const val AUTHORITY = "com.google.android.as.pam.ambientmusic.backuprestoreprovider"

        private val URI_HISTORY = Uri.Builder().apply {
            scheme(SCHEME)
            authority(AUTHORITY)
            path("history")
        }.build()

        private val URI_FAVOURITES = Uri.Builder().apply {
            scheme(SCHEME)
            authority(AUTHORITY)
            path("favourites")
        }.build()

        private val URI_LINEAR = Uri.Builder().apply {
            scheme(SCHEME)
            authority(AUTHORITY)
            path("linear")
        }.build()

        private val URI_LINEAR_V3 = Uri.Builder().apply {
            scheme(SCHEME)
            authority(AUTHORITY)
            path("linear_v3")
        }.build()
    }

    private val contentProvider = context.contentResolver
    private val gson = Gson()
    private val createDocumentFile = { uri: Uri ->
        DocumentFile.fromSingleUri(context, uri)
    }

    override fun createBackup(uri: Uri): Flow<BackupState> = flow {
        val backup = createBackup(this) ?: run {
            emit(BackupState.BackupComplete(BackupResult.FAILED_TO_CONNECT))
            return@flow
        }
        val file = createDocumentFile(uri)
        if(file == null || !file.canWrite()){
            emit(BackupState.BackupComplete(BackupResult.FAILED_TO_WRITE))
            return@flow
        }
        emit(BackupState.WritingBackup)
        val success = gson.contentProviderBackedWriter(uri) {
            try {
                gson.toJson(backup, Backup::class.java, it)
                true
            }catch (e: JsonIOException) {
                false
            }
        }
        emit(BackupState.BackupComplete(if(success){
            BackupResult.SUCCESS
        } else {
            BackupResult.FAILED_TO_WRITE
        }))
    }.flowOn(Dispatchers.IO)

    private suspend fun createBackup(flow: FlowCollector<BackupState>): Backup? {
        flow.emit(BackupState.LoadingHistory)
        val history = contentProvider.safeQuery(
            URI_HISTORY, null, null, null, null
        )?.map {
            HistoryItem.fromCursor(it)
        } ?: return null
        flow.emit(BackupState.LoadingFavourites)
        val favourites = contentProvider.safeQuery(
            URI_FAVOURITES, null, null, null, null
        )?.map {
            FavouritesItem.fromCursor(it)
        } ?: return null
        flow.emit(BackupState.LoadingLinear)
        val linear = contentProvider.safeQuery(
            URI_LINEAR, null, null, null, null
        )?.map {
            LinearItem.fromCursor(it)
        } ?: emptyList()
        val linearv3 = contentProvider.safeQuery(
            URI_LINEAR_V3, null, null, null, null
        )?.map {
            LinearItem.fromCursor(it)
        } ?: emptyList()
        flow.emit(BackupState.LoadingSettings)
        val settings = loadSettingsBackup() ?: return null
        return Backup(
            history = history,
            linear = linear,
            linearv3 = linearv3,
            favourites = favourites,
            settingsBackup = settings
        )
    }

    override fun restoreBackup(uri: Uri, restoreOptions: RestoreOptions): Flow<RestoreState> = flow {
        emit(RestoreState.LoadingBackup)
        val file = createDocumentFile(uri)
        if(file == null || !file.canRead()) {
            emit(RestoreState.RestoreComplete(RestoreResult.FAILED_TO_OPEN))
        }
        val backup = gson.contentProviderBackedReader(uri) {
            try {
                gson.fromJson<Backup>(it, Backup::class.java)
            }catch (e: Exception){
                null
            }
        } ?: run {
            emit((RestoreState.RestoreComplete(RestoreResult.FAILED_TO_READ)))
            return@flow
        }
        val result = restoreBackup(this, backup, restoreOptions)
        emit(RestoreState.RestoreComplete(result))
    }.flowOn(Dispatchers.IO)

    private suspend fun restoreBackup(
        flow: FlowCollector<RestoreState>, backup: Backup, options: RestoreOptions
    ): RestoreResult {
        if(backup.version != Backup.VERSION) return RestoreResult.FAILED_VERSION_INCOMPATIBLE
        if(options.clearHistory){
            flow.emit(RestoreState.ClearingHistory)
            contentProvider.delete(URI_HISTORY, null, null)
        }
        if(options.restoreHistory) {
            val size = backup.history.size.toFloat()
            backup.history.forEachIndexed { index, historyItem ->
                val progress = ((index / size) * 100).roundToInt()
                flow.emit(RestoreState.RestoringHistory(progress))
                contentProvider.insert(URI_HISTORY, historyItem.toContentValues())
            }
        }
        if(options.clearFavourites) {
            flow.emit(RestoreState.ClearingFavourites)
            contentProvider.delete(URI_FAVOURITES, null, null)
        }
        if(options.restoreFavourites) {
            val size = backup.favourites.size.toFloat()
            backup.favourites.forEachIndexed { index, favouritesItem ->
                val progress = ((index / size) * 100).roundToInt()
                flow.emit(RestoreState.RestoringFavourites(progress))
                contentProvider.insert(URI_FAVOURITES, favouritesItem.toContentValues())
            }
        }
        if(options.clearLinear){
            flow.emit(RestoreState.ClearingLinear)
            contentProvider.delete(URI_LINEAR, null, null)
            contentProvider.delete(URI_LINEAR_V3, null, null)
        }
        if(options.restoreLinear){
            val size = (backup.linear.size + backup.linearv3.size).toFloat()
            backup.linear.forEachIndexed { index, linearItem ->
                val progress = ((index / size) * 100).roundToInt()
                flow.emit(RestoreState.RestoringLinear(progress))
                contentProvider.insert(URI_LINEAR, linearItem.toContentValues())
            }
            backup.linearv3.forEachIndexed { index, linearItem ->
                val progress = (((backup.linear.size + index) / size) * 100).roundToInt()
                flow.emit(RestoreState.RestoringLinear(progress))
                contentProvider.insert(URI_LINEAR_V3, linearItem.toContentValues())
            }
        }
        flow.emit(RestoreState.RestoringSettings)
        if(options.restoreSettings){
            restoreSettingsBackup(backup.settingsBackup)
        }
        return RestoreResult.SUCCESS
    }

    private suspend fun loadSettingsBackup(): SettingsBackup? {
        val remote = remoteSettingsRepository.getRemoteSettings().first() ?: return null
        if(remote !is SettingsState.Available) return null
        return SettingsBackup(
            remote.mainEnabled,
            remote.onDemandEnabled,
            settingsRepository.recognitionPeriod.getOrNull(),
            settingsRepository.recognitionPeriodAdaptive.getOrNull(),
            settingsRepository.recognitionBuffer.getOrNull(),
            settingsRepository.lockscreenOverlayEnhanced.getOrNull(),
            settingsRepository.lockscreenOverlayStyle.getOrNull(),
            settingsRepository.lockscreenOverlayYPos.getOrNull(),
            settingsRepository.lockscreenOverlayClicked.getOrNull(),
            settingsRepository.lockscreenOwnerInfo.getOrNull(),
            settingsRepository.lockscreenOwnerInfoShowNote.getOrNull(),
            settingsRepository.lockscreenOwnerInfoFallback.getOrNull(),
            settingsRepository.onDemandLockscreenEnabled.getOrNull(),
            settingsRepository.triggerWhenScreenOn.getOrNull(),
            settingsRepository.runOnBatterySaver.getOrNull(),
            settingsRepository.bedtimeModeEnabled.getOrNull(),
            settingsRepository.bedtimeModeStart.getOrNull(),
            settingsRepository.bedtimeModeEnd.getOrNull(),
            settingsRepository.automaticMusicDatabaseUpdates.getOrNull(),
            deviceConfigRepository.cacheShardEnabled.getOrNull(),
            deviceConfigRepository.runOnSmallCores.getOrNull(),
            deviceConfigRepository.nnfpv3Enabled.getOrNull(),
            deviceConfigRepository.onDemandVibrateEnabled.getOrNull(),
            deviceConfigRepository.deviceCountry.getOrNull(),
            deviceConfigRepository.superpacksRequireCharging.getOrNull(),
            deviceConfigRepository.superpacksRequireWiFi.getOrNull(),
            deviceConfigRepository.recordingGain.getOrNull(),
            deviceConfigRepository.showAlbumArt.getOrNull(),
            settingsRepository.lockscreenOverlayColour.getOrNull(),
            settingsRepository.lockscreenOverlayCustomColour.getOrNull(),
            deviceConfigRepository.alternativeEncoding.getOrNull()
        )
    }

    private suspend fun restoreSettingsBackup(settingsBackup: SettingsBackup) = with(settingsBackup) {
        //Remote
        remoteSettingsRepository.commitChanges(
            SettingsStateChange(mainEnabled, onDemandEnabled)
        )
        //Local
        recognitionPeriod.restoreTo(settingsRepository.recognitionPeriod)
        recognitionPeriodAdaptive.restoreTo(settingsRepository.recognitionPeriodAdaptive)
        recognitionBuffer.restoreTo(settingsRepository.recognitionBuffer)
        lockscreenOverlayEnhanced.restoreTo(settingsRepository.lockscreenOverlayEnhanced)
        lockscreenOverlayStyle.restoreTo(settingsRepository.lockscreenOverlayStyle)
        lockscreenOverlayYPos.restoreTo(settingsRepository.lockscreenOverlayYPos)
        lockscreenOnTrackClicked.restoreTo(settingsRepository.lockscreenOverlayClicked)
        lockscreenOwnerInfo.restoreTo(settingsRepository.lockscreenOwnerInfo)
        lockscreenOwnerInfoShowNote.restoreTo(settingsRepository.lockscreenOwnerInfoShowNote)
        lockscreenOwnerInfoFallback.restoreTo(settingsRepository.lockscreenOwnerInfoFallback)
        onDemandLockscreenEnabled.restoreTo(settingsRepository.onDemandLockscreenEnabled)
        triggerWhenScreenOn.restoreTo(settingsRepository.triggerWhenScreenOn)
        runOnBatterySaver.restoreTo(settingsRepository.runOnBatterySaver)
        bedtimeModeEnabled.restoreTo(settingsRepository.bedtimeModeEnabled)
        bedtimeModeStart.restoreTo(settingsRepository.bedtimeModeStart)
        bedtimeModeEnd.restoreTo(settingsRepository.bedtimeModeEnd)
        automaticDatabaseUpdates.restoreTo(settingsRepository.automaticMusicDatabaseUpdates)
        //Device Config
        cacheShardEnabled.restoreTo(deviceConfigRepository.cacheShardEnabled)
        runOnSmallCores.restoreTo(deviceConfigRepository.runOnSmallCores)
        nnfpv3Enabled.restoreTo(deviceConfigRepository.nnfpv3Enabled)
        onDemandVibrateEnabled.restoreTo(deviceConfigRepository.onDemandVibrateEnabled)
        deviceCountry.restoreTo(deviceConfigRepository.deviceCountry)
        superpacksRequireCharging.restoreTo(deviceConfigRepository.superpacksRequireCharging)
        superpacksRequireWiFi.restoreTo(deviceConfigRepository.superpacksRequireWiFi)
        recordingGain.restoreTo(deviceConfigRepository.recordingGain)
        showAlbumArt.restoreTo(deviceConfigRepository.showAlbumArt)

        overlayTextColour.restoreTo(settingsRepository.lockscreenOverlayColour)
        overlayCustomTextColour.restoreTo(settingsRepository.lockscreenOverlayCustomColour)
        alternativeEncoding.restoreTo(deviceConfigRepository.alternativeEncoding)
    }

    private suspend fun <T> T?.restoreTo(setting: BaseSettingsRepository.AmbientMusicModSetting<T>) {
        if(this == null) return
        setting.set(this)
    }

    private fun <T> Gson.contentProviderBackedWriter(uri: Uri, block: (JsonWriter) -> T): T {
        return contentProvider.openOutputStream(uri).use { raw ->
            val gzippedOutput = GZIPOutputStream(raw).use { stream ->
                val result = OutputStreamWriter(stream).use {
                    val result = block(newJsonWriter(it))
                    it.flush()
                    result
                }
                stream.flush()
                result
            }
            raw?.flush()
            gzippedOutput
        }
    }

    private fun <T> Gson.contentProviderBackedReader(uri: Uri, block: (JsonReader) -> T): T? {
        return contentProvider.openInputStream(uri).use { raw ->
            try {
                GZIPInputStream(raw).use { stream ->
                    InputStreamReader(stream).use {
                        block(newJsonReader(it))
                    }
                }
            }catch (e: ZipException){
                null
            }
        }
    }

}