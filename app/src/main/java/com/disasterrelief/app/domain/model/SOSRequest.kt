package com.disasterrelief.app.domain.model

/**
 * Domain model for an SOS distress request.
 * Clean separation from the Room entity — the presentation layer only sees this.
 */
data class SOSRequest(
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
