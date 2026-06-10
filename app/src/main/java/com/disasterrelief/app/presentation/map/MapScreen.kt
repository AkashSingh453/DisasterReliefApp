package com.disasterrelief.app.presentation.map

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.disasterrelief.app.domain.model.SOSRequest
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Offline map screen using OSMDroid wrapped in an AndroidView composable.
 *
 * Displays SOS request pins color-coded by severity level.
 * The map is configured for offline tile rendering — if no tiles are cached,
 * it will attempt to load from the default online source.
 */
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToDirectChat: (String, String) -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val sosRequests by viewModel.sosRequests.collectAsState()
    val mapCenterLat by viewModel.mapCenterLat.collectAsState()
    val mapCenterLon by viewModel.mapCenterLon.collectAsState()
    val zoomLevel by viewModel.zoomLevel.collectAsState()
    val context = LocalContext.current

    var selectedSos by remember { mutableStateOf<SOSRequest?>(null) }
    val sheetState = rememberModalBottomSheetState()

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
                text = "Situation Map",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${sosRequests.size} SOS markers plotted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ── Map View ──
        Box(modifier = Modifier.weight(1f)) {
            val mapView = remember {
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    minZoomLevel = 3.0
                    maxZoomLevel = 19.0
                    controller.setZoom(zoomLevel)
                    controller.setCenter(GeoPoint(mapCenterLat, mapCenterLon))
                }
            }

            // Lifecycle management for MapView
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> mapView.onResume()
                        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    mapView.onDetach()
                }
            }

            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // Clear existing markers and re-add from current state
                    view.overlays.removeAll { it is Marker }

                    sosRequests.forEach { sos ->
                        val marker = Marker(view).apply {
                            position = GeoPoint(sos.latitude, sos.longitude)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "${sos.injuryType} (Severity: ${sos.severity})"
                            snippet = sos.description.take(100)
                            subDescription = "Node: ${sos.createdByNodeId.take(8)}…"
                            setOnMarkerClickListener { _, _ ->
                                selectedSos = sos
                                true
                            }
                        }
                        view.overlays.add(marker)
                    }

                    // Center map on first SOS if available and map hasn't been manually moved
                    if (sosRequests.isNotEmpty()) {
                        val firstSos = sosRequests.first()
                        view.controller.setCenter(GeoPoint(firstSos.latitude, firstSos.longitude))
                    }

                    view.invalidate()
                }
            )

            // ── Legend Overlay ──
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "SOS Markers",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap a pin for details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (selectedSos != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedSos = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "SOS Details",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Injury Type: ${selectedSos!!.injuryType}", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Severity: ${selectedSos!!.severity}", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Description: ${selectedSos!!.description}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Node ID: ${selectedSos!!.createdByNodeId}", style = MaterialTheme.typography.labelSmall)

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val sos = selectedSos!!
                        selectedSos = null
                        onNavigateToDirectChat(sos.createdByNodeId, "Node-${sos.createdByNodeId.take(4)}")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Help / Chat")
                }
            }
        }
    }
}
