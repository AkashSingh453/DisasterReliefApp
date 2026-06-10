package com.disasterrelief.app.domain.model

/**
 * Represents a peer device discovered on the mesh network.
 *
 * @property endpointId The Nearby Connections endpoint identifier (transient, session-scoped).
 * @property nodeId The permanent UUID of the peer's node (from [UserNode.id]).
 * @property displayName The human-readable name the peer advertised.
 * @property isConnected Whether an active data connection is established.
 */
data class DiscoveredPeer(
    val endpointId: String,
    val nodeId: String = "",
    val displayName: String,
    val isConnected: Boolean = false
)
