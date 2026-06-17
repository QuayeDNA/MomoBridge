package com.momobridge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoTypography
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EmptyState(
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MoMoGeometricLogo(
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MomoTypography.BodyLarge,
                color = MomoColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MomoTypography.BodySmall,
                    color = MomoColors.TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
            if (action != null) {
                Spacer(modifier = Modifier.height(16.dp))
                action()
            }
        }
    }
}

@Composable
fun MoMoGeometricLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val r = size.minDimension / 2.5f

        // Outer hexagon (navy outline)
        val hexPath = androidx.compose.ui.graphics.Path().apply {
            for (i in 0..5) {
                val angle = PI / 3 * i - PI / 6
                val x = cx + r * cos(angle).toFloat()
                val y = cy + r * sin(angle).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(hexPath, color = MomoColors.BorderSubtle, style = Stroke(width = 2.dp.toPx()))

        // Inner gold diamond
        val dr = r * 0.5f
        val diamondPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, cy - dr)
            lineTo(cx + dr, cy)
            lineTo(cx, cy + dr)
            lineTo(cx - dr, cy)
            close()
        }
        drawPath(diamondPath, color = MomoColors.Gold, alpha = 0.9f)

        // Small dot at center
        drawCircle(color = MomoColors.OnGold, radius = 3.dp.toPx(), center = Offset(cx, cy))
    }
}
