package com.disasterrelief.app.data.repository

import com.disasterrelief.app.data.local.dao.UserNodeDao
import com.disasterrelief.app.data.local.entity.UserNodeEntity
import com.disasterrelief.app.domain.model.UserNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for user/device node records in the mesh.
 */
@Singleton
class UserNodeRepository @Inject constructor(
    private val userNodeDao: UserNodeDao
) {

    fun observeAll(): Flow<List<UserNode>> {
        return userNodeDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun observeOnlineNodes(): Flow<List<UserNode>> {
        return userNodeDao.observeOnlineNodes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun observeOnlineCount(): Flow<Int> = userNodeDao.observeOnlineCount()

    suspend fun getById(id: String): UserNode? {
        return userNodeDao.getById(id)?.toDomain()
    }

    /**
     * Registers or updates the local node's identity in the database.
     */
    suspend fun registerLocalNode(
        nodeId: String,
        displayName: String,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        val now = System.currentTimeMillis()
        val entity = UserNodeEntity(
            id = nodeId,
            displayName = displayName,
            lastKnownLatitude = latitude,
            lastKnownLongitude = longitude,
            lastUpdatedTimestamp = now,
            originNodeId = nodeId,
            isDeleted = false,
            isOnline = true
        )
        userNodeDao.insertReplace(entity)
    }

    suspend fun updateOnlineStatus(nodeId: String, isOnline: Boolean) {
        userNodeDao.updateOnlineStatus(nodeId, isOnline, System.currentTimeMillis())
    }

    suspend fun updateLocation(nodeId: String, latitude: Double, longitude: Double) {
        userNodeDao.updateLocation(nodeId, latitude, longitude, System.currentTimeMillis())
    }

    // ── Mapping ──

    private fun UserNodeEntity.toDomain() = UserNode(
        id = id,
        displayName = displayName,
        lastKnownLatitude = lastKnownLatitude,
        lastKnownLongitude = lastKnownLongitude,
        lastUpdatedTimestamp = lastUpdatedTimestamp,
        originNodeId = originNodeId,
        isDeleted = isDeleted,
        isOnline = isOnline
    )
}
