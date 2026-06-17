package com.momobridge.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
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
import com.momobridge.ui.components.StatusBadge
import com.momobridge.ui.components.TransactionCard
import com.momobridge.ui.components.TransactionDetailDialog
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTransactions: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()
    val confirmedCount by viewModel.confirmedCount.collectAsStateWithLifecycle()
    val offsetPx = with(LocalDensity.current) { 24.dp.toPx().toInt() }
    val failedCount by viewModel.failedCount.collectAsStateWithLifecycle()
    val expiredCount by viewModel.expiredCount.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val selectedTransaction by viewModel.selectedTransaction.collectAsStateWithLifecycle()
    val ownerName = viewModel.ownerName
    val scanningHistorical by viewModel.scanningHistorical.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val isEmpty = transactions.isEmpty() && !scanningHistorical && !isLoading

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
                        text = "MoMo Bridge",
                        style = MomoTypography.TitleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MomoColors.GroundMedium
            )
        )

        if (isLoading) {
            SkeletonList(
                count = 5,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MomoSpacing.Lg)
            )
        } else if (isEmpty) {
            EmptyState(
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
                // Status badges row
                item {
                    Spacer(modifier = Modifier.height(MomoSpacing.Sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MomoSpacing.Sm)
                    ) {
                        StatusBadge(
                            icon = Icons.Default.HourglassEmpty,
                            label = "Pending",
                            count = pendingCount,
                            color = MomoColors.StatusPending,
                            modifier = Modifier.weight(1f)
                        )
                        StatusBadge(
                            icon = Icons.Default.CheckCircle,
                            label = "Confirmed",
                            count = confirmedCount,
                            color = MomoColors.StatusConfirmed,
                            modifier = Modifier.weight(1f)
                        )
                    }
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
                                style = MomoTypography.BodySmall,
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

                itemsIndexed(transactions.take(10)) { index, txn ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(
                            initialOffsetY = { offsetPx },
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 350,
                                delayMillis = index * 50,
                                easing = com.momobridge.ui.theme.MomoMotion.EaseOutExpo
                            )
                        ) + fadeIn(
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 350,
                                delayMillis = index * 50
                            )
                        )
                    ) {
                        TransactionCard(
                            txn = txn,
                            onClick = { viewModel.selectTransaction(txn) }
                        )
                    }
                }

                if (transactions.size > 10) {
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
                            style = MomoTypography.BodyMedium,
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
