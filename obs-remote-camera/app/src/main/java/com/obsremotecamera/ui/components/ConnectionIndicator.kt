package com.obsremotecamera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obsremotecamera.TailscaleStatus

@Composable
fun ConnectionIndicator(
    tailscaleStatus: TailscaleStatus,
    apiStatus: ApiConnectionStatus,
    modifier: Modifier = Modifier
) {
    // Prioridad: VPN primero, luego API
    val (color, label) = when {
        tailscaleStatus == TailscaleStatus.NOT_INSTALLED  -> Color.Red         to "Sin Tailscale"
        tailscaleStatus == TailscaleStatus.VPN_OFF        -> Color.Red         to "VPN desconectada"
        tailscaleStatus == TailscaleStatus.CHECKING       -> Color(0xFFFFA500) to "Conectando…"
        apiStatus == ApiConnectionStatus.DISCONNECTED     -> Color.Red         to "API desconectada"
        apiStatus == ApiConnectionStatus.CONNECTING       -> Color(0xFFFFA500) to "Conectando…"
        else                                              -> Color(0xFF00C853) to "Conectado"
    }

    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
