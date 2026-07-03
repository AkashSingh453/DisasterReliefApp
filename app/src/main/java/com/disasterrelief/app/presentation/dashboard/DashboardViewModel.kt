package com.disasterrelief.app.presentation.dashboard

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disasterrelief.app.data.repository.SOSRepository
import com.disasterrelief.app.data.repository.UserNodeRepository
import com.disasterrelief.app.data.sync.CloudSyncManager
import com.disasterrelief.app.data.sync.CrdtSyncEngine
import com.disasterrelief.app.domain.model.DiscoveredPeer
import com.disasterrelief.app.domain.model.SOSRequest
import com.disasterrelief.app.domain.usecase.ObserveMessagesUseCase
import com.disasterrelief.app.domain.usecase.ObserveSOSRequestsUseCase
import com.disasterrelief.app.mesh.MeshNetworkManager
import com.disasterrelief.app.mesh.SyncEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the Mesh Dashboard screen.
 *
 * Provides:
 * - Live mesh connection state (connected peers, mesh active status).
 * - SOS request and message counts.
 * - Stream of sync events for the activity feed.
 * - Mesh start/stop controls.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val meshNetworkManager: MeshNetworkManager,
    private val observeSOSRequestsUseCase: ObserveSOSRequestsUseCase,
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val sosRepository: SOSRepository,
    private val userNodeRepository: UserNodeRepository,
    private val crdtSyncEngine: CrdtSyncEngine,
    private val cloudSyncManager: CloudSyncManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ── Node identity ──
    private val prefs: SharedPreferences =
        context.getSharedPreferences("disaster_relief_prefs", Context.MODE_PRIVATE)

    val localNodeId: String = prefs.getString("node_id", null) ?: run {
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString("node_id", newId).apply()
        newId
    }

    val localDisplayName: String = prefs.getString("node_name", null) ?: run {
        val name = "Node-${localNodeId.take(6)}"
        prefs.edit().putString("node_name", name).apply()
        name
    }

    val localRole: String = prefs.getString("node_role", "VOLUNTEER") ?: "VOLUNTEER"

    // ── Mesh State ──
    val connectedPeers: StateFlow<List<DiscoveredPeer>> = meshNetworkManager.connectedPeers
    val isMeshActive: StateFlow<Boolean> = meshNetworkManager.isMeshActive

    // ── Data State ──
    val sosRequests: StateFlow<List<SOSRequest>> = observeSOSRequestsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _messageCount = MutableStateFlow(0)
    val messageCount: StateFlow<Int> = _messageCount.asStateFlow()

    // ── Sync Events ──
    private val _recentSyncEvents = MutableStateFlow<List<SyncEvent>>(emptyList())
    val recentSyncEvents: StateFlow<List<SyncEvent>> = _recentSyncEvents.asStateFlow()

    init {
        // Register local node into the database so it can be synced to peers
        viewModelScope.launch {
            userNodeRepository.registerLocalNode(
                nodeId = localNodeId,
                displayName = localDisplayName
            )
        }


        viewModelScope.launch {
            observeMessagesUseCase().collect { messages ->
                _messageCount.value = messages.size
            }
        }

        // Collect sync events (keep last 20)
        viewModelScope.launch {
            meshNetworkManager.syncEvents.collect { event ->
                _recentSyncEvents.update { events ->
                    (listOf(event) + events).take(20)
                }
            }
        }
    }

    val BAseURL = "http://192.168.31.81:8082/"

    fun startMesh() {
        meshNetworkManager.startMesh(localNodeId, localDisplayName)
       // cloudSyncManager.connect("http://ec2-13-222-13-194.compute-1.amazonaws.com/")
        cloudSyncManager.connect(BAseURL)
        cloudSyncManager.initialSync(localNodeId)
    }

    fun stopMesh() {
        meshNetworkManager.stopMesh()
        cloudSyncManager.disconnect()
    }

    fun updateDisplayName(name: String) {
        prefs.edit().putString("display_name", name).apply()
    }

    /**
     * Triggers a broadcast of the full local state to all connected peers.
     */
    fun broadcastFullSync() {
        viewModelScope.launch {
            val payload = crdtSyncEngine.serializeDelta(localNodeId, sinceTimestamp = 0L)
            meshNetworkManager.broadcastPayload(payload)
            cloudSyncManager.sendPayload(payload)
        }
    }
}
