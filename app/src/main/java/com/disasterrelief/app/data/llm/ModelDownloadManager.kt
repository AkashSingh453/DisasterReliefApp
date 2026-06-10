package com.disasterrelief.app.data.llm

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val modelFileName = "emergency_llm.bin"
    
    // This is the absolute path you will pass to LiteRT-LM
    val modelFile: File 
        get() = File(context.filesDir, modelFileName)

    private val _downloadState = MutableStateFlow<DownloadState>(if (isModelDownloaded()) DownloadState.Completed(modelFile) else DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var pollingJob: Job? = null

    fun isModelDownloaded(): Boolean {
        return modelFile.exists() && modelFile.length() > 0
    }

    fun downloadModel(serverUrl: String) {
        if (isModelDownloaded()) {
            _downloadState.value = DownloadState.Completed(modelFile)
            return
        }

        _downloadState.value = DownloadState.Downloading(0f)

        val request = DownloadManager.Request(Uri.parse(serverUrl))
            .setTitle("Downloading Emergency AI Engine")
            .setDescription("Optimizing localized triage models...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            // Temporarily saved to external files, we will move it to secure internal storage on completion
            .setDestinationInExternalFilesDir(context, null, modelFileName) 
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)
        
        // Save download ID to prefs so the receiver knows which download is the model
        val prefs = context.getSharedPreferences("llm_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("model_download_id", downloadId).apply()
        
        startPollingProgress(downloadManager, downloadId)
    }

    private fun startPollingProgress(downloadManager: DownloadManager, downloadId: Long) {
        pollingJob?.cancel()
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            var downloading = true
            while (downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    
                    if (bytesDownloadedIndex >= 0 && bytesTotalIndex >= 0 && statusIndex >= 0) {
                        val bytesDownloaded = cursor.getInt(bytesDownloadedIndex)
                        val bytesTotal = cursor.getInt(bytesTotalIndex)
                        val status = cursor.getInt(statusIndex)
                        
                        if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                            downloading = false
                        } else if (bytesTotal > 0) {
                            val progress = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                            _downloadState.value = DownloadState.Downloading(progress)
                        }
                    }
                }
                cursor?.close()
                if (downloading) delay(500)
            }
        }
    }
    
    fun setCompletedState() {
        pollingJob?.cancel()
        _downloadState.value = DownloadState.Completed(modelFile)
    }

    fun setErrorState(error: String) {
        pollingJob?.cancel()
        _downloadState.value = DownloadState.Error(error)
    }

    fun deleteModel() {
        if (modelFile.exists()) {
            modelFile.delete()
        }
        val prefs = context.getSharedPreferences("llm_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("model_download_id").apply()
        _downloadState.value = DownloadState.Idle
    }
}

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    data class Completed(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}
