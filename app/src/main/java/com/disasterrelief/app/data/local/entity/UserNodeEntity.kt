package com.disasterrelief.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a node (device/user) in the mesh network.
 *
 * Each device in the mesh generates a permanent [id] on first launch and
 * advertises its [displayName] and last known GPS coordinates.
 * The LWW merge ensures the freshest node state propagates across the mesh.
 */
@Entity(
    tableName = "user_nodes",
    indices = [
        Index(value = ["lastUpdatedTimestamp"]),
        Index(value = ["isOnline"])
    ]
)
data class UserNodeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "displayName")
    val displayName: String,

    @ColumnInfo(name = "role")
    val role: String = "VOLUNTEER",

    @ColumnInfo(name = "lastKnownLatitude")
    val lastKnownLatitude: Double? = null,

    @ColumnInfo(name = "lastKnownLongitude")
    val lastKnownLongitude: Double? = null,

    // ── CRDT Metadata ──

    @ColumnInfo(name = "lastUpdatedTimestamp")
    val lastUpdatedTimestamp: Long,

    @ColumnInfo(name = "originNodeId")
    val originNodeId: String,

    @ColumnInfo(name = "isDeleted")
    val isDeleted: Boolean = false,

    // ── Mesh Status ──

    @ColumnInfo(name = "isOnline")
    val isOnline: Boolean = false
)
