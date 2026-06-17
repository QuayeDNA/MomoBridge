package com.momobridge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun TransactionCard(
    txn: SmsTransactionEntity,
    modifier: Modifier = Modifier,
    showFullDate: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val visual = statusVisual(txn.status)
    val dateFormat = if (showFullDate) {
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US)
    } else {
        SimpleDateFormat("dd MMM HH:mm", Locale.US)
    }
    val timeStr = dateFormat.format(Date(txn.receivedAt))

    val isConfirmed = txn.status == SmsTransactionEntity.CONFIRMED
    val isClaimed = isConfirmed && txn.claimedByKeyLabel != null
    val statusText = if (isClaimed) "CLAIMED" else visual.label
    val statusColor = if (isClaimed) MomoColors.Gold else visual.color
    val borderColor = if (isClaimed) MomoColors.Gold else if (isConfirmed) MomoColors.StatusConfirmed else MomoColors.BorderSubtle

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = MomoShapes.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MomoColors.GroundMedium
        ),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MomoSpacing.CardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${txn.network} · ",
                        style = MomoTypography.TitleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MomoColors.TextPrimary
                    )
                    Text(
                        text = "GH₵${"%.2f".format(txn.amount)}",
                        style = MomoTypography.AmountMono
                    )
                    if (isConfirmed) {
                        Spacer(modifier = Modifier.width(MomoSpacing.Sm))
                        if (isClaimed) {
                            Box(
                                modifier = Modifier
                                    .background(MomoColors.Highlight, RoundedCornerShape(4.dp))
                                    .padding(horizontal = MomoSpacing.Xs, vertical = MomoSpacing.Xxs)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "CLAIMED",
                                        style = MomoTypography.LabelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MomoColors.Gold
                                    )
                                    Spacer(modifier = Modifier.width(MomoSpacing.Xs))
                                    Text(
                                        text = "· ${txn.claimedByKeyLabel}",
                                        style = MomoTypography.LabelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MomoColors.Gold
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "CONFIRMED",
                                style = MomoTypography.LabelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MomoColors.StatusConfirmed
                            )
                        }
                    }
                }
                if (txn.senderName != null) {
                    Text(
                        text = "from ${txn.senderName}",
                        style = MomoTypography.BodySmall,
                        color = MomoColors.TextSecondary
                    )
                }
                Text(
                    text = txn.reference,
                    style = MomoTypography.BodySmall,
                    color = MomoColors.TextSecondary
                )
                Text(
                    text = timeStr,
                    style = MomoTypography.LabelSmall,
                    color = MomoColors.TextTertiary
                )
            }
        }
    }
}
