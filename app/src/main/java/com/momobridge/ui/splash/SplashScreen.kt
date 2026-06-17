package com.momobridge.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoTypography
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    appVersion: String = "1.0.0"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LaunchedEffect(Unit) {
        delay(2000)
        onFinished()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Push content to vertical center
        Spacer(modifier = Modifier.weight(1f))

        LogoCircle(size = 88.dp)

        Spacer(modifier = Modifier.height(MomoSpacing.Xl))

        Text(
            text = "MoMo Bridge",
            style = MomoTypography.TitleLarge,
            color = MomoColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MomoSpacing.Xs))

        Text(
            text = "Mobile Money Gateway",
            style = MomoTypography.BodySmall,
            color = MomoColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MomoSpacing.Xxl))

        DotRow(alpha = pulseAlpha)

        // Push version to bottom
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "v$appVersion · DNA Studios",
            style = MomoTypography.LabelSmall,
            color = MomoColors.TextTertiary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MomoSpacing.Xl))
    }
}

@Composable
private fun LogoCircle(size: Dp) {
    val gold = MomoColors.Gold
    Canvas(modifier = Modifier.size(size)) {
        val cx = size.toPx() / 2f
        val cy = size.toPx() / 2f
        val r = size.toPx() / 2f - 2f

        drawCircle(color = gold.copy(alpha = 0.12f), radius = r + 1f)
        drawCircle(color = gold, radius = r, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
    }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "MB",
            style = MomoTypography.DisplayLarge,
            fontWeight = FontWeight.Bold,
            color = MomoColors.Gold
        )
    }
}

@Composable
private fun DotRow(alpha: Float) {
    val dotColor = MomoColors.Gold.copy(alpha = alpha)
    Canvas(modifier = Modifier.size(width = 40.dp, height = 8.dp)) {
        val dotRadius = 3.dp.toPx()
        val spacing = 16.dp.toPx()
        val startX = (size.width - spacing * 2) / 2f
        for (i in 0..2) {
            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = Offset(startX + spacing * i, size.height / 2f)
            )
        }
    }
}
