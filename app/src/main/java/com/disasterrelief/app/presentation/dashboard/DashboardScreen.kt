package com.disasterrelief.app.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.disasterrelief.app.mesh.SyncEvent
import com.disasterrelief.app.presentation.theme.CrimsonPrimary
import com.disasterrelief.app.presentation.theme.StatusOnline
import com.disasterrelief.app.presentation.theme.StatusSyncing
import com.disasterrelief.app.presentation.theme.TealSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigateToSOS: () -> Unit,
    onNavigateToAiAnalysis: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val connectedPeers by viewModel.connectedPeers.collectAsState()
    val isMeshActive by viewModel.isMeshActive.collectAsState()
    val sosRequests by viewModel.sosRequests.collectAsState()
    val messageCount by viewModel.messageCount.collectAsState()
    val recentSyncEvents by viewModel.recentSyncEvents.collectAsState()
    val context = LocalContext.current

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissionsToRequest.all { permissions[it] == true }
        if (allGranted) {
            viewModel.startMesh()
        } else {
            Toast.makeText(context, "All permissions are required for the mesh network to function.", Toast.LENGTH_LONG).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header ──
        item {
            Text(
                text = "Mesh Dashboard",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            Text(
                text = "Node: ${viewModel.localDisplayName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ── Mesh Status Card ──
        item {
            MeshStatusCard(
                peerCount = connectedPeers.size,
                isMeshActive = isMeshActive,
                onStartMesh = { permissionLauncher.launch(permissionsToRequest) },
                onStopMesh = { viewModel.stopMesh() }
            )
        }

        // ── SOS Trigger Button ──
        item {
            SOSTriggerButton(onClick = onNavigateToSOS)
        }

        // ── Quick Stats Row ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Sos,
                    value = sosRequests.size.toString(),
                    label = "SOS Requests"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Message,
                    value = messageCount.toString(),
                    label = "Messages"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Groups,
                    value = connectedPeers.size.toString(),
                    label = "Peers"
                )
            }
        }

        // ── Sync to cloud button ──
        item {
            FilledTonalButton(
                onClick = { viewModel.broadcastFullSync() },
                modifier = Modifier.fillMaxWidth(),
                enabled = isMeshActive && connectedPeers.isNotEmpty()
            ) {
                Icon(Icons.Filled.CloudSync, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Broadcast Full Sync to Peers")
            }
        }

        // ── AI Analysis Button ──
        item {
            Button(
                onClick = onNavigateToAiAnalysis,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealSecondary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("AI Emergency Analyzer", style = MaterialTheme.typography.titleMedium)
            }
        }

        // ── Sync Event Feed ──
        item {
            Text(
                text = "Sync Activity",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (recentSyncEvents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "No sync events yet. Start the mesh to begin peer discovery.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }

        items(recentSyncEvents) { event ->
            SyncEventItem(event = event)
        }
    }
}

@Composable
private fun MeshStatusCard(
    peerCount: Int,
    isMeshActive: Boolean,
    onStartMesh: () -> Unit,
    onStopMesh: () -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = if (isMeshActive) StatusOnline else MaterialTheme.colorScheme.outline,
        animationSpec = tween(500),
        label = "meshStatusColor"
    )

    // Pulsing animation when mesh is active
    val infiniteTransition = rememberInfiniteTransition(label = "meshPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .alpha(if (isMeshActive) pulseAlpha else 1f)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (isMeshActive) "Mesh Active" else "Mesh Offline",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Icon(
                    imageVector = Icons.Filled.Bluetooth,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            AnimatedVisibility(visible = isMeshActive) {
                Text(
                    text = "$peerCount connected peer${if (peerCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TealSecondary
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = if (isMeshActive) onStopMesh else onStartMesh,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMeshActive)
                        MaterialTheme.colorScheme.error
                    else
                        TealSecondary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isMeshActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isMeshActive) "Stop Mesh" else "Start Mesh",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SOSTriggerButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CrimsonPrimary
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Sos,
            contentDescription = "Send SOS",
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "SEND SOS SIGNAL",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SyncEventItem(event: SyncEvent) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (event) {
                    is SyncEvent.Incoming -> Icons.Filled.Sync
                    is SyncEvent.Outgoing -> Icons.Filled.SyncAlt
                },
                contentDescription = null,
                tint = when (event) {
                    is SyncEvent.Incoming -> StatusSyncing
                    is SyncEvent.Outgoing -> TealSecondary
                },
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (event) {
                        is SyncEvent.Incoming -> "Received ${event.recordCount} records"
                        is SyncEvent.Outgoing -> "Sent ${event.recordCount} records to ${event.peerCount} peers"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (event) {
                        is SyncEvent.Incoming -> "From: ${event.fromEndpointId.take(8)}…"
                        is SyncEvent.Outgoing -> "Broadcast"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = timeFormat.format(Date(event.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
