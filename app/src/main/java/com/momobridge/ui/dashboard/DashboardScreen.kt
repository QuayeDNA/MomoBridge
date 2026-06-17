package com.momobridge.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momobridge.data.local.SmsTransactionEntity
import com.momobridge.service.RelayConnectionStatus
import com.momobridge.ui.components.EmptyState
import com.momobridge.ui.components.GoldOutlineButton
import com.momobridge.ui.components.SectionHeader
import com.momobridge.ui.components.SkeletonList
import com.momobridge.ui.components.TransactionCard
import com.momobridge.ui.components.TransactionDetailDialog
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoShapes
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTransactions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()
    val confirmedCount by viewModel.confirmedCount.collectAsStateWithLifecycle()
    val failedCount by viewModel.failedCount.collectAsStateWithLifecycle()
    val expiredCount by viewModel.expiredCount.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val selectedTransaction by viewModel.selectedTransaction.collectAsStateWithLifecycle()
    val ownerName = viewModel.ownerName
    val scanningHistorical by viewModel.scanningHistorical.collectAsStateWithLifecycle()

    val isEmpty = transactions.isEmpty() && !scanningHistorical

    // Detail dialog
    selectedTransaction?.let { txn ->
        TransactionDetailDialog(
            txn = txn,
            onDismiss = viewModel::dismissTransaction
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (connectionState.status) {
                                    RelayConnectionStatus.CONNECTED -> MomoColors.StatusConfirmed
                                    RelayConnectionStatus.CONNECTING -> MomoColors.StatusPending
                                    RelayConnectionStatus.RECONNECTING -> MomoColors.StatusPending
                                    RelayConnectionStatus.DISCONNECTED -> MomoColors.StatusFailed
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(MomoSpacing.Sm))
                    Text(
                        text = if (ownerName.isNotBlank()) "Hey, $ownerName" else "MoMo Bridge",
                        style = MomoType.TitleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MomoColors.GroundMedium
            ),
            actions = {
                Text(
                    text = "Settings",
                    color = MomoColors.Gold,
                    style = MomoType.LabelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onNavigateToSettings() }
                        .padding(MomoSpacing.Lg)
                )
            }
        )

        if (isEmpty) {
            EmptyState(
                icon = Icons.Default.Inbox,
                title = "No transactions yet",
                subtitle = "Transactions will appear here when SMS payments are received.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MomoSpacing.Lg),
                verticalArrangement = Arrangement.spacedBy(MomoSpacing.Sm)
            ) {
                // Summary card
                item {
                    Spacer(modifier = Modifier.height(MomoSpacing.Sm))
                    SummaryCard(
                        totalCount = totalCount,
                        confirmedCount = confirmedCount,
                        pendingCount = pendingCount,
                        failedCount = failedCount,
                        expiredCount = expiredCount
                    )
                }

                // Historical scan in progress
                if (scanningHistorical) {
                    item {
                        Spacer(modifier = Modifier.height(MomoSpacing.Sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MomoColors.Gold
                            )
                            Spacer(modifier = Modifier.width(MomoSpacing.Sm))
                            Text(
                                text = "Scanning inbox for past transactions...",
                                style = MomoType.BodySmall,
                                color = MomoColors.TextSecondary
                            )
                        }
                    }
                }

                // Recent transactions header
                item {
                    Spacer(modifier = Modifier.height(MomoSpacing.Md))
                    SectionHeader(title = "Recent Transactions")
                }

                items(transactions.take(20)) { txn ->
                    TransactionCard(
                        txn = txn,
                        onClick = { viewModel.selectTransaction(txn) }
                    )
                }

                if (transactions.size > 20) {
                    item {
                        Spacer(modifier = Modifier.height(MomoSpacing.Sm))
                        GoldOutlineButton(
                            text = "View All Transactions",
                            onClick = onNavigateToTransactions
                        )
                        Spacer(modifier = Modifier.height(MomoSpacing.Lg))
                    }
                } else if (transactions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(MomoSpacing.Sm))
                        Text(
                            text = "View all transactions",
                            color = MomoColors.Gold,
                            style = MomoType.BodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { onNavigateToTransactions() }
                                .padding(vertical = MomoSpacing.Sm)
                        )
                        Spacer(modifier = Modifier.height(MomoSpacing.Lg))
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    totalCount: Int,
    confirmedCount: Int,
    pendingCount: Int,
    failedCount: Int,
    expiredCount: Int
) {
    Card(
        shape = MomoShapes.CardShape,
        colors = CardDefaults.cardColors(containerColor = MomoColors.GroundMedium)
    ) {
        Column(modifier = Modifier.padding(MomoSpacing.Lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = totalCount.toString(),
                    style = MomoType.DisplayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MomoColors.TextPrimary
                )
                Spacer(modifier = Modifier.width(MomoSpacing.Sm))
                Text(
                    text = "Total Transactions",
                    style = MomoType.BodyMedium,
                    color = MomoColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(MomoSpacing.Md))
            HorizontalDivider(color = MomoColors.BorderSubtle)
            Spacer(modifier = Modifier.height(MomoSpacing.Md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatLabel("Confirmed", confirmedCount, MomoColors.StatusConfirmed)
                StatLabel("Pending", pendingCount, MomoColors.StatusPending)
                StatLabel("Failed", failedCount, MomoColors.StatusFailed)
            }
            Spacer(modifier = Modifier.height(MomoSpacing.Sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                StatLabel("Expired", expiredCount, MomoColors.StatusExpired)
            }
        }
    }
}

@Composable
private fun StatLabel(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(
            text = count.toString(),
            style = MomoType.TitleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MomoType.LabelSmall,
            color = MomoColors.TextTertiary
        )
    }
}
