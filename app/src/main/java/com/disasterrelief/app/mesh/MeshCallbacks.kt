package com.disasterrelief.app.mesh

import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate

/**
 * Clean callback implementations for the Nearby Connections API.
 *
 * Each callback delegates to lambda functions provided by [MeshNetworkManager],
 * keeping the callback logic decoupled from the manager's state management.
 * This also avoids anonymous inner class sprawl in the manager.
 */

// ══════════════════════════════════════════════════════════════════════
//  ENDPOINT DISCOVERY
// ══════════════════════════════════════════════════════════════════════

/**
 * Wraps [EndpointDiscoveryCallback] and delegates to lambda handlers.
 *
 * In the disaster relief mesh, endpoint discovery triggers an automatic
 * connection request — we want to connect to every device we find.
 */
class MeshEndpointDiscoveryCallback(
    private val onEndpointFound: (endpointId: String, info: DiscoveredEndpointInfo) -> Unit,
    private val onEndpointLost: (endpointId: String) -> Unit
) : EndpointDiscoveryCallback() {

    override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
        onEndpointFound.invoke(endpointId, info)
    }

    override fun onEndpointLost(endpointId: String) {
        onEndpointLost.invoke(endpointId)
    }
}

// ══════════════════════════════════════════════════════════════════════
//  CONNECTION LIFECYCLE
// ══════════════════════════════════════════════════════════════════════

/**
 * Wraps [ConnectionLifecycleCallback] and delegates to lambda handlers.
 *
 * In the disaster relief context:
 * - [onConnectionInitiated]: Auto-accepts all connections (no auth verification).
 * - [onConnectionResult]: On success, adds the peer and triggers a full state sync.
 * - [onDisconnected]: Removes the peer from the active connections list.
 */
class MeshConnectionLifecycleCallback(
    private val onConnectionInitiated: (endpointId: String, info: ConnectionInfo) -> Unit,
    private val onConnectionResult: (endpointId: String, result: ConnectionResolution) -> Unit,
    private val onDisconnected: (endpointId: String) -> Unit
) : ConnectionLifecycleCallback() {

    override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
        onConnectionInitiated.invoke(endpointId, info)
    }

    override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
        onConnectionResult.invoke(endpointId, result)
    }

    override fun onDisconnected(endpointId: String) {
        onDisconnected.invoke(endpointId)
    }
}

// ══════════════════════════════════════════════════════════════════════
//  PAYLOAD TRANSFER
// ══════════════════════════════════════════════════════════════════════

/**
 * Wraps [PayloadCallback] and delegates to lambda handlers.
 *
 * Handles incoming CRDT sync payloads (BYTES type) from connected peers.
 * The actual deserialization and CRDT merge is performed by [MeshNetworkManager]
 * via the [MeshPayloadHandler] and [com.disasterrelief.app.data.sync.CrdtSyncEngine].
 */
class MeshPayloadCallbackImpl(
    private val onPayloadReceived: (endpointId: String, payload: Payload) -> Unit,
    private val onPayloadTransferUpdate: (endpointId: String, update: PayloadTransferUpdate) -> Unit
) : PayloadCallback() {

    override fun onPayloadReceived(endpointId: String, payload: Payload) {
        onPayloadReceived.invoke(endpointId, payload)
    }

    override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
        onPayloadTransferUpdate.invoke(endpointId, update)
    }
}
