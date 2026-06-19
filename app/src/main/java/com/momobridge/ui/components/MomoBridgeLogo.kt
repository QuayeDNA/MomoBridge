package com.momobridge.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoTypography

@Composable
fun MomoBridgeLogo(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = MomoTypography.TitleLarge.fontSize,
    fontWeight: FontWeight = FontWeight.Bold
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "MoMo",
            style = MomoTypography.TitleLarge.copy(
                fontSize = fontSize,
                fontWeight = fontWeight
            ),
            color = MomoColors.Gold
        )
        Text(
            text = "Bridge",
            style = MomoTypography.TitleLarge.copy(
                fontSize = fontSize,
                fontWeight = fontWeight
            ),
            color = MomoColors.TextPrimary
        )
    }
}
