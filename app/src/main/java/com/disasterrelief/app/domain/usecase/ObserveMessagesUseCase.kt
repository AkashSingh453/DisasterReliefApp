package com.disasterrelief.app.domain.usecase

import com.disasterrelief.app.data.repository.MessageRepository
import com.disasterrelief.app.domain.model.Message
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing all chat messages as a reactive stream.
 */
class ObserveMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(): Flow<List<Message>> {
        return messageRepository.observeGlobalMessages()
    }
}
