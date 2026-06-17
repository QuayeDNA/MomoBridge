package com.momobridge.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momobridge.domain.model.SmsSource
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoShapes
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoTypography

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SmsSourceCard(
    source: SmsSource,
    onToggle: (Boolean) -> Unit,
    onConfigure: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = onConfigure,
    onLongClick: () -> Unit = onDelete
) {
    val isConfigured = source.parsingRule != null
    Card(
        shape = MomoShapes.CardShape,
        colors = CardDefaults.cardColors(containerColor = MomoColors.GroundMedium),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MomoSpacing.CardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Health dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isConfigured) MomoColors.StatusConfirmed else MomoColors.TextTertiary)
            )
            Spacer(modifier = Modifier.width(MomoSpacing.Sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.label,
                    style = MomoTypography.TitleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MomoColors.TextPrimary
                )
                Text(
                    text = if (isConfigured) "Configured" else source.senderAddress,
                    style = MomoTypography.LabelSmall,
                    color = if (isConfigured) MomoColors.StatusConfirmed else MomoColors.TextTertiary
                )
            }
            if (isConfigured) {
                IconButton(onClick = onConfigure) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Configure",
                        tint = MomoColors.Gold
                    )
                }
            } else {
                IconButton(onClick = onConfigure) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Configure",
                        tint = MomoColors.TextTertiary
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MomoColors.StatusFailed
                )
            }
        }
    }
}
