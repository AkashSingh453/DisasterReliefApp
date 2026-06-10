package com.disasterrelief.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an SOS distress request in the CRDT-enabled local database.
 *
 * CRDT Metadata:
 * - [id]: Globally unique UUID generated at creation. Acts as the merge key across all nodes.
 * - [lastUpdatedTimestamp]: Epoch milliseconds. The LWW comparison field — the record with the
 *   highest timestamp wins during conflict resolution.
 * - [originNodeId]: The UUID of the node that last modified this record. Used for provenance
 *   tracking and debugging merge conflicts.
 * - [isDeleted]: Soft-delete tombstone. In the LWW-Element-Set model, a "remove" operation
 *   sets this to true with a newer timestamp rather than physically deleting the row.
 */
@Entity(
    tableName = "sos_requests",
    indices = [
        Index(value = ["lastUpdatedTimestamp"]),
        Index(value = ["isSyncedToCloud"]),
        Index(value = ["createdByNodeId"])
    ]
)
data class SOSRequestEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "injuryType")
    val injuryType: String,

    @ColumnInfo(name = "severity")
    val severity: Int,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "createdByNodeId")
    val createdByNodeId: String,

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
