package com.disasterrelief.app.data.repository

import com.disasterrelief.app.data.local.dao.MessageDao
import com.disasterrelief.app.data.local.entity.MessageEntity
import com.disasterrelief.app.domain.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for mesh chat messages.
 */
@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao
) {

    /**
     * Observes all non-deleted messages, ordered by timestamp ascending (chat order).
     */
    fun observeGlobalMessages(): Flow<List<Message>> {
        return messageDao.observeGlobalMessages().map { entities ->
            entities.map { entity ->
                Message(
                    id = entity.id,
                    senderNodeId = entity.senderNodeId,
                    senderName = entity.senderName,
                    content = entity.content,
                    recipientId = entity.recipientId,
                    isAlert = entity.isAlert,
                    alertPriority = entity.alertPriority,
                    lastUpdatedTimestamp = entity.lastUpdatedTimestamp,
                    originNodeId = entity.originNodeId,
                    isDeleted = entity.isDeleted
                )
            }
        }
    }

    fun observeDirectMessages(localNodeId: String, peerId: String): Flow<List<Message>> {
        return messageDao.observeDirectMessages(localNodeId, peerId).map { entities ->
            entities.map { entity ->
                Message(
                    id = entity.id,
                    senderNodeId = entity.senderNodeId,
                    senderName = entity.senderName,
                    content = entity.content,
                    recipientId = entity.recipientId,
                    isAlert = entity.isAlert,
                    alertPriority = entity.alertPriority,
                    lastUpdatedTimestamp = entity.lastUpdatedTimestamp,
                    originNodeId = entity.originNodeId,
                    isDeleted = entity.isDeleted
                )
            }
        }
    }

    fun observeCount(): Flow<Int> = messageDao.observeCount()

    fun observeAllDirectMessages(localNodeId: String): Flow<List<Message>> {
        return messageDao.observeAllDirectMessages(localNodeId).map { entities ->
            entities.map { entity ->
                Message(
                    id = entity.id,
                    senderNodeId = entity.senderNodeId,
                    senderName = entity.senderName,
                    content = entity.content,
                    recipientId = entity.recipientId,
                    isAlert = entity.isAlert,
                    alertPriority = entity.alertPriority,
                    lastUpdatedTimestamp = entity.lastUpdatedTimestamp,
                    originNodeId = entity.originNodeId,
                    isDeleted = entity.isDeleted
                )
            }
        }
    }

    /**
     * Sends a new chat message. Returns the generated UUID.and returns the generated ID.
     *
     * @param localNodeId This device's permanent node UUID.
     * @param senderName Display name of the sender.
     * @param content Message text content.
     * @return The generated UUID of the new message.
     */
    suspend fun sendMessage(
        localNodeId: String,
        senderName: String,
        content: String,
        recipientId: String? = null,
        isAlert: Boolean = false,
        alertPriority: Int = 0
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val entity = MessageEntity(
            id = id,
            senderNodeId = localNodeId,
            senderName = senderName,
            content = content,
            recipientId = recipientId,
            isAlert = isAlert,
            alertPriority = alertPriority,
            lastUpdatedTimestamp = now,
            originNodeId = localNodeId,
            isDeleted = false,
            isSyncedToCloud = false
        )

        messageDao.insertReplace(entity)
        return id
    }

    suspend fun softDelete(id: String, localNodeId: String) {
        messageDao.softDelete(
            id = id,
            timestamp = System.currentTimeMillis(),
            nodeId = localNodeId
        )
    }

    // ── Mapping ──

    private fun MessageEntity.toDomain() = Message(
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
}
