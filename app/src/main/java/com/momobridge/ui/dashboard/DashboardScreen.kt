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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momobridge.data.local.SmsTransactionEntity
import com.momobridge.service.RelayConnectionStatus
import com.momobridge.ui.components.EmptyState
import com.momobridge.ui.components.GoldOutlineButton
import com.momobridge.ui.components.MomoBridgeLogo
import com.momobridge.ui.components.TransactionCard
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoShapes
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoTypography

private val ShimmerColor = Color(0xFF1E2748)
private val ShimmerHighlight = Color(0xFF2A3560)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTransactions: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()
    val confirmedCount by viewModel.confirmedCount.collectAsStateWithLifecycle()
    val offsetPx = with(LocalDensity.current) { 24.dp.toPx().toInt() }
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val selectedTransaction by viewModel.selectedTransaction.collectAsStateWithLifecycle()
    val scanningHistorical by viewModel.scanningHistorical.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val isEmpty = transactions.isEmpty() && !scanningHistorical && !isLoading
    val isDisconnected = connectionState.status == RelayConnectionStatus.DISCONNECTED
    val showDisconnectedWarning = isDisconnected && transactions.isNotEmpty()

    selectedTransaction?.let { txn ->
        com.momobridge.ui.components.TransactionDetailDialog(
            txn = txn,
            onDismiss = viewModel::dismissTransaction
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                                    RelayConnectionStatus.CONNECTING,
                                    RelayConnectionStatus.RECONNECTING -> MomoColors.StatusPending
                                    RelayConnectionStatus.DISCONNECTED -> MomoColors.StatusFailed
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(MomoSpacing.Sm))
                    MomoBridgeLogo(fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MomoColors.GroundDark
            )
        )

        if (isLoading) {
            LoadingContent()
        } else {
            SummaryCardsSection(
                pendingCount = pendingCount,
                confirmedCount = confirmedCount
            )

            HorizontalDivider(
                color = MomoColors.GroundLight,
                modifier = Modifier.padding(horizontal = MomoSpacing.Lg)
            )

            if (scanningHistorical) {
                ScanningBanner()
            }

            if (showDisconnectedWarning) {
                DisconnectedWarning()
            }

            if (isEmpty) {
                EmptyState(
                    title = "No transactions yet",
                    subtitle = "Transactions will appear here when SMS payments are received.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = MomoSpacing.Lg),
                    verticalArrangement = Arrangement.spacedBy(MomoSpacing.Sm)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(MomoSpacing.Sm))
                        Text(
                            text = "Recent Transactions",
                            style = MomoTypography.BodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MomoColors.TextPrimary
                        )
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

                    if (transactions.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(MomoSpacing.Sm))
                            GoldOutlineButton(
                                text = "View all transactions \u2192",
                                onClick = onNavigateToTransactions
                            )
                            Spacer(modifier = Modifier.height(MomoSpacing.Lg))
                        }
                    }
                }
            }
        }
    }
}

// ── Summary Cards ─────────────────────────────────────────────────────

@Composable
private fun SummaryCardsSection(
    pendingCount: Int,
    confirmedCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MomoSpacing.Lg)
            .padding(vertical = MomoSpacing.Lg),
        horizontalArrangement = Arrangement.spacedBy(MomoSpacing.Sm)
    ) {
        SummaryCard(
            count = pendingCount,
            label = "Pending",
            icon = Icons.Default.HourglassEmpty,
            color = MomoColors.StatusPending,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            count = confirmedCount,
            label = "Confirmed",
            icon = Icons.Default.CheckCircle,
            color = MomoColors.StatusConfirmed,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    count: Int,
    label: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MomoShapes.BadgeShape,
        colors = CardDefaults.cardColors(containerColor = MomoColors.GroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MomoSpacing.Lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MomoTypography.DisplayMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(MomoSpacing.Xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(MomoSpacing.Xs))
                Text(
                    text = label,
                    style = MomoTypography.LabelSmall,
                    color = MomoColors.TextSecondary
                )
            }
        }
    }
}

// ── Scanning Banner ───────────────────────────────────────────────────

@Composable
private fun ScanningBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MomoSpacing.Lg, vertical = MomoSpacing.Md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp,
            color = MomoColors.Gold
        )
        Spacer(modifier = Modifier.width(MomoSpacing.Sm))
        Text(
            text = "Scanning inbox for past SMS\u2026",
            style = MomoTypography.BodySmall,
            color = MomoColors.TextSecondary
        )
    }
}

// ── Disconnected Warning ──────────────────────────────────────────────

@Composable
private fun DisconnectedWarning() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MomoSpacing.Lg, vertical = MomoSpacing.Md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(MomoColors.StatusFailed.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(MomoColors.StatusFailed, CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(MomoSpacing.Sm))
        Text(
            text = "Relay disconnected \u2014 claims won\u2019t reach your phone.",
            style = MomoTypography.BodySmall,
            color = MomoColors.TextSecondary
        )
    }
}

// ── Loading State ─────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MomoSpacing.Lg)
    ) {
        Spacer(modifier = Modifier.height(MomoSpacing.Lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MomoSpacing.Sm)
        ) {
            ShimmerBlock(
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp)
            )
            ShimmerBlock(
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp)
            )
        }
        Spacer(modifier = Modifier.height(MomoSpacing.Lg))
        ShimmerBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        )
        Spacer(modifier = Modifier.height(MomoSpacing.Sm))
        ShimmerBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        )
        Spacer(modifier = Modifier.height(MomoSpacing.Sm))
        ShimmerBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        )
    }
}

@Composable
private fun ShimmerBlock(modifier: Modifier = Modifier) {
    val brush = Brush.linearGradient(
        colors = listOf(ShimmerColor, ShimmerHighlight, ShimmerColor),
        start = Offset.Zero,
        end = Offset(x = 1000f, y = 1000f)
    )

    Box(
        modifier = modifier
            .clip(MomoShapes.CardShape)
            .background(brush)
    )
}
