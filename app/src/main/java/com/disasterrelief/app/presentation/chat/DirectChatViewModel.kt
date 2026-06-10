package com.disasterrelief.app.presentation.chat

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disasterrelief.app.data.sync.CloudSyncManager
import com.disasterrelief.app.data.sync.CrdtSyncEngine
import com.disasterrelief.app.domain.model.Message
import com.disasterrelief.app.domain.usecase.ObserveDirectMessagesUseCase
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
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DirectChatViewModel @Inject constructor(
    private val observeDirectMessagesUseCase: ObserveDirectMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val meshNetworkManager: MeshNetworkManager,
    private val crdtSyncEngine: CrdtSyncEngine,
    private val cloudSyncManager: CloudSyncManager,
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val prefs = context.getSharedPreferences("disaster_relief_prefs", Context.MODE_PRIVATE)
    val localNodeId = prefs.getString("node_id", "") ?: UUID.randomUUID().toString()
    val localDisplayName = prefs.getString("display_name", "Unknown") ?: "Unknown"

    val peerId: String = checkNotNull(savedStateHandle["peerId"])
    val peerName: String = checkNotNull(savedStateHandle["peerName"])

    val messages: StateFlow<List<Message>> = observeDirectMessagesUseCase(localNodeId, peerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()

    fun updateMessageInput(input: String) {
        _messageInput.value = input
    }

    fun sendMessage() {
        val content = _messageInput.value.trim()
        if (content.isNotEmpty()) {
            viewModelScope.launch {
                val msg = sendMessageUseCase(
                    localNodeId = localNodeId,
                    senderName = localDisplayName,
                    content = content,
                    recipientId = peerId
                )
                
                // Broadcast to mesh and cloud
                val payload = crdtSyncEngine.serializeDelta(localNodeId, sinceTimestamp = 0L)
                meshNetworkManager.broadcastPayload(payload)
                cloudSyncManager.sendPayload(payload)
                
                _messageInput.value = ""
            }
        }
    }
}
