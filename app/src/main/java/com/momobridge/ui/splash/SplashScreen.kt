package com.momobridge.ui.splash

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.momobridge.ui.components.MomoBridgeLogo
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoTypography
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    appVersion: String = "1.0.0"
) {
    LaunchedEffect(Unit) {
        delay(2000)
        onFinished()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        MomoBridgeLogo(
            fontSize = 40.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(MomoSpacing.Sm))

        Text(
            text = "Mobile Money Gateway",
            style = MomoTypography.BodySmall,
            color = MomoColors.TextSecondary,
            textAlign = TextAlign.Center
        )

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
