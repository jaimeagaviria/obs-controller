package com.obsremotecamera.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obsremotecamera.MainViewModel
import com.obsremotecamera.config.AppConfig

data class ResolutionOption(
    val label: String,
    val width: Int,
    val height: Int,
    val fps: Int
)

private val resolutionOptions = listOf(
    ResolutionOption("720p", 1280, 720, 30),
    ResolutionOption("1080p", 1920, 1080, 30),
    ResolutionOption("1080p60", 1920, 1080, 60)
)

private val bitrateOptions = listOf(2000, 4000, 6000, 8000)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConfigScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val currentConfig by viewModel.config.collectAsState()

    var selectedCamera by remember(currentConfig) { mutableIntStateOf(currentConfig.cameraNumber) }
    var host by remember(currentConfig) { mutableStateOf(currentConfig.tailscaleHost) }
    var selectedResolution by remember(currentConfig) {
        mutableStateOf(
            resolutionOptions.find {
                it.width == currentConfig.resolutionWidth &&
                    it.height == currentConfig.resolutionHeight &&
                    it.fps == currentConfig.fps
            } ?: resolutionOptions[1]
        )
    }
    var selectedBitrate by remember(currentConfig) { mutableIntStateOf(currentConfig.videoBitrateKbps) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Camera selector
            Text(
                text = "Cámara",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (i in 1..6) {
                    val isSelected = selectedCamera == i
                    OutlinedButton(
                        onClick = { selectedCamera = i },
                        modifier = Modifier.size(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) Color(0xFF2196F3) else Color.Transparent,
                            contentColor = Color.White
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF2196F3) else Color.Gray
                        )
                    ) {
                        Text(
                            text = "$i",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Resolution selector
            Text(
                text = "Resolución",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                resolutionOptions.forEach { option ->
                    FilterChip(
                        selected = selectedResolution == option,
                        onClick = { selectedResolution = option },
                        label = { Text(option.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2196F3),
                            selectedLabelColor = Color.White,
                            labelColor = Color.White
                        )
                    )
                }
            }

            // Bitrate selector
            Text(
                text = "Bitrate",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                bitrateOptions.forEach { bitrate ->
                    FilterChip(
                        selected = selectedBitrate == bitrate,
                        onClick = { selectedBitrate = bitrate },
                        label = { Text("${bitrate / 1000} Mbps") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2196F3),
                            selectedLabelColor = Color.White,
                            labelColor = Color.White
                        )
                    )
                }
            }

            // Host field
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Host SRT") },
                placeholder = { Text("obs-server.tail-xxxx.ts.net") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Effective port (read-only, derived from camera number)
            OutlinedTextField(
                value = (5000 + selectedCamera).toString(),
                onValueChange = {},
                label = { Text("Puerto SRT") },
                singleLine = true,
                readOnly = true,
                modifier = Modifier.width(200.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save button
            Button(
                onClick = {
                    val newConfig = AppConfig(
                        cameraNumber = selectedCamera,
                        tailscaleHost = host.trim(),
                        srtBasePort = 5000,
                        resolutionWidth = selectedResolution.width,
                        resolutionHeight = selectedResolution.height,
                        fps = selectedResolution.fps,
                        videoBitrateKbps = selectedBitrate
                    )
                    viewModel.saveConfig(newConfig)
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Text("Guardar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
