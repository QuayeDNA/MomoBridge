package com.momobridge.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.momobridge.domain.model.SmsSource
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoShapes
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoType

@Composable
fun SmsSourceCard(
    source: SmsSource,
    onToggle: (Boolean) -> Unit,
    onConfigure: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MomoShapes.CardShape,
        colors = CardDefaults.cardColors(containerColor = MomoColors.GroundMedium),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MomoSpacing.CardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = source.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MomoColors.Gold,
                    checkedTrackColor = MomoColors.Gold.copy(alpha = 0.3f),
                    uncheckedThumbColor = MomoColors.TextTertiary,
                    uncheckedTrackColor = MomoColors.GroundElevated
                )
            )
            Spacer(modifier = Modifier.width(MomoSpacing.Sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.label,
                    style = MomoType.BodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MomoColors.TextPrimary
                )
                Text(
                    text = if (source.parsingRule != null) "Parsing configured" else source.senderAddress,
                    style = MomoType.LabelSmall,
                    color = if (source.parsingRule != null) MomoColors.StatusConfirmed else MomoColors.TextTertiary
                )
            }
            if (source.parsingRule != null) {
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
