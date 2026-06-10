package com.disasterrelief.app.domain.usecase

import com.disasterrelief.app.data.repository.SOSRepository
import com.disasterrelief.app.domain.model.SOSRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing all active SOS requests as a reactive stream.
 */
class ObserveSOSRequestsUseCase @Inject constructor(
    private val sosRepository: SOSRepository
) {
    operator fun invoke(): Flow<List<SOSRequest>> {
        return sosRepository.observeAll()
    }
}
