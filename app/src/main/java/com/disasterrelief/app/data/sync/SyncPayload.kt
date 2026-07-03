package com.disasterrelief.app.data.sync

/**
 * Wire-format payload for CRDT delta synchronization.
 *
 * This structure is used for both:
 * 1. **Mesh transport**: Serialized to compact protobuf bytes and sent via Nearby Connections payloads.
 * 2. **Cloud sync**: Transmitted as gRPC request/response body.
 *
 * The payload contains delta updates — only records modified since the last sync point.
 * Each DTO mirrors its corresponding Room entity but is decoupled for transport optimization.
 */
data class SyncPayload(
    /** UUID of the node that produced this payload. */
    val sourceNodeId: String,
    /** Epoch ms when this payload was assembled. */
    val timestamp: Long,
    /** Delta SOS request records. */
    val sosRequests: List<SOSRequestDto> = emptyList(),
    /** Delta chat message records. */
    val messages: List<MessageDto> = emptyList(),
    /** Delta user node records. */
    val userNodes: List<UserNodeDto> = emptyList()
)

/**
 * Transport DTO for SOS requests. Mirrors [com.disasterrelief.app.data.local.entity.SOSRequestEntity]
 */
data class SOSRequestDto(
    val id: String,
    val injuryType: String,
    val severity: Int,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val createdByNodeId: String,
    val lastUpdatedTimestamp: Long,
    val originNodeId: String,
    val isDeleted: Boolean = false
)

/**
 * Transport DTO for chat messages.
 */
data class MessageDto(
    val id: String,
    val senderNodeId: String,
    val senderName: String,
    val content: String,
    val recipientId: String? = null,
    val isAlert: Boolean = false,
    val alertPriority: Int = 0,
    val lastUpdatedTimestamp: Long,
    val originNodeId: String,
    val isDeleted: Boolean = false
)

/**
 * Transport DTO for user node identity records.
 */
data class UserNodeDto(
    val id: String,
    val displayName: String,
    val role: String = "VOLUNTEER",
    val lastKnownLatitude: Double? = null,
    val lastKnownLongitude: Double? = null,
    val lastUpdatedTimestamp: Long,
    val originNodeId: String,
    val isDeleted: Boolean = false,
    val isOnline: Boolean = false
)

