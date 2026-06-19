package com.momobridge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momobridge.data.local.SmsTransactionEntity
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoShapes
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StatusVisual(
    val color: Color,
    val icon: ImageVector,
    val label: String
)

fun statusVisual(status: String): StatusVisual = when (status) {
    SmsTransactionEntity.CONFIRMED -> StatusVisual(MomoColors.StatusConfirmed, Icons.Default.CheckCircle, "CONFIRMED")
    SmsTransactionEntity.PENDING -> StatusVisual(MomoColors.StatusPending, Icons.Default.HourglassEmpty, "PENDING")
    SmsTransactionEntity.FAILED -> StatusVisual(MomoColors.StatusFailed, Icons.Default.Error, "FAILED")
    SmsTransactionEntity.EXPIRED -> StatusVisual(MomoColors.StatusExpired, Icons.Default.Timelapse, "EXPIRED")
    else -> StatusVisual(Color.Gray, Icons.Default.HourglassEmpty, status.uppercase())
}

fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("dd MMM", Locale.US).format(Date(timestamp))
    }
}

@Composable
fun TransactionCard(
    txn: SmsTransactionEntity,
    modifier: Modifier = Modifier,
    showFullDate: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val visual = statusVisual(txn.status)
    val isConfirmed = txn.status == SmsTransactionEntity.CONFIRMED
    val isClaimed = isConfirmed && txn.claimedByKeyLabel != null
    val timeStr = if (showFullDate) {
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US).format(Date(txn.receivedAt))
    } else {
        formatTimeAgo(txn.receivedAt)
    }
    val borderColor = when {
        isClaimed -> MomoColors.Gold
        isConfirmed -> MomoColors.StatusConfirmed
        else -> MomoColors.BorderSubtle
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = MomoShapes.CardShape,
        colors = CardDefaults.cardColors(containerColor = MomoColors.GroundMedium),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MomoSpacing.Lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = txn.network,
                    style = MomoTypography.BodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MomoColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "GH\u20B5${"%.2f".format(txn.amount)}",
                    style = MomoTypography.AmountMono,
                    color = MomoColors.TextPrimary
                )
            }

            if (txn.senderName != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "from ${txn.senderName}",
                    style = MomoTypography.BodySmall,
                    color = MomoColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(MomoSpacing.Xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(visual.color)
                )
                Spacer(modifier = Modifier.width(MomoSpacing.Xs))
                Text(
                    text = if (isClaimed) "CLAIMED" else visual.label,
                    style = MomoTypography.LabelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isClaimed) MomoColors.Gold else visual.color
                )

                if (isClaimed) {
                    Spacer(modifier = Modifier.width(MomoSpacing.Sm))
                    Text(
                        text = "\u00B7",
                        style = MomoTypography.LabelSmall,
                        color = MomoColors.TextTertiary
                    )
                    Spacer(modifier = Modifier.width(MomoSpacing.Sm))
                    Text(
                        text = txn.claimedByKeyLabel ?: "",
                        style = MomoTypography.LabelSmall,
                        color = MomoColors.Gold
                    )
                }

                Spacer(modifier = Modifier.width(MomoSpacing.Sm))
                Text(
                    text = "\u00B7",
                    style = MomoTypography.LabelSmall,
                    color = MomoColors.TextTertiary
                )
                Spacer(modifier = Modifier.width(MomoSpacing.Sm))
                Text(
                    text = timeStr,
                    style = MomoTypography.LabelSmall,
                    color = MomoColors.TextTertiary
                )
            }
        }
    }
}
