package com.momobridge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoShapes
import com.momobridge.ui.theme.MomoTypography

@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(48.dp),
        enabled = enabled && !loading,
        shape = MomoShapes.ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MomoColors.Gold,
            contentColor = MomoColors.OnGold,
            disabledContainerColor = MomoColors.Gold.copy(alpha = 0.5f),
            disabledContentColor = MomoColors.OnGold.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = MomoColors.OnGold,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = if (loading) "" else text,
            style = MomoTypography.LabelLarge
        )
    }
}

@Composable
fun GoldOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(44.dp),
        enabled = enabled,
        shape = MomoShapes.ButtonShape,
        border = BorderStroke(1.dp, MomoColors.BorderAccent),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MomoColors.Gold,
            disabledContentColor = MomoColors.GoldDim.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        Text(
            text = text,
            style = MomoTypography.LabelLarge
        )
    }
}

@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(48.dp),
        enabled = enabled,
        shape = MomoShapes.ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MomoColors.StatusFailed,
            contentColor = Color.White,
            disabledContainerColor = MomoColors.StatusFailed.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Text(text = text, style = MomoTypography.LabelLarge)
    }
}
