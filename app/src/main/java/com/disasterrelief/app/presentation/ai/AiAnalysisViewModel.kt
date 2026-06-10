package com.disasterrelief.app.presentation.ai

import android.util.Log
import androidx.compose.ui.layout.LookaheadScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disasterrelief.app.BuildConfig
import com.disasterrelief.app.data.llm.DownloadState
import com.disasterrelief.app.data.llm.LocalLlmRepository
import com.disasterrelief.app.data.llm.ModelDownloadManager
import com.disasterrelief.app.domain.usecase.ObserveSOSRequestsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiAnalysisViewModel @Inject constructor(
    private val modelDownloadManager: ModelDownloadManager,
    private val localLlmRepository: LocalLlmRepository,
    private val observeSOSRequestsUseCase: ObserveSOSRequestsUseCase
) : ViewModel() {

    val downloadState: StateFlow<DownloadState> = modelDownloadManager.downloadState

    val mapCenterLat = MutableStateFlow(28.6139) // Default: New Delhi
    val mapCenterLon = MutableStateFlow(77.2090)
    val radiusKm = MutableStateFlow(5f)

    private val _analysisResult = MutableStateFlow("")
    val analysisResult: StateFlow<String> = _analysisResult.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // Expose all active requests for the Map UI
    val allRequests = observeSOSRequestsUseCase()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Download from our DisasterRelief server — derives URL from the single BuildConfig base
    fun triggerModelSetup() {
        val url = BuildConfig.SYNC_BASE_URL + "models/emergency_llm.bin"
        modelDownloadManager.downloadModel(url)
    }

    fun clearCorruptedModel() {
        modelDownloadManager.deleteModel()
    }

    fun analyzeRegionalData() {
        if (_isAnalyzing.value) return
        
        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisResult.value = ""
            
            try {
                localLlmRepository.initializeEngine()
                
                // Fetch all SOS requests
                val sosRequests = observeSOSRequestsUseCase().first()
                
                // Filter by radius
                val centerLat = mapCenterLat.value
                val centerLon = mapCenterLon.value
                val maxRadius = radiusKm.value.toDouble()
                
                val regionalRequests = sosRequests.filter { req ->
                    distanceInKm(centerLat, centerLon, req.latitude, req.longitude) <= maxRadius
                }
                
                if (regionalRequests.isEmpty()) {
                    _analysisResult.value = "No active SOS requests found within $maxRadius km of the selected coordinates."
                    _isAnalyzing.value = false
                    return@launch
                }
                
                // Format prompt
                val promptBuilder = StringBuilder()
                promptBuilder.append("Target Region: Lat $centerLat, Lon $centerLon, Radius $maxRadius km.\n")
                promptBuilder.append("Here are the current SOS requests inside this region:\n")
                regionalRequests.forEach { req ->
                    promptBuilder.append("- Severity: ${req.severity}, Type: ${req.injuryType}. Description: ${req.description}\n")
                }
                
                // Stream output
                localLlmRepository.processTriageChat(promptBuilder.toString())
                    .collect { token ->
                        _analysisResult.value += token
                    }
                Log.d("AIViewModel" , _analysisResult.value.toString())
            } catch (e: Exception) {
                Log.d("AIViewModel" , e.message.toString())
                _analysisResult.value = "Error during analysis: ${e.message}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    private fun distanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
