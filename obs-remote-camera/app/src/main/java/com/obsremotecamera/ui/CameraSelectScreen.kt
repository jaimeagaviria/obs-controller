package com.obsremotecamera.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obsremotecamera.MainViewModel
import com.obsremotecamera.ui.components.ApiConnectionStatus

private val CamBlue       = Color(0xFF2196F3)
private val CamBlueDark   = Color(0xFF0D1B4B)
private val CamBlueBorder = Color(0xFF1E5CB3)
private val CardDark      = Color(0xFF111318)
private val CardBorder    = Color(0xFF252830)
private val BgColor       = Color(0xFF0A0A0F)
private val TextDim       = Color(0xFF4A5060)

@Composable
fun CameraSelectScreen(
    viewModel: MainViewModel,
    onConfirmed: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    val enabledCameras by viewModel.enabledCameras.collectAsState()
    val wsStatus by viewModel.apiConnectionStatus.collectAsState()

    // Si la cámara seleccionada desaparece de la lista, auto-ajustar a la primera disponible
    val initialCamera by remember(config.cameraNumber, enabledCameras) {
        derivedStateOf {
            if (enabledCameras.contains(config.cameraNumber)) config.cameraNumber
            else enabledCameras.firstOrNull() ?: config.cameraNumber
        }
    }
    var selectedCamera by remember(initialCamera) { mutableIntStateOf(initialCamera) }

    // Si el admin elimina la cámara actualmente seleccionada, reajustar silenciosamente
    LaunchedEffect(enabledCameras) {
        if (enabledCameras.isNotEmpty() && !enabledCameras.contains(selectedCamera)) {
            selectedCamera = enabledCameras.first()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0D1220), BgColor),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x08FFFFFF),
                            Color.Transparent,
                            Color(0x04FFFFFF)
                        )
                    )
                )
        )

        // Indicador WebSocket (esquina superior derecha)
        SyncIndicator(
            status = wsStatus,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 760.dp)
                .padding(horizontal = 40.dp)
        ) {
            // ── Header ───────────────────────────────────────────────────
            Text(
                text = "OBS REMOTE CAMERA",
                color = Color(0xFF3A4A6A),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Selecciona tu cámara",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Elige con qué cámara vas a transmitir en esta sesión",
                color = Color(0xFF555E70),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // ── Camera grid ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
                enabledCameras.forEach { cam ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.85f),
                        exit  = fadeOut(tween(200)) + scaleOut(tween(200)),
                        modifier = Modifier.weight(1f)
                    ) {
                        CameraCard(
                            cameraNumber = cam,
                            isSelected = cam == selectedCamera,
                            onClick = { selectedCamera = cam }
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Confirm button ───────────────────────────────────────────
            Button(
                onClick = {
                    viewModel.saveConfig(config.copy(cameraNumber = selectedCamera))
                    onConfirmed()
                },
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CamBlue),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Transmitir con Cámara $selectedCamera",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xAAFFFFFF)
                )
            }
        }
    }
}

// ── Indicador de sincronización en vivo ──────────────────────────────────────

@Composable
private fun SyncIndicator(status: ApiConnectionStatus, modifier: Modifier = Modifier) {
    val isConnected = status == ApiConnectionStatus.CONNECTED
    val isConnecting = status == ApiConnectionStatus.CONNECTING

    val infiniteTransition = rememberInfiniteTransition(label = "sync")
    // Pulso de respiración solo cuando conecta o reconecta
    val breatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val dotColor by animateColorAsState(
        targetValue = when {
            isConnected  -> Color(0xFF69F0AE)   // verde — sincronizado en tiempo real
            isConnecting -> Color(0xFFFFA726)   // ámbar — conectando
            else         -> Color(0xFF555E70)   // gris — desconectado
        },
        animationSpec = tween(400),
        label = "dotColor"
    )
    val dotAlpha = if (isConnecting) breatheAlpha else 1f
    val label = when {
        isConnected  -> "EN VIVO"
        isConnecting -> "CONECTANDO"
        else         -> "SIN CONEXIÓN"
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .alpha(dotAlpha)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            color = dotColor.copy(alpha = dotAlpha),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )
    }
}

// ── Tarjeta de cámara ─────────────────────────────────────────────────────────

@Composable
private fun CameraCard(
    cameraNumber: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) CamBlueDark else CardDark,
        animationSpec = tween(220), label = "cardBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) CamBlueBorder else CardBorder,
        animationSpec = tween(220), label = "cardBorder"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) CamBlue else TextDim,
        animationSpec = tween(220), label = "iconTint"
    )
    val numberColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF353A45),
        animationSpec = tween(220), label = "numColor"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF8BB8E8) else Color(0xFF2A2F38),
        animationSpec = tween(220), label = "labelColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1f,
        animationSpec = tween(200), label = "scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "$cameraNumber",
                color = numberColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 30.sp
            )
            Text(
                text = "CAM",
                color = labelColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
        }
    }
}
