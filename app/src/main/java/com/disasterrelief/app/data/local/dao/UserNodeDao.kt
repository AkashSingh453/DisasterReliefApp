package com.disasterrelief.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.disasterrelief.app.data.local.entity.UserNodeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [UserNodeEntity] with LWW conflict resolution.
 *
 * Tracks all known nodes in the mesh. When a peer connects and exchanges
 * its identity, the node record is merged using LWW semantics. This allows
 * the mesh to maintain a shared directory of all participants.
 */
@Dao
abstract class UserNodeDao {

    // ── Reactive Queries ──

    @Query("SELECT * FROM user_nodes WHERE isDeleted = 0 ORDER BY displayName ASC")
    abstract fun observeAll(): Flow<List<UserNodeEntity>>

    @Query("SELECT * FROM user_nodes WHERE isOnline = 1 AND isDeleted = 0")
    abstract fun observeOnlineNodes(): Flow<List<UserNodeEntity>>

    @Query("SELECT * FROM user_nodes WHERE id = :id")
    abstract fun observeById(id: String): Flow<UserNodeEntity?>

    // ── Synchronous Queries ──

    @Query("SELECT * FROM user_nodes WHERE id = :id")
    abstract suspend fun getById(id: String): UserNodeEntity?

    @Query("SELECT * FROM user_nodes WHERE isDeleted = 0")
    abstract suspend fun getAll(): List<UserNodeEntity>

    @Query("SELECT * FROM user_nodes WHERE lastUpdatedTimestamp > :sinceTimestamp")
    abstract suspend fun getModifiedSince(sinceTimestamp: Long): List<UserNodeEntity>

    @Query("SELECT COUNT(*) FROM user_nodes WHERE isOnline = 1 AND isDeleted = 0")
    abstract fun observeOnlineCount(): Flow<Int>

    // ── Write Operations ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReplace(entity: UserNodeEntity)

    /**
     * LWW merge for user node records.
     */
    @Transaction
    open suspend fun lwwMerge(entity: UserNodeEntity): Boolean {
        val existing = getById(entity.id)
        if (existing == null || entity.lastUpdatedTimestamp > existing.lastUpdatedTimestamp) {
            insertReplace(entity)
            return true
        }
        return false
    }

    @Transaction
    open suspend fun lwwMergeBatch(entities: List<UserNodeEntity>): Boolean {
        var changed = false
        for (entity in entities) {
            if (lwwMerge(entity)) {
                changed = true
            }
        }
        return changed
    }

    /**
     * Marks a node as online/offline. Used when mesh connections are established or lost.
     */
    @Query("UPDATE user_nodes SET isOnline = :isOnline, lastUpdatedTimestamp = :timestamp WHERE id = :id")
    abstract suspend fun updateOnlineStatus(id: String, isOnline: Boolean, timestamp: Long)

    /**
     * Updates the node's last known GPS coordinates.
     */
    @Query(
        """
        UPDATE user_nodes 
        SET lastKnownLatitude = :latitude, 
            lastKnownLongitude = :longitude, 
            lastUpdatedTimestamp = :timestamp 
        WHERE id = :id
        """
    )
    abstract suspend fun updateLocation(id: String, latitude: Double, longitude: Double, timestamp: Long)
}
