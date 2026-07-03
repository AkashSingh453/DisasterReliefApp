package com.disasterrelief.app.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.disasterrelief.app.mesh.MeshNetworkManager
import com.disasterrelief.proto.DownloadRequestProto
import com.disasterrelief.proto.SyncPayloadProto
import com.disasterrelief.proto.SyncServiceGrpcKt
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Job

@Singleton
class CloudSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncService: SyncServiceGrpcKt.SyncServiceCoroutineStub,
    private val crdtSyncEngine: CrdtSyncEngine,
    private val meshNetworkManager: MeshNetworkManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var streamJob: Job? = null
    private val outboundFlow = MutableSharedFlow<SyncPayloadProto>(extraBufferCapacity = 64)
    private val TAG = "CloudSyncManager"

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun connect(baseUrl: String) { // baseUrl parameter is no longer strictly used here as channel is bound in DI
        if (streamJob?.isActive == true) return
        if (!isNetworkAvailable()) {
            Log.w(TAG, "No network available. Skipping gRPC streaming connection.")
            return
        }

        streamJob = scope.launch {
            try {
                Log.i(TAG, "Starting gRPC bidirectional stream to cloud")
                
                // Call the bidirectional streaming RPC
                syncService.streamSync(outboundFlow).collect { incomingProto ->
                    val incomingSize = incomingProto.serializedSize
                    Log.d(TAG, "gRPC stream received payload from cloud: $incomingSize bytes")
                    
                    try {
                        val payload = incomingProto.toDomain()
                        
                        // 1. Merge into local Room Database
                        val hasNewData = crdtSyncEngine.mergeDelta(payload)
                        
                        // 2. Broadcast to offline mesh so they get the cloud update!
                        if (hasNewData) {
                            meshNetworkManager.broadcastPayload(payload)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing incoming gRPC payload", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "gRPC stream failed or closed", e)
                streamJob = null
            }
        }
    }

    fun disconnect() {
        streamJob?.cancel()
        streamJob = null
        Log.i(TAG, "gRPC stream disconnected.")
    }

    /**
     * Serializes the payload to Protobuf and pushes it into the active gRPC stream.
     */
    fun sendPayload(payload: SyncPayload) {
        if (streamJob?.isActive != true) {
            Log.w(TAG, "Cannot send payload, gRPC stream is not active.")
            return
        }
        
        scope.launch {
            try {
                val proto = payload.toProto()
                val bytesSize = proto.serializedSize
                outboundFlow.emit(proto)
                Log.d(TAG, "Sent streaming payload to server: $bytesSize bytes (SOS: ${payload.sosRequests.size}, Msgs: ${payload.messages.size})")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to emit gRPC payload", e)
            }
        }
    }

    /**
     * Performs a full initial two-way sync with the cloud server using standard gRPC unary calls.
     */
    fun initialSync(nodeId: String) {
        if (!isNetworkAvailable()) return
        scope.launch {
            try {
                // 1. Download full server state
                val request = DownloadRequestProto.newBuilder()
                    .setNodeId(nodeId)
                    .setSinceTimestamp(0L)
                    .build()
                    
                val serverProto = syncService.downloadSync(request)
                val downloadedSize = serverProto.serializedSize
                Log.d(TAG, "Downloaded full sync payload from server: $downloadedSize bytes")
                val serverPayload = serverProto.toDomain()
                
                val hasNewData = crdtSyncEngine.mergeDelta(serverPayload)
                if (hasNewData) {
                    meshNetworkManager.broadcastPayload(serverPayload)
                }
                Log.i(TAG, "Initial sync downloaded data successfully via gRPC.")
                
                // 2. Upload full local state
                val localPayload = crdtSyncEngine.serializeDelta(nodeId, 0L)
                val localProto = localPayload.toProto()
                val uploadedSize = localProto.serializedSize
                Log.d(TAG, "Uploading full sync payload to server: $uploadedSize bytes")
                syncService.uploadSync(localProto)
                Log.i(TAG, "Initial sync uploaded data successfully via gRPC.")
                
            } catch (e: Exception) {
                Log.e(TAG, "Initial sync failed", e)
            }
        }
    }
}
