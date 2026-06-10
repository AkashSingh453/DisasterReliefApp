package com.disasterrelief.app.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.disasterrelief.app.data.local.dao.MessageDao
import com.disasterrelief.app.data.local.dao.SOSRequestDao
import com.disasterrelief.app.data.remote.SyncApiService
import com.disasterrelief.app.mesh.MeshNetworkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val syncApiService: SyncApiService, // Kept for backward compatibility if needed
    private val crdtSyncEngine: CrdtSyncEngine,
    private val sosRequestDao: SOSRequestDao,
    private val messageDao: MessageDao,
    private val meshNetworkManager: MeshNetworkManager
) {
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "CloudSyncManager"

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun connect(baseUrl: String) {
        if (webSocket != null) return
        if (!isNetworkAvailable()) {
            Log.w(TAG, "No network available. Skipping WebSocket connection.")
            return
        }

        val wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://") + "api/v1/sync/ws"
        val request = Request.Builder().url(wsUrl).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connected successfully to cloud")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.i(TAG, "WebSocket received message from cloud")
                scope.launch {
                    try {
                        val payload = crdtSyncEngine.decodeFromJsonString(text)
                        
                        // 1. Merge into local Room Database
                        val hasNewData = crdtSyncEngine.mergeDelta(payload)
                        
                        // 2. Broadcast to offline mesh so they get the cloud update!
                        if (hasNewData) {
                            meshNetworkManager.broadcastPayload(payload)
                        }
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing incoming WebSocket payload", e)
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                Log.i(TAG, "WebSocket closing: $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@CloudSyncManager.webSocket = null
                Log.i(TAG, "WebSocket closed.")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@CloudSyncManager.webSocket = null
                Log.e(TAG, "WebSocket connection failed", t)
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }

    /**
     * Serializes the payload to JSON and sends it up to the Ktor server via WebSocket.
     */
    fun sendPayload(payload: SyncPayload) {
        if (webSocket == null) {
            Log.w(TAG, "Cannot send payload, WebSocket is not connected.")
            return
        }
        
        scope.launch {
            try {
                val json = crdtSyncEngine.encodeToJsonString(payload)
                webSocket?.send(json)
                Log.i(TAG, "WebSocket sent payload to cloud")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send WebSocket payload", e)
            }
        }
    }

    /**
     * Performs a full initial two-way REST sync with the cloud server.
     */
    fun initialSync(nodeId: String) {
        if (!isNetworkAvailable()) return
        scope.launch {
            try {
                // 1. Download full server state
                val response = syncApiService.downloadSync(nodeId, 0L)
                if (response.isSuccessful && response.body() != null) {
                    val serverPayload = response.body()!!
                    val hasNewData = crdtSyncEngine.mergeDelta(serverPayload)
                    if (hasNewData) {
                        meshNetworkManager.broadcastPayload(serverPayload)
                    }
                    Log.i(TAG, "Initial sync downloaded data successfully.")
                }
                
                // 2. Upload full local state
                val localPayload = crdtSyncEngine.serializeDelta(nodeId, 0L)
                syncApiService.uploadSync(localPayload)
                Log.i(TAG, "Initial sync uploaded data successfully.")
                
            } catch (e: Exception) {
                Log.e(TAG, "Initial sync failed", e)
            }
        }
    }
}
