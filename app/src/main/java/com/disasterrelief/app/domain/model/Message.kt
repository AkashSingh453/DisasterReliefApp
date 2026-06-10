package com.disasterrelief.app.domain.model

/**
 * Domain model for a mesh chat message.
 */
data class Message(
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
