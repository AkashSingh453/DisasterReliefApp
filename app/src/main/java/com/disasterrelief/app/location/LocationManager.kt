package com.disasterrelief.app.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

data class LocationResult(
    val latitude: Double,
    val longitude: Double
)

interface LocationManager {
    /**
     * Fetches the current location using FusedLocationProviderClient.
     * Returns null if location cannot be fetched (e.g., GPS is off, or permissions missing).
     */
    suspend fun getCurrentLocation(): LocationResult?
}

class DefaultLocationManager(
    private val context: Context
) : LocationManager {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission") // Caller is responsible for ensuring permissions are granted
    override suspend fun getCurrentLocation(): LocationResult? {
        return try {
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()

            if (location != null) {
                LocationResult(latitude = location.latitude, longitude = location.longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
