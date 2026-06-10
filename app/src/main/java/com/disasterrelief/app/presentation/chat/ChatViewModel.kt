package com.disasterrelief.app.presentation.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disasterrelief.app.data.sync.CloudSyncManager
import com.disasterrelief.app.data.sync.CrdtSyncEngine
import com.disasterrelief.app.domain.model.Message
import com.disasterrelief.app.domain.usecase.ObserveMessagesUseCase
import com.disasterrelief.app.domain.usecase.ObserveInboxUseCase
import com.disasterrelief.app.domain.usecase.ObserveNodesUseCase
import com.disasterrelief.app.domain.usecase.SendMessageUseCase
import com.disasterrelief.app.mesh.MeshNetworkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val observeInboxUseCase: ObserveInboxUseCase,
    private val observeNodesUseCase: ObserveNodesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val meshNetworkManager: MeshNetworkManager,
    private val crdtSyncEngine: CrdtSyncEngine,
    private val cloudSyncManager: CloudSyncManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("disaster_relief_prefs", Context.MODE_PRIVATE)
    val localNodeId: String = prefs.getString("node_id", "") ?: ""
    val localDisplayName: String = prefs.getString("node_name", "Unknown") ?: "Unknown"
    val localRole: String = prefs.getString("node_role", "VOLUNTEER") ?: "VOLUNTEER"

    val messages: StateFlow<List<Message>> = observeMessagesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inboxThreads: StateFlow<List<Message>> = observeInboxUseCase(localNodeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()

    private val _selectedRecipientId = MutableStateFlow<String?>(null)
    val selectedRecipientId: StateFlow<String?> = _selectedRecipientId.asStateFlow()

    private val _isAlert = MutableStateFlow(false)
    val isAlert: StateFlow<Boolean> = _isAlert.asStateFlow()

    val availableNodes = observeNodesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    fun updateMessageInput(text: String) {
        _messageInput.value = text
    }

    fun selectRecipient(id: String?) {
        _selectedRecipientId.value = id
    }

    fun toggleAlert(enabled: Boolean) {
        _isAlert.value = enabled
    }

    fun sendMessage() {
        val content = _messageInput.value.trim()
        if (content.isBlank()) return

        _isSending.value = true

        viewModelScope.launch {
            try {
                // Persist message locally
                val alertPriority = if (_isAlert.value) 5 else 0

                sendMessageUseCase(
                    localNodeId = localNodeId,
                    senderName = localDisplayName,
                    content = content,
                    recipientId = _selectedRecipientId.value,
                    isAlert = _isAlert.value,
                    alertPriority = alertPriority
                )

                // Serialize the local database changes into a payload
                val payload = crdtSyncEngine.serializeDelta(localNodeId, sinceTimestamp = 0L)

                // Broadcast to the offline mesh
                meshNetworkManager.broadcastPayload(payload)
                
                // Forward to the cloud (if connected)
                cloudSyncManager.sendPayload(payload)

                // Clear input
                _messageInput.value = ""
            } catch (_: Exception) {
                // Message is already persisted locally, broadcast failure is non-fatal
            } finally {
                _isSending.value = false
            }
        }
    }
}
