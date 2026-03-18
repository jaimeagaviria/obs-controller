package com.obsremotecamera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BitrateOverlay(
    bitrateKbps: Long,
    resolutionLabel: String,
    modifier: Modifier = Modifier
) {
    val bitrateText = if (bitrateKbps > 0) {
        "%.1f Mbps".format(bitrateKbps / 1000.0)
    } else {
        "-- Mbps"
    }

    Text(
        text = "$bitrateText | $resolutionLabel",
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
