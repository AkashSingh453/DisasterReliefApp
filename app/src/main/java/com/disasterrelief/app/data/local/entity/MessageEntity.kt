package com.disasterrelief.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a chat message in the mesh network.
 *
 * Messages are append-only in nature (you don't typically edit a chat message),
 * but the LWW metadata is retained for consistency across the CRDT sync engine.
 * Edits or deletions use the [lastUpdatedTimestamp] for conflict resolution.
 */
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["lastUpdatedTimestamp"]),
        Index(value = ["isSyncedToCloud"]),
        Index(value = ["senderNodeId"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "senderNodeId")
    val senderNodeId: String,

    @ColumnInfo(name = "senderName")
    val senderName: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "recipientId")
    val recipientId: String? = null,

    @ColumnInfo(name = "isAlert")
    val isAlert: Boolean = false,

    @ColumnInfo(name = "alertPriority")
    val alertPriority: Int = 0,

    // ── CRDT Metadata ──

    @ColumnInfo(name = "lastUpdatedTimestamp")
    val lastUpdatedTimestamp: Long,

    @ColumnInfo(name = "originNodeId")
    val originNodeId: String,

    @ColumnInfo(name = "isDeleted")
    val isDeleted: Boolean = false,

    // ── Cloud Sync Tracking ──

    @ColumnInfo(name = "isSyncedToCloud")
    val isSyncedToCloud: Boolean = false
)
