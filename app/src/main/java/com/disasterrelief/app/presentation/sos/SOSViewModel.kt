package com.disasterrelief.app.presentation.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disasterrelief.app.data.sync.CloudSyncManager
import com.disasterrelief.app.data.sync.CrdtSyncEngine
import com.disasterrelief.app.domain.usecase.CreateSOSRequestUseCase
import com.disasterrelief.app.mesh.MeshNetworkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.disasterrelief.app.data.repository.SOSRepository
import com.disasterrelief.app.domain.usecase.ObserveSOSRequestsUseCase
import com.disasterrelief.app.location.LocationManager
import com.disasterrelief.app.domain.model.SOSRequest
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * Form state for the SOS creation screen.
 */
data class SOSFormState(
    val injuryType: String = "",
    val severity: Int = 3,
    val latitude: String = "",
    val longitude: String = "",
    val description: String = "",
    val isSubmitting: Boolean = false,
    val isFetchingLocation: Boolean = false,
    val selectedInjuryIndex: Int = -1
)

/**
 * One-shot UI events from the SOS ViewModel.
 */
sealed class SOSUiEvent {
    data object SubmitSuccess : SOSUiEvent()
    data class SubmitError(val message: String) : SOSUiEvent()
}

val INJURY_TYPES = listOf(
    "Fracture",
    "Burn",
    "Bleeding / Laceration",
    "Head Injury",
    "Crush Injury",
    "Breathing Difficulty",
    "Heart Attack / Cardiac",
    "Drowning",
    "Hypothermia",
    "Other"
)

@HiltViewModel
class SOSViewModel @Inject constructor(
    private val createSOSRequestUseCase: CreateSOSRequestUseCase,
    private val observeSOSRequestsUseCase: ObserveSOSRequestsUseCase,
    private val sosRepository: SOSRepository,
    private val meshNetworkManager: MeshNetworkManager,
    private val crdtSyncEngine: CrdtSyncEngine,
    private val cloudSyncManager: CloudSyncManager,
    private val locationManager: LocationManager
) : ViewModel() {

    private val _formState = MutableStateFlow(SOSFormState())
    val formState: StateFlow<SOSFormState> = _formState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<SOSUiEvent>(extraBufferCapacity = 1)
    val uiEvents: SharedFlow<SOSUiEvent> = _uiEvents.asSharedFlow()

    val activeSosRequests: StateFlow<List<SOSRequest>> = observeSOSRequestsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Retrieve the local node ID from shared preferences (set in DashboardViewModel)
    private var localNodeId: String = ""

    fun setLocalNodeId(nodeId: String) {
        localNodeId = nodeId
    }

    fun getLocalNodeId(): String = localNodeId

    fun updateInjuryType(type: String, index: Int) {
        _formState.update { it.copy(injuryType = type, selectedInjuryIndex = index) }
    }

    fun updateSeverity(severity: Int) {
        _formState.update { it.copy(severity = severity) }
    }

    fun updateLatitude(lat: String) {
        _formState.update { it.copy(latitude = lat) }
    }

    fun updateLongitude(lon: String) {
        _formState.update { it.copy(longitude = lon) }
    }

    fun updateDescription(desc: String) {
        _formState.update { it.copy(description = desc) }
    }

    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _formState.update { it.copy(isFetchingLocation = true) }
            val location = locationManager.getCurrentLocation()
            if (location != null) {
                _formState.update { 
                    it.copy(
                        latitude = location.latitude.toString(),
                        longitude = location.longitude.toString(),
                        isFetchingLocation = false
                    ) 
                }
            } else {
                _formState.update { it.copy(isFetchingLocation = false) }
                _uiEvents.emit(SOSUiEvent.SubmitError("Unable to fetch location. Please ensure GPS is enabled and permissions are granted."))
            }
        }
    }

    fun submitSOS() {
        val state = _formState.value

        // Validation
        if (state.injuryType.isBlank()) {
            _uiEvents.tryEmit(SOSUiEvent.SubmitError("Please select an injury type"))
            return
        }
        val lat = state.latitude.toDoubleOrNull()
        val lon = state.longitude.toDoubleOrNull()
        if (lat == null || lon == null) {
            _uiEvents.tryEmit(SOSUiEvent.SubmitError("Please enter valid coordinates"))
            return
        }

        _formState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            try {
                // Create SOS record locally
                createSOSRequestUseCase(
                    localNodeId = localNodeId,
                    injuryType = state.injuryType,
                    severity = state.severity,
                    latitude = lat,
                    longitude = lon,
                    description = state.description
                )

                // Broadcast delta to mesh peers
                val payload = crdtSyncEngine.serializeDelta(localNodeId, sinceTimestamp = 0L)
                meshNetworkManager.broadcastPayload(payload)
                cloudSyncManager.sendPayload(payload)

                // Reset form
                _formState.value = SOSFormState()
                _uiEvents.emit(SOSUiEvent.SubmitSuccess)
            } catch (e: Exception) {
                _formState.update { it.copy(isSubmitting = false) }
                _uiEvents.emit(SOSUiEvent.SubmitError(e.message ?: "Unknown error"))
            }
        }
    }

    fun resolveSOS(sosId: String) {
        viewModelScope.launch {
            try {
                sosRepository.softDelete(sosId, localNodeId)
                val payload = crdtSyncEngine.serializeDelta(localNodeId, sinceTimestamp = 0L)
                meshNetworkManager.broadcastPayload(payload)
                cloudSyncManager.sendPayload(payload)
            } catch (e: Exception) {
                _uiEvents.emit(SOSUiEvent.SubmitError("Failed to resolve SOS: ${e.message}"))
            }
        }
    }
}
