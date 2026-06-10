package com.disasterrelief.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.disasterrelief.app.data.local.entity.SOSRequestEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [SOSRequestEntity] with Last-Writer-Wins (LWW) conflict resolution.
 *
 * The LWW algorithm is implemented via [lwwMerge]: when a record arrives from a peer node
 * (via mesh or cloud sync), it is only written if its [SOSRequestEntity.lastUpdatedTimestamp]
 * is strictly greater than the existing record's timestamp. This ensures that the most recent
 * mutation always wins, regardless of which node originated it.
 *
 * All merge operations run inside a [Transaction] to guarantee atomicity — the SELECT and
 * conditional INSERT/REPLACE execute as a single indivisible unit, preventing race conditions
 * between concurrent mesh payload arrivals.
 */
@Dao
abstract class SOSRequestDao {

    // ── Reactive Queries ──

    /**
     * Observes all non-deleted SOS requests, ordered by most recent first.
     * Emits a new list whenever the underlying table changes.
     */
    @Query("SELECT * FROM sos_requests WHERE isDeleted = 0 ORDER BY lastUpdatedTimestamp DESC")
    abstract fun observeAll(): Flow<List<SOSRequestEntity>>

    /**
     * Observes a single SOS request by its globally unique ID.
     */
    @Query("SELECT * FROM sos_requests WHERE id = :id")
    abstract fun observeById(id: String): Flow<SOSRequestEntity?>

    // ── Synchronous Queries ──

    @Query("SELECT * FROM sos_requests WHERE id = :id")
    abstract suspend fun getById(id: String): SOSRequestEntity?

    @Query("SELECT * FROM sos_requests WHERE isDeleted = 0")
    abstract suspend fun getAll(): List<SOSRequestEntity>

    /**
     * Returns all records modified after [sinceTimestamp].
     * Used by [com.disasterrelief.app.data.sync.CrdtSyncEngine] to build delta payloads
     * for mesh broadcast.
     */
    @Query("SELECT * FROM sos_requests WHERE lastUpdatedTimestamp > :sinceTimestamp")
    abstract suspend fun getModifiedSince(sinceTimestamp: Long): List<SOSRequestEntity>

    /**
     * Returns records that have not yet been uploaded to the cloud backend.
     * Used by [com.disasterrelief.app.data.sync.CloudSyncManager] to batch uploads.
     */
    @Query("SELECT * FROM sos_requests WHERE isSyncedToCloud = 0")
    abstract suspend fun getUnsyncedToCloud(): List<SOSRequestEntity>

    @Query("SELECT COUNT(*) FROM sos_requests WHERE isDeleted = 0")
    abstract fun observeCount(): Flow<Int>

    // ── Write Operations ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReplace(entity: SOSRequestEntity)

    /**
     * Last-Writer-Wins merge for a single entity.
     *
     * Algorithm:
     * 1. Attempt to load the existing record by primary key.
     * 2. If no record exists → insert directly (first time this ID is seen on this node).
     * 3. If a record exists → compare timestamps:
     *    - Incoming timestamp > existing → replace with incoming (incoming wins).
     *    - Incoming timestamp <= existing → discard (local version is newer or equal).
     * 4. On successful write, mark [isSyncedToCloud] = false to trigger future cloud upload.
     */
    @Transaction
    open suspend fun lwwMerge(entity: SOSRequestEntity): Boolean {
        val existing = getById(entity.id)
        if (existing == null || entity.lastUpdatedTimestamp > existing.lastUpdatedTimestamp) {
            insertReplace(entity.copy(isSyncedToCloud = false))
            return true
        }
        return false
    }

    /**
     * Batch LWW merge for incoming sync payloads.
     * Runs the entire batch inside a single transaction to minimize disk I/O.
     */
    @Transaction
    open suspend fun lwwMergeBatch(entities: List<SOSRequestEntity>): Boolean {
        var changed = false
        for (entity in entities) {
            if (lwwMerge(entity)) {
                changed = true
            }
        }
        return changed
    }

    /**
     * Marks all currently unsynced records as synced after a successful cloud upload.
     */
    @Query("UPDATE sos_requests SET isSyncedToCloud = 1 WHERE isSyncedToCloud = 0")
    abstract suspend fun markAllSyncedToCloud()

    @Query("UPDATE sos_requests SET isSyncedToCloud = 1 WHERE id IN (:ids)")
    abstract suspend fun markSyncedToCloud(ids: List<String>)

    /**
     * Soft-delete: sets [isDeleted] = true with a new timestamp so the deletion
     * propagates through the LWW merge across the mesh.
     */
    @Query(
        """
        UPDATE sos_requests 
        SET isDeleted = 1, 
            lastUpdatedTimestamp = :timestamp, 
            originNodeId = :nodeId, 
            isSyncedToCloud = 0 
        WHERE id = :id
        """
    )
    abstract suspend fun softDelete(id: String, timestamp: Long, nodeId: String)
}
