package com.momobridge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoTypography

enum class StepState {
    COMPLETED, ACTIVE, PENDING
}

@Composable
fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList()
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until totalSteps) {
                if (i > 0) {
                    ConnectorLine(
                        completed = i <= currentStep,
                        modifier = Modifier.weight(0.15f)
                    )
                }
                StepDot(
                    index = i,
                    currentStep = currentStep,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (labels.size == totalSteps) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                for (i in 0 until totalSteps) {
                    if (i > 0) {
                        Box(modifier = Modifier.weight(0.15f))
                    }
                    Text(
                        text = labels[i],
                        style = MomoTypography.LabelSmall.copy(fontSize = 10.sp),
                        fontWeight = if (i == currentStep) FontWeight.SemiBold else FontWeight.Normal,
                        color = when {
                            i <= currentStep -> MomoColors.TextSecondary
                            else -> MomoColors.TextTertiary
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.StepDot(
    index: Int,
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    val state = when {
        index < currentStep -> StepState.COMPLETED
        index == currentStep -> StepState.ACTIVE
        else -> StepState.PENDING
    }
    val color = when (state) {
        StepState.COMPLETED -> MomoColors.StatusConfirmed
        StepState.ACTIVE -> MomoColors.Gold
        StepState.PENDING -> MomoColors.TextTertiary
    }
    val label = when (state) {
        StepState.COMPLETED -> "\u2713"
        StepState.ACTIVE -> "\u25CF"
        StepState.PENDING -> "\u25CB"
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
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
    }
}

@Composable
private fun RowScope.ConnectorLine(
    completed: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(2.dp)
            .background(
                if (completed) MomoColors.StatusConfirmed
                else MomoColors.TextTertiary.copy(alpha = 0.3f)
            )
    )
}
