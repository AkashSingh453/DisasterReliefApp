package com.disasterrelief.app.domain.usecase

import com.disasterrelief.app.domain.model.Message
import com.disasterrelief.app.data.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDirectMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(localNodeId: String, peerId: String): Flow<List<Message>> {
        return messageRepository.observeDirectMessages(localNodeId, peerId)
    }
}
