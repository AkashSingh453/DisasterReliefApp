package com.disasterrelief.app.data.repository

import com.disasterrelief.app.data.local.dao.SOSRequestDao
import com.disasterrelief.app.data.local.entity.SOSRequestEntity
import com.disasterrelief.app.domain.model.SOSRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for SOS request data, serving as the single point of access for
 * the domain/presentation layers.
 *
 * Responsibilities:
 * - Maps between Room entities and domain models.
 * - Generates UUIDs and timestamps for new records.
 * - Exposes reactive [Flow] streams for UI observation.
 */
@Singleton
class SOSRepository @Inject constructor(
    private val sosRequestDao: SOSRequestDao
) {

    /**
     * Observes all non-deleted SOS requests as domain models.
     */
    fun observeAll(): Flow<List<SOSRequest>> {
        return sosRequestDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Observes the count of active SOS requests.
     */
    fun observeCount(): Flow<Int> = sosRequestDao.observeCount()

    /**
     * Creates a new SOS request, persists it locally, and returns the generated ID.
     *
     * @param localNodeId The UUID of this device's node (used as both createdByNodeId and originNodeId).
     * @param injuryType The type of injury reported.
     * @param severity Severity level (1-5).
     * @param latitude GPS latitude.
     * @param longitude GPS longitude.
     * @param description Free-text description of the emergency.
     * @return The generated UUID of the new SOS request.
     */
    suspend fun createSOSRequest(
        localNodeId: String,
        injuryType: String,
        severity: Int,
        latitude: Double,
        longitude: Double,
        description: String
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val entity = SOSRequestEntity(
            id = id,
            injuryType = injuryType,
            severity = severity,
            latitude = latitude,
            longitude = longitude,
            description = description,
            createdByNodeId = localNodeId,
            lastUpdatedTimestamp = now,
            originNodeId = localNodeId,
            isDeleted = false,
            isSyncedToCloud = false
        )

        sosRequestDao.insertReplace(entity)
        return id
    }

    /**
     * Soft-deletes an SOS request by marking it as deleted with a new timestamp.
     * The deletion propagates across the mesh via the CRDT sync engine.
     */
    suspend fun softDelete(id: String, localNodeId: String) {
        sosRequestDao.softDelete(
            id = id,
            timestamp = System.currentTimeMillis(),
            nodeId = localNodeId
        )
    }

    suspend fun getById(id: String): SOSRequest? {
        return sosRequestDao.getById(id)?.toDomain()
    }

    // ── Mapping ──

    private fun SOSRequestEntity.toDomain() = SOSRequest(
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
}
