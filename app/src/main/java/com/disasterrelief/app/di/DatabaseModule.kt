package com.disasterrelief.app.di

import android.content.Context
import androidx.room.Room
import com.disasterrelief.app.data.local.AppDatabase
import com.disasterrelief.app.data.local.dao.MessageDao
import com.disasterrelief.app.data.local.dao.SOSRequestDao
import com.disasterrelief.app.data.local.dao.UserNodeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing the Room database and all DAO instances.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "disaster_relief_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSOSRequestDao(database: AppDatabase): SOSRequestDao {
        return database.sosRequestDao()
    }

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    fun provideUserNodeDao(database: AppDatabase): UserNodeDao {
        return database.userNodeDao()
    }
}
