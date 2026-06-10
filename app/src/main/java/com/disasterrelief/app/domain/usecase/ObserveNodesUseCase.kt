package com.disasterrelief.app.domain.usecase

import com.disasterrelief.app.data.local.dao.UserNodeDao
import com.disasterrelief.app.data.local.entity.UserNodeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNodesUseCase @Inject constructor(
    private val userNodeDao: UserNodeDao
) {
    operator fun invoke(): Flow<List<UserNodeEntity>> {
        return userNodeDao.observeAll()
    }
}
