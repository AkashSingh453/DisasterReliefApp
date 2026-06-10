package com.disasterrelief.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.disasterrelief.app.data.local.dao.MessageDao
import com.disasterrelief.app.data.local.dao.SOSRequestDao
import com.disasterrelief.app.data.local.dao.UserNodeDao
import com.disasterrelief.app.data.local.entity.MessageEntity
import com.disasterrelief.app.data.local.entity.SOSRequestEntity
import com.disasterrelief.app.data.local.entity.UserNodeEntity

/**
 * Room database definition for the Disaster Relief application.
 *
 * Contains three tables that form the CRDT-enabled local-first data store:
 * - [SOSRequestEntity]: Distress signals with injury/severity/location data.
 * - [MessageEntity]: Mesh chat messages.
 * - [UserNodeEntity]: Registry of all known mesh participants.
 *
 * Schema is exported to aid migration testing and version tracking.
 */
@Database(
    entities = [
        SOSRequestEntity::class,
        MessageEntity::class,
        UserNodeEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sosRequestDao(): SOSRequestDao
    abstract fun messageDao(): MessageDao
    abstract fun userNodeDao(): UserNodeDao
}
