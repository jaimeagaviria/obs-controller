package com.obsremotecamera.ui

import com.pedro.encoder.utils.gl.AspectRatioMode
import com.pedro.library.view.OpenGlView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.obsremotecamera.MainViewModel
import com.obsremotecamera.streaming.StreamState
import com.obsremotecamera.ui.components.BitrateOverlay
import com.obsremotecamera.ui.components.LiveBadge

@Composable
fun StreamScreen(
    viewModel: MainViewModel,
    onNavigateToConfig: () -> Unit,
    onExit: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    val streamState by viewModel.streamState.collectAsState()
    val bitrateKbps by viewModel.currentBitrateKbps.collectAsState()

    val durationSeconds by viewModel.streamDurationSeconds.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }


    val isStreaming = streamState is StreamState.Streaming
    val isConnecting = streamState is StreamState.Connecting || streamState is StreamState.Reconnecting

    // Mantener pantalla encendida mientras StreamScreen esté visible (sin timeout por inactividad)
    val currentView = LocalView.current
    DisposableEffect(Unit) {
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    // Auto-start stream on enter if host is configured.
    // Delay guards against race with MainViewModel's camera-change restart:
    // when returning from ConfigScreen, StreamScreen recomposes and this
    // LaunchedEffect runs while MainViewModel is mid-way through stop→start.
    LaunchedEffect(config.srtHost) {
        if (config.srtHost.isNotBlank() &&
            viewModel.streamState.value is StreamState.Idle
        ) {
            delay(500)
            if (viewModel.streamState.value is StreamState.Idle) {
                viewModel.startStream()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview (full screen)
        AndroidView(
            factory = { ctx ->
                OpenGlView(ctx).also { surface ->
                    surface.setAspectRatioMode(AspectRatioMode.Fill)
                    surface.holder.addCallback(object : android.view.SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                            viewModel.attachSurface(surface)
                        }
                        override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {}
                        override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {}
                    })
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = {
                viewModel.detachSurface()
            }
        )

        // Top overlay bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Top-center: Camera number
            Text(
                text = "Cámara ${config.cameraNumber}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Top-right: LIVE badge
            LiveBadge(
                isLive = isStreaming,
                durationSeconds = durationSeconds
            )
        }

        // Bottom overlay bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            // Bottom-left: Exit button
            FloatingActionButton(
                onClick = {
                    if (isStreaming) {
                        showExitDialog = true
                    } else {
                        onExit()
                    }
                },
                containerColor = Color.Red.copy(alpha = 0.9f),
                contentColor = Color.White,
                modifier = Modifier.size(48.dp).align(Alignment.BottomStart),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Close, contentDescription = "Salir")
            }

            // Bottom-center: Bitrate overlay
            if (isStreaming || isConnecting) {
                BitrateOverlay(
                    bitrateKbps = bitrateKbps,
                    resolutionLabel = config.resolutionLabel,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Bottom-center: Start/Stop stream button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                if (!isStreaming && !isConnecting) {
                    FloatingActionButton(
                        onClick = {
                            if (config.srtHost.isNotBlank()) {
                                viewModel.startStream()
                            }
                        },
                        containerColor = Color(0xFF00C853),
                        contentColor = Color.White,
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Iniciar")
                    }
                }

                // Stream status text
                if (streamState is StreamState.Error) {
                    Text(
                        text = (streamState as StreamState.Error).message,
                        color = Color.Red,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (isConnecting) {
                    Text(
                        text = if (streamState is StreamState.Reconnecting) "Reconectando…" else "Conectando…",
                        color = Color.Yellow,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Bottom-right: Config button
            FloatingActionButton(
                onClick = onNavigateToConfig,
                containerColor = Color.Gray.copy(alpha = 0.8f),
                contentColor = Color.White,
                modifier = Modifier.size(48.dp).align(Alignment.BottomEnd),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Configuración")
            }
        }

    }

    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Salir") },
            text = { Text("¿Detener transmisión y salir?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    viewModel.stopStream()
                    onExit()
                }) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("No")
                }
            }
        )
    }

}
