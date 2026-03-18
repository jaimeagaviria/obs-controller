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
fun TailscaleIndicator(
    status: TailscaleStatus,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (status) {
        TailscaleStatus.CHECKING -> Color(0xFFFFA500) to "Verificando…"
        TailscaleStatus.NOT_INSTALLED -> Color.Red to "Sin Tailscale"
        TailscaleStatus.VPN_OFF -> Color.Red to "VPN desconectada"
        TailscaleStatus.SERVER_UNREACHABLE -> Color(0xFFFFA500) to "PC no alcanzable"
        TailscaleStatus.CONNECTED -> Color(0xFF00C853) to "Conectado"
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
