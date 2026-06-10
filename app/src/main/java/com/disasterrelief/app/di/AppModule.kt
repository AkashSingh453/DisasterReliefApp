package com.disasterrelief.app.di

import android.content.Context
import com.disasterrelief.app.location.DefaultLocationManager
import com.disasterrelief.app.location.LocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Application-level Hilt module.
 *
 * Repositories, use cases, and the CrdtSyncEngine all use constructor injection
 * with @Singleton and @Inject, so they are automatically provided without
 * explicit @Provides methods.
 *
 * This module serves as an extension point for application-scoped bindings
 * that may require manual construction in the future.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLocationManager(
        @ApplicationContext context: Context
    ): LocationManager {
        return DefaultLocationManager(context)
    }
}
