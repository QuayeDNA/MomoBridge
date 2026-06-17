package com.momobridge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoTypography

enum class StepState {
    COMPLETED, ACTIVE, PENDING
}

@Composable
fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = MomoSpacing.Lg),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSteps) {
            val state = when {
                i < currentStep -> StepState.COMPLETED
                i == currentStep -> StepState.ACTIVE
                else -> StepState.PENDING
            }
            val color = when (state) {
                StepState.COMPLETED -> MomoColors.StatusConfirmed
                StepState.ACTIVE -> MomoColors.Gold
                StepState.PENDING -> MomoColors.TextTertiary
            }
            val label = when (state) {
                StepState.COMPLETED -> "✓"
                StepState.ACTIVE -> "●"
                StepState.PENDING -> "○"
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MomoTypography.LabelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            if (i < totalSteps - 1) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = 32.dp, height = 2.dp)
                        .background(
                            if (i < currentStep) MomoColors.StatusConfirmed
                            else MomoColors.TextTertiary.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}
