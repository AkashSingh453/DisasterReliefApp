package com.disasterrelief.app.domain.model

/**
 * Domain model for a user/device node in the mesh network.
 */
data class UserNode(
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
