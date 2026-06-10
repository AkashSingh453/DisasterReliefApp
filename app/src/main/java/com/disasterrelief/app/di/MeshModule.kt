package com.disasterrelief.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for mesh networking bindings.
 *
 * [com.disasterrelief.app.mesh.MeshNetworkManager] and [com.disasterrelief.app.mesh.MeshPayloadHandler]
 * use constructor injection with @Singleton and @Inject, so they are automatically
 * provided by Hilt without explicit @Provides methods.
 *
 * This module exists as an extension point for mesh-specific configuration
 * bindings if needed in the future (e.g., injectable service ID, strategy config).
 */
@Module
@InstallIn(SingletonComponent::class)
object MeshModule
