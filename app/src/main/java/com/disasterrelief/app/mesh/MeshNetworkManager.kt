package com.disasterrelief.app.mesh

import android.content.Context
import android.util.Log
import com.disasterrelief.app.data.sync.CrdtSyncEngine
import com.disasterrelief.app.data.sync.SyncPayload
import com.disasterrelief.app.domain.model.DiscoveredPeer
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.Strategy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Lazy
import com.disasterrelief.app.data.sync.CloudSyncManager

/**
 * Manages the P2P mesh network using Google Nearby Connections API
 * with [Strategy.P2P_CLUSTER] topology.
 *
 * Key design decisions:
 * - **Singleton scope**: One mesh manager per application lifecycle, injected by Hilt.
 * - **Concurrent advertise + discover**: Both run simultaneously so every device can
 *   both find peers and be found, forming an amorphous mesh cluster.
 * - **Auto-accept**: All incoming connection requests are automatically accepted to
 *   maximize mesh connectivity in disaster scenarios (no auth handshake).
 * - **Auto-sync on connect**: When a new peer connects, the full local database delta
 *   is immediately broadcast to the new peer.
 *
 * Reactive state:
 * - [connectedPeers]: Live list of all currently connected peer devices.
 * - [syncEvents]: Stream of sync events for UI display (incoming/outgoing payloads).
 */
