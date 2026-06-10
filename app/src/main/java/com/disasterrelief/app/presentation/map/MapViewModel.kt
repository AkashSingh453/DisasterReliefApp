package com.disasterrelief.app.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disasterrelief.app.domain.model.SOSRequest
import com.disasterrelief.app.domain.usecase.ObserveSOSRequestsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the offline map screen.
 *
 * Provides SOS request data for pin rendering and manages the map's center coordinates.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    observeSOSRequestsUseCase: ObserveSOSRequestsUseCase
) : ViewModel() {

    val sosRequests: StateFlow<List<SOSRequest>> = observeSOSRequestsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Map center latitude. Defaults to 0.0 and updates to the first SOS coordinate
     * or user's last known location.
     */
    private val _mapCenterLat = MutableStateFlow(28.6139) // Default: New Delhi
    val mapCenterLat: StateFlow<Double> = _mapCenterLat.asStateFlow()

    private val _mapCenterLon = MutableStateFlow(77.2090)
    val mapCenterLon: StateFlow<Double> = _mapCenterLon.asStateFlow()

    private val _zoomLevel = MutableStateFlow(12.0)
    val zoomLevel: StateFlow<Double> = _zoomLevel.asStateFlow()

    fun updateMapCenter(lat: Double, lon: Double) {
        _mapCenterLat.value = lat
        _mapCenterLon.value = lon
    }

    fun updateZoomLevel(zoom: Double) {
        _zoomLevel.value = zoom
    }
}
