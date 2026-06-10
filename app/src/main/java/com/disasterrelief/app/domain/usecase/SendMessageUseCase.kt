package com.disasterrelief.app.domain.usecase

import com.disasterrelief.app.data.repository.MessageRepository
import javax.inject.Inject

/**
 * Use case for sending a chat message in the mesh network.
 */
class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        localNodeId: String,
        senderName: String,
        content: String,
        recipientId: String? = null,
        isAlert: Boolean = false,
        alertPriority: Int = 0
    ): String {
        return messageRepository.sendMessage(
            localNodeId = localNodeId,
            senderName = senderName,
            content = content,
            recipientId = recipientId,
            isAlert = isAlert,
            alertPriority = alertPriority
        )
    }
}
