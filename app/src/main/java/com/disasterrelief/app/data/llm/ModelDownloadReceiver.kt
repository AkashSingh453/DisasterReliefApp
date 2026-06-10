package com.disasterrelief.app.data.llm

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class ModelDownloadReceiver : BroadcastReceiver() {

    @Inject
    lateinit var modelDownloadManager: ModelDownloadManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            
            val prefs = context.getSharedPreferences("llm_prefs", Context.MODE_PRIVATE)
            val expectedDownloadId = prefs.getLong("model_download_id", -1)

            if (downloadId != -1L && downloadId == expectedDownloadId) {
                handleDownloadCompletion(context, downloadId)
            }
        }
    }

    private fun handleDownloadCompletion(context: Context, downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        if (cursor != null && cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (statusIndex >= 0) {
                val status = cursor.getInt(statusIndex)
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    // Find the file in external files dir
                    val externalFile = File(context.getExternalFilesDir(null), modelDownloadManager.modelFileName)
                    
                    if (externalFile.exists()) {
                        try {
                            // Securely copy the file to internal storage (filesDir)
                            val internalFile = modelDownloadManager.modelFile
                            FileInputStream(externalFile).use { input ->
                                FileOutputStream(internalFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            
                            // Delete temporary external file
                            externalFile.delete()
                            
                            modelDownloadManager.setCompletedState()
                            Log.i("ModelDownloadReceiver", "Model downloaded and moved securely to internal storage.")
                        } catch (e: Exception) {
                            Log.e("ModelDownloadReceiver", "Failed to move model file", e)
                            modelDownloadManager.setErrorState(e.message ?: "Failed to copy file")
                        }
                    } else {
                        modelDownloadManager.setErrorState("Downloaded file not found in external dir")
                    }
                } else if (status == DownloadManager.STATUS_FAILED) {
                    val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                    val reason = if (reasonIndex >= 0) cursor.getInt(reasonIndex).toString() else "Unknown Error"
                    modelDownloadManager.setErrorState("Download failed. Reason: $reason")
                }
            }
            cursor.close()
        }
    }
}
