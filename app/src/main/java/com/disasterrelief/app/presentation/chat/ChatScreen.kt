package com.disasterrelief.app.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.disasterrelief.app.domain.model.Message
import com.disasterrelief.app.presentation.theme.TealSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToDirectChat: (String, String) -> Unit = { _, _ -> },
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val availableNodes by viewModel.availableNodes.collectAsState()
    val messageInput by viewModel.messageInput.collectAsState()
    val selectedRecipientId by viewModel.selectedRecipientId.collectAsState()
    val isAlert by viewModel.isAlert.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val inboxThreads by viewModel.inboxThreads.collectAsState()
    val listState = rememberLazyListState()

    var expanded by remember { androidx.compose.runtime.mutableStateOf(false) }
    var selectedTabIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Mesh Chat",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${messages.size} messages synced across mesh",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Global Mesh") }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Inbox (${inboxThreads.size})") }
            )
        }

        if (selectedTabIndex == 0) {
            // ── Global Chat ──
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No messages yet.\nSend a message to start the conversation.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            items(messages, key = { it.id }) { message ->
                val isOwnMessage = message.senderNodeId == viewModel.localNodeId
                MessageBubble(
                    message = message,
                    isOwnMessage = isOwnMessage
                )
            }
        }

        // ── Input Bar ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Options Row (Recipient & Alert Toggle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Recipient Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.weight(1f)
                ) {
                    val recipientName = if (selectedRecipientId == null) "Everyone" else {
                        availableNodes.find { it.id == selectedRecipientId }?.displayName ?: "Unknown"
                    }
                    OutlinedTextField(
                        value = recipientName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Everyone") },
                            onClick = {
                                viewModel.selectRecipient(null)
                                expanded = false
                            }
                        )
                        availableNodes.forEach { node ->
                            if (node.id != viewModel.localNodeId) {
                                DropdownMenuItem(
                                    text = { Text(node.displayName) },
                                    onClick = {
                                        viewModel.selectRecipient(node.id)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Alert Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isAlert,
                        onCheckedChange = { viewModel.toggleAlert(it) }
                    )
                    Text("Alert", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Text Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { viewModel.updateMessageInput(it) },
                    placeholder = { Text("Type a message…") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp),
                    enabled = !isSending
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = messageInput.isNotBlank() && !isSending,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = TealSecondary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
        } else {
            // ── Inbox List ──
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (inboxThreads.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No direct messages yet.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                items(inboxThreads, key = { it.id }) { message ->
                    val isOwnMessage = message.senderNodeId == viewModel.localNodeId
                    val peerId = if (isOwnMessage) message.recipientId!! else message.senderNodeId
                    val peerName = if (isOwnMessage) "Node-${peerId.take(4)}" else message.senderName
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDirectChat(peerId, peerName) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Sos,
                                    contentDescription = "SOS Helper",
                                    tint = androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp).padding(end = 4.dp)
                                )
                                Text(text = "Chat with $peerName", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${if(isOwnMessage) "You" else peerName}: ${message.content.take(40)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isOwnMessage: Boolean
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val alignment = if (isOwnMessage) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isAlert) {
        androidx.compose.ui.graphics.Color(0xFFFFA000).copy(alpha = 0.2f) // Warning Orange
    } else if (isOwnMessage) {
        TealSecondary.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = MaterialTheme.colorScheme.onSurface

    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isOwnMessage) 16.dp else 4.dp,
        bottomEnd = if (isOwnMessage) 4.dp else 16.dp
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isOwnMessage) {
            val labelText = if (message.recipientId != null) "${message.senderName} (Direct to you)" else message.senderName
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TealSecondary,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
            )
        }

        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = bubbleShape
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.isAlert) {
                    Text(
                        text = "⚠️ ALERT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = timeFormat.format(Date(message.lastUpdatedTimestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
