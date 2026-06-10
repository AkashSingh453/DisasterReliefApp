package com.disasterrelief.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.disasterrelief.app.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [MessageEntity] with LWW conflict resolution.
 *
 * Chat messages are inherently append-only, but the LWW metadata allows edits
 * and deletions to propagate correctly across the mesh. The merge logic is
 * identical to [SOSRequestDao.lwwMerge].
 */
@Dao
abstract class MessageDao {

    // ── Reactive Queries ──

    @Query("SELECT * FROM messages WHERE isDeleted = 0 AND recipientId IS NULL ORDER BY lastUpdatedTimestamp ASC")
    abstract fun observeGlobalMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE isDeleted = 0 AND ((senderNodeId = :localNodeId AND recipientId = :peerId) OR (senderNodeId = :peerId AND recipientId = :localNodeId)) ORDER BY lastUpdatedTimestamp ASC")
    abstract fun observeDirectMessages(localNodeId: String, peerId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE isDeleted = 0 AND recipientId IS NOT NULL AND (senderNodeId = :localNodeId OR recipientId = :localNodeId) ORDER BY lastUpdatedTimestamp DESC")
    abstract fun observeAllDirectMessages(localNodeId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    abstract fun observeById(id: String): Flow<MessageEntity?>

    // ── Synchronous Queries ──

    @Query("SELECT * FROM messages WHERE id = :id")
    abstract suspend fun getById(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE isDeleted = 0")
    abstract suspend fun getAll(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE lastUpdatedTimestamp > :sinceTimestamp")
    abstract suspend fun getModifiedSince(sinceTimestamp: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE isSyncedToCloud = 0")
    abstract suspend fun getUnsyncedToCloud(): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE isDeleted = 0")
    abstract fun observeCount(): Flow<Int>

    // ── Write Operations ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReplace(entity: MessageEntity)

    /**
     * LWW merge: incoming message replaces local only if its timestamp is newer.
     */
    @Transaction
    open suspend fun lwwMerge(entity: MessageEntity): Boolean {
        val existing = getById(entity.id)
        if (existing == null || entity.lastUpdatedTimestamp > existing.lastUpdatedTimestamp) {
            insertReplace(entity.copy(isSyncedToCloud = false))
            return true
        }
        return false
    }

    @Transaction
    open suspend fun lwwMergeBatch(entities: List<MessageEntity>): Boolean {
        var changed = false
        for (entity in entities) {
            if (lwwMerge(entity)) {
                changed = true
            }
        }
        return changed
    }

    @Query("UPDATE messages SET isSyncedToCloud = 1 WHERE isSyncedToCloud = 0")
    abstract suspend fun markAllSyncedToCloud()

    @Query("UPDATE messages SET isSyncedToCloud = 1 WHERE id IN (:ids)")
    abstract suspend fun markSyncedToCloud(ids: List<String>)

    @Query(
        """
        UPDATE messages 
        SET isDeleted = 1, 
            lastUpdatedTimestamp = :timestamp, 
            originNodeId = :nodeId, 
            isSyncedToCloud = 0 
        WHERE id = :id
        """
    )
    abstract suspend fun softDelete(id: String, timestamp: Long, nodeId: String)
}
