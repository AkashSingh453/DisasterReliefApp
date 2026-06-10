package com.disasterrelief.app.domain.usecase

import com.disasterrelief.app.data.repository.SOSRepository
import javax.inject.Inject

/**
 * Use case for creating a new SOS distress request.
 * Encapsulates the business logic of SOS creation and returns the generated ID
 * for subsequent mesh broadcast.
 */
class CreateSOSRequestUseCase @Inject constructor(
    private val sosRepository: SOSRepository
) {
    suspend operator fun invoke(
        localNodeId: String,
        injuryType: String,
        severity: Int,
        latitude: Double,
        longitude: Double,
        description: String
    ): String {
        return sosRepository.createSOSRequest(
            localNodeId = localNodeId,
            injuryType = injuryType,
            severity = severity,
            latitude = latitude,
            longitude = longitude,
            description = description
        )
    }
}
