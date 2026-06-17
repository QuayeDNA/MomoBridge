package com.momobridge.ui.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momobridge.data.local.SmsTransactionEntity
import com.momobridge.ui.components.EmptyState
import com.momobridge.ui.components.GoldOutlineButton
import com.momobridge.ui.components.TransactionCard
import com.momobridge.ui.components.TransactionDetailDialog
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoShapes
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val keyLabelFilter by viewModel.keyLabelFilter.collectAsStateWithLifecycle()
    val keyLabels by viewModel.keyLabels.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()
    val confirmedCount by viewModel.confirmedCount.collectAsStateWithLifecycle()
    val failedCount by viewModel.failedCount.collectAsStateWithLifecycle()
    val expiredCount by viewModel.expiredCount.collectAsStateWithLifecycle()
    val hasMore by viewModel.hasMore.collectAsStateWithLifecycle()
    val selectedTransaction by viewModel.selectedTransaction.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()

    val statusFilters = listOf(
        Triple("Pending", SmsTransactionEntity.PENDING, pendingCount),
        Triple("Confirmed", SmsTransactionEntity.CONFIRMED, confirmedCount),
        Triple("Failed", SmsTransactionEntity.FAILED, failedCount),
        Triple("Expired", SmsTransactionEntity.EXPIRED, expiredCount)
    )

    // Detail dialog
    selectedTransaction?.let { txn ->
        TransactionDetailDialog(
            txn = txn,
            onDismiss = viewModel::dismissTransaction
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text("Transaction Log", style = MomoTypography.TitleLarge, fontWeight = FontWeight.Bold)
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MomoColors.GroundMedium
            )
        )

        // Status filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = MomoSpacing.Lg),
            horizontalArrangement = Arrangement.spacedBy(MomoSpacing.Sm)
        ) {
            FilterChip(
                selected = statusFilter == null && keyLabelFilter == null,
                onClick = {
                    viewModel.clearFilter()
                    viewModel.clearKeyLabelFilter()
                },
                label = { Text("All ($totalCount)", style = MomoTypography.LabelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MomoColors.Highlight,
                    selectedLabelColor = MomoColors.Gold
                ),
                shape = MomoShapes.ChipShape
            )
            statusFilters.forEach { (label, status, count) ->
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { viewModel.setFilter(status) },
                    label = { Text("$label ($count)", style = MomoTypography.LabelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MomoColors.Highlight,
                        selectedLabelColor = MomoColors.Gold
                    ),
                    shape = MomoShapes.ChipShape
                )
            }
        }

        // Store filter row (only if there are labels from transactions)
        if (keyLabels.isNotEmpty()) {
            Spacer(modifier = Modifier.height(MomoSpacing.Sm))
            HorizontalDivider(
                color = MomoColors.BorderSubtle,
                modifier = Modifier.padding(horizontal = MomoSpacing.Lg)
            )
            Spacer(modifier = Modifier.height(MomoSpacing.Sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = MomoSpacing.Lg),
                horizontalArrangement = Arrangement.spacedBy(MomoSpacing.Sm)
            ) {
                keyLabels.forEach { label ->
                    FilterChip(
                        selected = keyLabelFilter == label,
                        onClick = { viewModel.setKeyLabelFilter(label) },
                        label = { Text(label, style = MomoTypography.LabelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MomoColors.Highlight,
                            selectedLabelColor = MomoColors.Gold
                        ),
                        shape = MomoShapes.ChipShape
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(MomoSpacing.Sm))

        if (transactions.isEmpty()) {
            EmptyState(
                title = "No transactions",
                subtitle = if (statusFilter != null || keyLabelFilter != null) "No transactions match the selected filters." else "Transactions will appear here when SMS payments are received.",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MomoSpacing.Lg),
                verticalArrangement = Arrangement.spacedBy(MomoSpacing.Sm)
            ) {
                items(transactions) { txn ->
                    TransactionCard(
                        txn = txn,
                        showFullDate = true,
                        onClick = { viewModel.selectTransaction(txn) }
                    )
                }
                if (hasMore) {
                    item {
                        Spacer(modifier = Modifier.height(MomoSpacing.Sm))
                        GoldOutlineButton(
                            text = "Load More",
                            onClick = viewModel::loadMore
                        )
                        Spacer(modifier = Modifier.height(MomoSpacing.Lg))
                    }
                } else {
                    item {
                        Spacer(modifier = Modifier.height(MomoSpacing.Lg))
                    }
                }
            }
        }
    }
}
