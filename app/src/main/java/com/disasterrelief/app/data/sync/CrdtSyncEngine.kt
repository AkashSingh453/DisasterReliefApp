package com.disasterrelief.app.data.sync

import com.disasterrelief.app.data.local.dao.MessageDao
import com.disasterrelief.app.data.local.dao.SOSRequestDao
import com.disasterrelief.app.data.local.dao.UserNodeDao
import com.disasterrelief.app.data.local.entity.MessageEntity
import com.disasterrelief.app.data.local.entity.SOSRequestEntity
import com.disasterrelief.app.data.local.entity.UserNodeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central CRDT synchronization engine.
 *
 * Responsible for two core operations:
 * 1. **Serialize delta**: Query all records modified since a given timestamp from the local Room
 *    database, convert them to compact transport DTOs, and serialize to a JSON [SyncPayload].
 * 2. **Merge delta**: Deserialize an incoming [SyncPayload] (from mesh peers or cloud), convert
 *    DTOs back to Room entities, and execute LWW merges via the DAOs.
 *
 * The engine is stateless — it delegates all persistence to the DAOs and all conflict resolution
 * to the LWW merge queries defined in each DAO.
 *
 * Data flow:
 * ```
 * Local Write → Room DB → serializeDelta() → JSON bytes → Mesh/Cloud transport
 * Incoming bytes → deserialize → SyncPayload → mergeDelta() → LWW DAOs → Room DB → UI refresh
 * ```
 */
@Singleton
class CrdtSyncEngine @Inject constructor(
    private val sosRequestDao: SOSRequestDao,
    private val messageDao: MessageDao,
    private val userNodeDao: UserNodeDao
) {



    // ══════════════════════════════════════════════════════════════════════
    //  SERIALIZE: Room DB → SyncPayload (for outbound transmission)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Builds a [SyncPayload] containing all records modified after [sinceTimestamp].
     *
     * @param localNodeId The UUID of this device's node.
     * @param sinceTimestamp Epoch ms. Only records with `lastUpdatedTimestamp > sinceTimestamp`
     *        are included. Pass 0 to export the full database state.
     * @return A [SyncPayload] ready for serialization and transmission.
     */
    suspend fun serializeDelta(localNodeId: String, sinceTimestamp: Long = 0L): SyncPayload {
        return withContext(Dispatchers.IO) {
            val sosEntities = sosRequestDao.getModifiedSince(sinceTimestamp)
            val messageEntities = messageDao.getModifiedSince(sinceTimestamp)
            val userNodeEntities = userNodeDao.getModifiedSince(sinceTimestamp)

            SyncPayload(
                sourceNodeId = localNodeId,
                timestamp = System.currentTimeMillis(),
                sosRequests = sosEntities.map { it.toDto() },
                messages = messageEntities.map { it.toDto() },
                userNodes = userNodeEntities.map { it.toDto() }
            )
        }
    }



    // ══════════════════════════════════════════════════════════════════════
    //  MERGE: SyncPayload → Room DB (for inbound data from peers/cloud)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Merges an incoming [SyncPayload] into the local Room database using LWW semantics.
     *
     * Each record type is batch-merged via the corresponding DAO's `lwwMergeBatch()`,
     * which runs inside a Room @Transaction for atomicity.
     *
     * @param payload The incoming delta payload from a mesh peer or cloud server.
     */
    suspend fun mergeDelta(payload: SyncPayload): Boolean {
        return withContext(Dispatchers.IO) {
            var changed = false
            // Merge SOS requests
            if (payload.sosRequests.isNotEmpty()) {
                val entities = payload.sosRequests.map { it.toEntity() }
                if (sosRequestDao.lwwMergeBatch(entities)) changed = true
            }

            // Merge messages
            if (payload.messages.isNotEmpty()) {
                val entities = payload.messages.map { it.toEntity() }
                if (messageDao.lwwMergeBatch(entities)) changed = true
            }

            // Merge user nodes
            if (payload.userNodes.isNotEmpty()) {
                val entities = payload.userNodes.map { it.toEntity() }
                if (userNodeDao.lwwMergeBatch(entities)) changed = true
            }
            
            changed
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ENTITY ↔ DTO MAPPING
    // ══════════════════════════════════════════════════════════════════════

    // ── SOS Request ──

    private fun SOSRequestEntity.toDto() = SOSRequestDto(
        id = id,
        injuryType = injuryType,
        severity = severity,
        latitude = latitude,
        longitude = longitude,
        description = description,
        createdByNodeId = createdByNodeId,
        lastUpdatedTimestamp = lastUpdatedTimestamp,
        originNodeId = originNodeId,
        isDeleted = isDeleted
    )

    private fun SOSRequestDto.toEntity() = SOSRequestEntity(
        id = id,
        injuryType = injuryType,
        severity = severity,
        latitude = latitude,
        longitude = longitude,
        description = description,
        createdByNodeId = createdByNodeId,
        lastUpdatedTimestamp = lastUpdatedTimestamp,
        originNodeId = originNodeId,
        isDeleted = isDeleted,
        isSyncedToCloud = false
    )

    // ── Message ──

    private fun MessageEntity.toDto() = MessageDto(
        id = id,
        senderNodeId = senderNodeId,
        senderName = senderName,
        content = content,
        recipientId = recipientId,
        isAlert = isAlert,
        alertPriority = alertPriority,
        lastUpdatedTimestamp = lastUpdatedTimestamp,
        originNodeId = originNodeId,
        isDeleted = isDeleted
    )

    private fun MessageDto.toEntity() = MessageEntity(
        id = id,
        senderNodeId = senderNodeId,
        senderName = senderName,
        content = content,
        recipientId = recipientId,
        isAlert = isAlert,
        alertPriority = alertPriority,
        lastUpdatedTimestamp = lastUpdatedTimestamp,
        originNodeId = originNodeId,
        isDeleted = isDeleted,
        isSyncedToCloud = false
    )

    // ── User Node ──

    private fun UserNodeEntity.toDto() = UserNodeDto(
        id = id,
        displayName = displayName,
        role = role,
        lastKnownLatitude = lastKnownLatitude,
        lastKnownLongitude = lastKnownLongitude,
        lastUpdatedTimestamp = lastUpdatedTimestamp,
        originNodeId = originNodeId,
        isDeleted = isDeleted,
        isOnline = isOnline
    )

    private fun UserNodeDto.toEntity() = UserNodeEntity(
        id = id,
        displayName = displayName,
        role = role,
        lastKnownLatitude = lastKnownLatitude,
        lastKnownLongitude = lastKnownLongitude,
        lastUpdatedTimestamp = lastUpdatedTimestamp,
        originNodeId = originNodeId,
        isDeleted = isDeleted,
        isOnline = isOnline
    )
}
