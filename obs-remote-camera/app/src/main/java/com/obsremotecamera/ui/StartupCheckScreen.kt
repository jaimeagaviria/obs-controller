package com.obsremotecamera.ui

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obsremotecamera.MainViewModel
import com.obsremotecamera.PrerequisiteStep
import com.obsremotecamera.StepStatus

@Composable
fun StartupCheckScreen(
    viewModel: MainViewModel,
    onAllDone: () -> Unit,
    onExit: () -> Unit
) {
    val steps by viewModel.prerequisiteSteps.collectAsState()
    val allDone by viewModel.prerequisitesDone.collectAsState()
    val context = LocalContext.current
    val vpnFailed = steps[0].status == StepStatus.FAILED

    LaunchedEffect(Unit) {
        viewModel.runPrerequisiteChecks()
    }

    LaunchedEffect(allDone) {
        if (allDone) {
            kotlinx.coroutines.delay(700)
            onAllDone()
        }
    }

    val hasFailed = steps.any { it.status == StepStatus.FAILED }
    val failedStep = steps.firstOrNull { it.status == StepStatus.FAILED }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .widthIn(max = 600.dp)
                .padding(horizontal = 48.dp)
        ) {
            // Title
            Text(
                text = "OBS Remote Camera",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(Modifier.height(6.dp))

            if (!hasFailed) {
                Text(
                    text = "Verificando prerequisitos…",
                    color = Color(0xFF757575),
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(52.dp))

            // Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.Center
            ) {
                steps.forEachIndexed { index, step ->
                    StepNode(step = step, number = index + 1)
                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 27.dp)
                        ) {
                            StepConnector(done = step.status == StepStatus.OK)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Error message
            if (failedStep != null) {
                Text(
                    text = failedStep.errorMessage,
                    color = Color(0xFFFF5252),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            // Action buttons (only on failure)
            if (hasFailed) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (vpnFailed) {
                        Button(
                            onClick = {
                                viewModel.tailscaleManager.getLaunchIntent()
                                    ?.let { context.startActivity(it) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                        ) {
                            Text("Abrir Tailscale")
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.runPrerequisiteChecks() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF616161))
                    ) {
                        Text("Reintentar")
                    }
                    Button(
                        onClick = onExit,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                    ) {
                        Text("Cerrar app")
                    }
                }
            }
        }
    }
}

@Composable
private fun StepNode(step: PrerequisiteStep, number: Int) {
    val nodeBackground by animateColorAsState(
        targetValue = when (step.status) {
            StepStatus.PENDING  -> Color(0xFF1A1A1A)
            StepStatus.CHECKING -> Color(0xFF0D1B4B)
            StepStatus.OK       -> Color(0xFF0A3D1A)
            StepStatus.FAILED   -> Color(0xFF3D0A0A)
        },
        animationSpec = tween(400),
        label = "nodeBg"
    )
    val borderColor by animateColorAsState(
        targetValue = when (step.status) {
            StepStatus.PENDING  -> Color(0xFF3A3A3A)
            StepStatus.CHECKING -> Color(0xFF3F51B5)
            StepStatus.OK       -> Color(0xFF4CAF50)
            StepStatus.FAILED   -> Color(0xFFF44336)
        },
        animationSpec = tween(400),
        label = "nodeBorder"
    )
    val labelColor by animateColorAsState(
        targetValue = when (step.status) {
            StepStatus.PENDING  -> Color(0xFF4A4A4A)
            StepStatus.CHECKING -> Color(0xFF7986CB)
            StepStatus.OK       -> Color(0xFF66BB6A)
            StepStatus.FAILED   -> Color(0xFFEF5350)
        },
        animationSpec = tween(400),
        label = "labelColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(nodeBackground)
                .border(1.5.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (step.status) {
                StepStatus.PENDING -> Text(
                    text = "$number",
                    color = Color(0xFF4A4A4A),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                StepStatus.CHECKING -> CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    color = Color(0xFF7986CB),
                    strokeWidth = 2.dp
                )
                StepStatus.OK -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF69F0AE),
                    modifier = Modifier.size(28.dp)
                )
                StepStatus.FAILED -> Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Text(
            text = step.label,
            color = labelColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

    }
}

@Composable
private fun StepConnector(done: Boolean) {
    val lineColor by animateColorAsState(
        targetValue = if (done) Color(0xFF2E7D32) else Color(0xFF1E1E1E),
        animationSpec = tween(600),
        label = "connectorColor"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.5.dp)
            .background(lineColor)
    )
}