@Singleton
class MeshNetworkManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crdtSyncEngine: CrdtSyncEngine,
    private val payloadHandler: MeshPayloadHandler,
    private val cloudSyncManager: Lazy<CloudSyncManager>
) {

    companion object {
        private const val TAG = "MeshNetworkManager"
        private const val SERVICE_ID = "com.disasterrelief.mesh"
        private val STRATEGY = Strategy.P2P_CLUSTER
    }

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ── Reactive State ──

    private val _connectedPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val connectedPeers: StateFlow<List<DiscoveredPeer>> = _connectedPeers.asStateFlow()

    private val _syncEvents = MutableSharedFlow<SyncEvent>(extraBufferCapacity = 64)
    val syncEvents: SharedFlow<SyncEvent> = _syncEvents.asSharedFlow()

    private val _isMeshActive = MutableStateFlow(false)
    val isMeshActive: StateFlow<Boolean> = _isMeshActive.asStateFlow()

    // ── Mesh identity ──
    private var localNodeId: String = ""
    private var localDisplayName: String = ""

    // ── Callback instances ──
    private val endpointDiscoveryCallback = MeshEndpointDiscoveryCallback(
        onEndpointFound = { endpointId, info ->
            Log.d(TAG, "Discovered endpoint: $endpointId (${info.endpointName})")
            // Auto-request connection upon discovery
            connectionsClient.requestConnection(
                localDisplayName,
                endpointId,
                connectionLifecycleCallback
            ).addOnSuccessListener {
                Log.d(TAG, "Connection request sent to $endpointId")
            }.addOnFailureListener { e ->
                Log.w(TAG, "Failed to request connection to $endpointId", e)
            }
        },
        onEndpointLost = { endpointId ->
            Log.d(TAG, "Lost endpoint: $endpointId")
            removePeer(endpointId)
        }
    )

    private val connectionLifecycleCallback = MeshConnectionLifecycleCallback(
        onConnectionInitiated = { endpointId, info ->
            Log.d(TAG, "Connection initiated with $endpointId (${info.endpointName})")
            // Auto-accept all connections to maximize mesh coverage
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        },
        onConnectionResult = { endpointId, result ->
            if (result.status.isSuccess) {
                Log.d(TAG, "Connected to $endpointId")
                addPeer(endpointId, endpointId)
                // Send full state to newly connected peer
                scope.launch {
                    sendFullSyncToPeer(endpointId)
                }
            } else {
                Log.w(TAG, "Connection failed to $endpointId: ${result.status}")
            }
        },
        onDisconnected = { endpointId ->
            Log.d(TAG, "Disconnected from $endpointId")
            removePeer(endpointId)
        }
    )

    private val payloadCallback = MeshPayloadCallbackImpl(
        onPayloadReceived = { endpointId, payload ->
            scope.launch {
                val size = payload.asBytes()?.size ?: 0
                Log.v(TAG, "Successfully received $size bytes from peer $endpointId")
                handleIncomingPayload(endpointId, payload)
            }
        },
        onPayloadTransferUpdate = { endpointId, update ->
            // Removed verbose transfer update logging for cleaner metrics
        }
    )

    // ══════════════════════════════════════════════════════════════════════
    //  PUBLIC API
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Starts the mesh by concurrently advertising and discovering.
     * Both run simultaneously under [Strategy.P2P_CLUSTER].
     */
    fun startMesh(nodeId: String, displayName: String) {
        localNodeId = nodeId
        localDisplayName = displayName
        startAdvertising()
        startDiscovery()
        _isMeshActive.value = true
        Log.i(TAG, "Mesh started for node: $nodeId ($displayName)")
    }

    /**
     * Stops all mesh activity and disconnects from all peers.
     */
    fun stopMesh() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        _connectedPeers.value = emptyList()
        _isMeshActive.value = false
        Log.i(TAG, "Mesh stopped")
    }

    /**
     * Broadcasts a [SyncPayload] to all currently connected peers.
     * Called after a local write (new SOS, new message) to propagate changes.
     */
    fun broadcastPayload(payload: SyncPayload, excludeEndpointId: String? = null) {
        val bytes = payloadHandler.encodePayload(payload)
        val nearbyPayload = Payload.fromBytes(bytes)

        val peerEndpoints = _connectedPeers.value
            .map { it.endpointId }
            .filter { it != excludeEndpointId }
            
        if (peerEndpoints.isNotEmpty()) {
            connectionsClient.sendPayload(peerEndpoints, nearbyPayload)
                .addOnSuccessListener {
                    scope.launch {
                        _syncEvents.emit(
                            SyncEvent.Outgoing(
                                peerCount = peerEndpoints.size,
                                recordCount = payload.sosRequests.size + payload.messages.size + payload.userNodes.size,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                    Log.d(TAG, "Broadcast payload to ${peerEndpoints.size} peers (Size: ${bytes.size} bytes)")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to broadcast payload", e)
                }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRIVATE
    // ══════════════════════════════════════════════════════════════════════

    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        connectionsClient.startAdvertising(
            localDisplayName,
            SERVICE_ID,
            connectionLifecycleCallback,
            options
        ).addOnSuccessListener {
            Log.d(TAG, "Advertising started")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to start advertising", e)
        }
    }

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            options
        ).addOnSuccessListener {
            Log.d(TAG, "Discovery started")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to start discovery", e)
        }
    }

    private fun addPeer(endpointId: String, displayName: String) {
        _connectedPeers.update { peers ->
            if (peers.none { it.endpointId == endpointId }) {
                peers + DiscoveredPeer(
                    endpointId = endpointId,
                    displayName = displayName,
                    isConnected = true
                )
            } else {
                peers
            }
        }
    }

    private fun removePeer(endpointId: String) {
        _connectedPeers.update { peers ->
            peers.filter { it.endpointId != endpointId }
        }
    }

    /**
     * Sends the full local database state to a specific peer.
     * Called immediately after a new connection is established to catch the peer up.
     */
    private suspend fun sendFullSyncToPeer(endpointId: String) {
        try {
            val payload = crdtSyncEngine.serializeDelta(localNodeId, sinceTimestamp = 0L)
            val bytes = payloadHandler.encodePayload(payload)
            connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
            Log.d(TAG, "Sent full sync to $endpointId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send full sync to $endpointId", e)
        }
    }

    /**
     * Handles an incoming payload from a mesh peer.
     * Deserializes the bytes into a [SyncPayload] and merges via the CRDT engine.
     */
    private suspend fun handleIncomingPayload(endpointId: String, payload: Payload) {
        try {
            val bytes = payload.asBytes() ?: return
            val syncPayload = payloadHandler.decodePayload(bytes)

            // Merge into local database
            val hasNewData = crdtSyncEngine.mergeDelta(syncPayload)
            
            if (hasNewData) {
                // Forward payload to the cloud (if connected)
                cloudSyncManager.get().sendPayload(syncPayload)
                
                // Rebroadcast to other mesh peers to enable multi-hop routing
                broadcastPayload(syncPayload, excludeEndpointId = endpointId)
            }

            // Emit sync event for UI
            _syncEvents.emit(
                SyncEvent.Incoming(
                    fromEndpointId = endpointId,
                    recordCount = syncPayload.sosRequests.size + syncPayload.messages.size + syncPayload.userNodes.size,
                    timestamp = System.currentTimeMillis()
                )
            )

            Log.d(
                TAG,
                "Merged payload from $endpointId: " +
                        "${syncPayload.sosRequests.size} SOS, " +
                        "${syncPayload.messages.size} messages, " +
                        "${syncPayload.userNodes.size} nodes"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process incoming payload from $endpointId", e)
        }
    }
}

/**
 * Represents a sync event for UI display.
 */
sealed class SyncEvent {
    abstract val timestamp: Long

    data class Incoming(
        val fromEndpointId: String,
        val recordCount: Int,
        override val timestamp: Long
    ) : SyncEvent()

    data class Outgoing(
        val peerCount: Int,
        val recordCount: Int,
        override val timestamp: Long
    ) : SyncEvent()
}
