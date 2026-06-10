package com.disasterrelief.app.domain.usecase

import com.disasterrelief.app.domain.model.Message
import com.disasterrelief.app.data.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveInboxUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(localNodeId: String): Flow<List<Message>> {
        return messageRepository.observeAllDirectMessages(localNodeId).map { messages ->
            // The query orders by lastUpdatedTimestamp DESC, so the first message for each peer is the latest.
            messages.groupBy { 
                if (it.senderNodeId == localNodeId) it.recipientId!! else it.senderNodeId 
            }.map { (peerId, msgs) ->
                msgs.first() // Get the latest message in this conversation
            }
        }
    }
}
