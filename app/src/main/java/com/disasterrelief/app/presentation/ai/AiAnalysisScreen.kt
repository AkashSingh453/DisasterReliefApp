package com.disasterrelief.app.presentation.ai

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.disasterrelief.app.data.llm.DownloadState
import com.disasterrelief.app.presentation.theme.TealSecondary
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun AiAnalysisScreen(
    viewModel: AiAnalysisViewModel = hiltViewModel()
) {
    val downloadState by viewModel.downloadState.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
        ,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AI Emergency Analyzer",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        AnimatedContent(targetState = downloadState, label = "DownloadState") { state ->
            when (state) {
                is DownloadState.Idle, is DownloadState.Error -> {
                    DownloadPromptCard(
                        error = (state as? DownloadState.Error)?.message,
                        onDownloadClick = { viewModel.triggerModelSetup() }
                    )
                }
                is DownloadState.Downloading -> {
                    DownloadingCard(progress = state.progress)
                }
                is DownloadState.Completed -> {
                    AnalysisDashboard(
                        viewModel = viewModel,
                        isAnalyzing = isAnalyzing,
                        analysisResult = analysisResult,
                        onAnalyzeClick = { viewModel.analyzeRegionalData() }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadPromptCard(error: String?, onDownloadClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = TealSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Download Local AI Engine",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "To analyze SOS requests securely without internet access, you must first download the Qwen-0.5B localized triage model (521 MB).",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDownloadClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
            ) {
                Text("Start Download", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun DownloadingCard(progress: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = TealSecondary,
                trackColor = MaterialTheme.colorScheme.surface
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Downloading AI Engine...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = TealSecondary,
                trackColor = MaterialTheme.colorScheme.surface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please do not close the app. The download continues in the background.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AnalysisDashboard(
    viewModel: AiAnalysisViewModel,
    isAnalyzing: Boolean,
    analysisResult: String,
    onAnalyzeClick: () -> Unit
) {
    val centerLat by viewModel.mapCenterLat.collectAsState()
    val centerLon by viewModel.mapCenterLon.collectAsState()
    val radiusKm by viewModel.radiusKm.collectAsState()
    val allRequests by viewModel.allRequests.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        
        // Map Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            val mapView = remember {
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    minZoomLevel = 3.0
                    maxZoomLevel = 19.0
                    controller.setZoom(10.0)
                    controller.setCenter(GeoPoint(centerLat, centerLon))
                }
            }

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
                    view.overlays.removeAll { it !is MapEventsOverlay }
                    
                    // Add click listener overlay if not present
                    if (view.overlays.none { it is MapEventsOverlay }) {
                        val eventsReceiver = object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                p?.let {
                                    viewModel.mapCenterLat.value = it.latitude
                                    viewModel.mapCenterLon.value = it.longitude
                                }
                                return true
                            }
                            override fun longPressHelper(p: GeoPoint?): Boolean = false
                        }
                        view.overlays.add(MapEventsOverlay(eventsReceiver))
                    }

                    // Add SOS Markers
                    allRequests.forEach { req ->
                        val reqMarker = Marker(view).apply {
                            position = GeoPoint(req.latitude, req.longitude)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "SOS: ${req.severity}"
                            snippet = "${req.injuryType} - ${req.description}"
                            val iconDrawable = androidx.core.content.ContextCompat.getDrawable(context, android.R.drawable.ic_dialog_alert)?.mutate()
                            iconDrawable?.setTint(android.graphics.Color.RED)
                            icon = iconDrawable
                        }
                        view.overlays.add(reqMarker)
                    }

                    // Add Target Pin
                    val targetMarker = Marker(view).apply {
                        position = GeoPoint(centerLat, centerLon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Target Zone"
                    }
                    view.overlays.add(targetMarker)
                    view.invalidate()
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Radius Slider
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Analysis Radius", style = MaterialTheme.typography.titleMedium)
                Text("${radiusKm.toInt()} km", style = MaterialTheme.typography.titleMedium, color = TealSecondary)
            }
            Slider(
                value = radiusKm,
                onValueChange = { viewModel.radiusKm.value = it },
                valueRange = 0f..200f,
                steps = 199,
                colors = SliderDefaults.colors(
                    thumbColor = TealSecondary,
                    activeTrackColor = TealSecondary
                )
            )
            Text(
                "Tap map to set target region.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onAnalyzeClick,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            enabled = !isAnalyzing,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Analyzing Region Data...", style = MaterialTheme.typography.titleMedium)
            } else {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Analyze Mesh SOS Data", style = MaterialTheme.typography.titleMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        androidx.compose.material3.OutlinedButton(
            onClick = { viewModel.clearCorruptedModel() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Clear Model (Fix TFLite Error)", color = MaterialTheme.colorScheme.error)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (analysisResult.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp), // Give it a fixed height so it's readable within the scrolling screen
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        text = analysisResult,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
