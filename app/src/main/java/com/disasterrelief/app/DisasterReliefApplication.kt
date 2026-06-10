package com.disasterrelief.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

/**
 * Application entry point for the Disaster Relief P2P mesh system.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation and
 * establish the application-level dependency container.
 */
@HiltAndroidApp
class DisasterReliefApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        configureOsmDroid()
    }

    /**
     * Initializes OSMDroid configuration for offline map rendering.
     * Sets the user agent to comply with OpenStreetMap usage policies and
     * configures the tile cache to use the app's private internal storage,
     * avoiding external storage permission requirements on modern Android.
     */
    private fun configureOsmDroid() {
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = packageName
        osmConfig.osmdroidTileCache = cacheDir.resolve("osmdroid_tiles")
    }
}
