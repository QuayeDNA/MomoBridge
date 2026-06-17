package com.momobridge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.momobridge.data.local.SmsTransactionEntity
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoShapes
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailDialog(
    txn: SmsTransactionEntity,
    onDismiss: () -> Unit
) {
    val visual = statusVisual(txn.status)
    val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US)
    val timeStr = dateFormat.format(Date(txn.receivedAt))
    val createdStr = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US)
        .format(Date(txn.createdAt))
    val isConfirmed = txn.status == SmsTransactionEntity.CONFIRMED
    val isClaimed = isConfirmed && txn.claimedByKeyLabel != null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MomoColors.GroundMedium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MomoSpacing.Lg)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction Details",
                        style = MomoType.TitleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MomoColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MomoColors.TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(MomoSpacing.Md))

                // Network badge + amount
                val borderStroke = if (isConfirmed) BorderStroke(1.dp, if (isClaimed) MomoColors.Gold else MomoColors.StatusConfirmed) else null
                Card(
                    shape = MomoShapes.CardShape,
                    colors = CardDefaults.cardColors(containerColor = MomoColors.GroundLight),
                    border = borderStroke
                ) {
                    Column(modifier = Modifier.padding(MomoSpacing.Lg)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = txn.network,
                                style = MomoType.TitleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MomoColors.TextSecondary
                            )
                            if (isConfirmed) {
                                Spacer(modifier = Modifier.width(MomoSpacing.Sm))
                                Text(
                                    text = if (isClaimed) "Claimed" else "CONFIRMED",
                                    style = MomoType.LabelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isClaimed) MomoColors.Gold else MomoColors.StatusConfirmed
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(MomoSpacing.Sm))
                        Text(
                            text = "GH₵${"%.2f".format(txn.amount)}",
                            style = MomoType.DisplayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MomoColors.TextPrimary
                        )
                        if (txn.senderName != null) {
                            Spacer(modifier = Modifier.height(MomoSpacing.Xs))
                            Text(
                                text = "from ${txn.senderName}",
                                style = MomoType.BodyMedium,
                                color = MomoColors.TextSecondary
                            )
                        }
                        if (isConfirmed && txn.confirmedAt != null) {
                            Spacer(modifier = Modifier.height(MomoSpacing.Xs))
                            Text(
                                text = "${if (isClaimed) "Claimed" else "Confirmed"} " + SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US).format(Date(txn.confirmedAt)),
                                style = MomoType.BodySmall,
                                color = if (isClaimed) MomoColors.Gold else MomoColors.StatusConfirmed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MomoSpacing.Lg))

                // Details section
                SectionHeader(title = "Details")
                Spacer(modifier = Modifier.height(MomoSpacing.Sm))

                DetailRow("Reference", txn.reference, highlight = true)
                DetailRow("Network", txn.network)
                DetailRow("Status", if (isClaimed) "Claimed" else visual.label, if (isClaimed) MomoColors.Gold else visual.color)
                if (txn.claimedByKeyLabel != null) {
                    DetailRow("Claimed Via", txn.claimedByKeyLabel!!, MomoColors.Gold)
                }
                if (txn.senderPhone != null) {
                    DetailRow("Sender Phone", txn.senderPhone, highlight = true)
                }
                if (txn.balanceAfter != null) {
                    DetailRow("Balance After", "GH₵${"%.2f".format(txn.balanceAfter)}", highlight = true)
                }
                DetailRow("Logged", timeStr)
                DetailRow("Created", createdStr)

                Spacer(modifier = Modifier.height(MomoSpacing.Lg))

                // Raw SMS section
                SectionHeader(title = "Parsed SMS")
                Spacer(modifier = Modifier.height(MomoSpacing.Sm))

                Card(
                    shape = MomoShapes.CardShape,
                    colors = CardDefaults.cardColors(containerColor = MomoColors.GroundElevated)
                ) {
                    Text(
                        text = txn.rawSms,
                        style = MomoType.BodySmall,
                        color = MomoColors.TextSecondary,
                        modifier = Modifier.padding(MomoSpacing.CardPadding)
                    )
                }

                Spacer(modifier = Modifier.height(MomoSpacing.Lg))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = MomoColors.TextPrimary, highlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MomoSpacing.Xxs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MomoType.BodySmall,
            color = MomoColors.TextTertiary,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MomoType.BodySmall,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
            color = if (highlight) MomoColors.StatusConfirmed else valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}


