package com.momobridge.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momobridge.service.RelayConnectionStatus
import com.momobridge.service.RelayState
import com.momobridge.ui.components.DangerButton
import com.momobridge.ui.components.GoldButton
import com.momobridge.ui.components.GoldOutlineButton
import com.momobridge.ui.components.SmsSourceCard
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoShapes
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoTypography

private enum class SettingsCategory { SENDERS, CONNECTION, RULES, ABOUT }

@Composable
fun SettingsScreen(
    onNavigateToHelp: () -> Unit,
    onNavigateToSenderConfig: (String, String) -> Unit,
    onReconfigure: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val relayState by viewModel.connectionState.collectAsStateWithLifecycle()
    var category by remember { mutableStateOf<SettingsCategory?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MomoColors.GroundDark)
    ) {
        when (category) {
            null -> SettingsMenu(
                state = state,
                relayState = relayState,
                onSelect = { category = it }
            )
            SettingsCategory.SENDERS -> SendersSettingsContent(
                state = state,
                onBack = {
                    viewModel.hideScanResults()
                    category = null
                },
                onToggle = viewModel::toggleSource,
                onConfigure = onNavigateToSenderConfig,
                onDelete = viewModel::removeSource,
                onAddManual = viewModel::addSourceManually,
                onScan = viewModel::scanInbox,
                onAddFromScan = viewModel::addSourceFromScan,
                onHideScan = viewModel::hideScanResults
            )
            SettingsCategory.CONNECTION -> ConnectionSettingsContent(
                relayState = relayState,
                relayUrl = viewModel.relayUrl,
                apiKey = viewModel.apiKey,
                onBack = { category = null },
                onUpdateUrl = viewModel::updateRelayUrl,
                onReconnect = viewModel::reconnect
            )
            SettingsCategory.RULES -> RulesSettingsContent(
                expiryEnabled = state.expiryEnabled,
                expiryHours = state.expiryHours,
                scanningHistorical = state.scanningHistorical,
                historicalScanResult = state.historicalScanResult,
                onBack = {
                    viewModel.clearHistoricalScanResult()
                    category = null
                },
                onToggleExpiry = viewModel::setExpiryEnabled,
                onHoursChange = viewModel::setExpiryHours,
                onScanHistorical = viewModel::scanHistoricalTransactions,
                onClearResult = viewModel::clearHistoricalScanResult
            )
            SettingsCategory.ABOUT -> AboutSettingsContent(
                onBack = { category = null },
                onHelp = onNavigateToHelp,
                onReset = onReconfigure
            )
        }
    }
}

// ─────────────────────── Menu ───────────────────────

@Composable
private fun SettingsMenu(
    state: SettingsUiState,
    relayState: RelayState,
    onSelect: (SettingsCategory) -> Unit
) {
    val activeSenders = state.smsSources.count { it.enabled }
    val totalSenders = state.smsSources.size
    val sendersSummary = if (totalSenders == 0) "None configured"
        else "$activeSenders of $totalSenders active"

    val connected = relayState.status == RelayConnectionStatus.CONNECTED
    val connectionSummary = if (connected) "Connected to relay" else "Disconnected"

    val expirySummary = if (state.expiryEnabled) "Expires after ${state.expiryHours}h"
        else "Never expires"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MomoSpacing.Lg)
    ) {
        Spacer(modifier = Modifier.height(MomoSpacing.Lg))
        Text(
            text = "Settings",
            style = MomoTypography.TitleLarge,
            fontWeight = FontWeight.Bold,
            color = MomoColors.Gold
        )
        Spacer(modifier = Modifier.height(MomoSpacing.Xl))

        SettingsCategoryRow(
            title = "Senders & Parsing",
            summary = sendersSummary,
            onClick = { onSelect(SettingsCategory.SENDERS) }
        )
        SettingsDivider()

        SettingsCategoryRow(
            title = "Connection",
            summary = connectionSummary,
            onClick = { onSelect(SettingsCategory.CONNECTION) }
        )
        SettingsDivider()

        SettingsCategoryRow(
            title = "Transaction Rules",
            summary = expirySummary,
            onClick = { onSelect(SettingsCategory.RULES) }
        )
        SettingsDivider()

        SettingsCategoryRow(
            title = "About",
            summary = "v1.0.0",
            onClick = { onSelect(SettingsCategory.ABOUT) }
        )
    }
}

@Composable
private fun SettingsCategoryRow(
    title: String,
    summary: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = MomoSpacing.Lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MomoTypography.TitleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MomoColors.TextPrimary
            )
            Text(
                text = summary,
                style = MomoTypography.LabelSmall,
                color = MomoColors.TextTertiary
            )
        }
        Spacer(modifier = Modifier.width(MomoSpacing.Sm))
        Text(
            text = "›",
            style = MomoTypography.TitleMedium,
            color = MomoColors.Gold
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MomoColors.BorderSubtle,
        thickness = 1.dp
    )
}

// ─────────────────── Sub-screen header ───────────────────

@Composable
private fun SubScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MomoSpacing.Lg, vertical = MomoSpacing.Md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onBack),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MomoColors.Gold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(MomoSpacing.Xs))
            Text(
                text = "Settings",
                style = MomoTypography.LabelSmall,
                color = MomoColors.Gold
            )
        }
        Spacer(modifier = Modifier.width(MomoSpacing.Lg))
        Text(
            text = title,
            style = MomoTypography.TitleLarge,
            fontWeight = FontWeight.Bold,
            color = MomoColors.TextPrimary
        )
    }
}

// ─────────────────── Senders & Parsing ───────────────────

@Composable
private fun SendersSettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onConfigure: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onAddManual: (String, String) -> Unit,
    onScan: () -> Unit,
    onAddFromScan: (String) -> Unit,
    onHideScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SubScreenHeader(title = "Senders & Parsing", onBack = onBack)

        if (state.showScanResults) {
            ScanResultDialog(
                senders = state.scannedSenders,
                loading = state.scanningInbox,
                onAdd = onAddFromScan,
                onDismiss = onHideScan
            )
        }

        Spacer(modifier = Modifier.height(MomoSpacing.Sm))

        if (state.smsSources.isEmpty()) {
            Text(
                text = "No senders configured yet. Add a sender or scan your inbox to get started.",
                style = MomoTypography.BodyMedium,
                color = MomoColors.TextSecondary,
                modifier = Modifier.padding(horizontal = MomoSpacing.Lg)
            )
            Spacer(modifier = Modifier.height(MomoSpacing.Lg))
        } else {
            Column(
                modifier = Modifier.padding(horizontal = MomoSpacing.Lg),
                verticalArrangement = Arrangement.spacedBy(MomoSpacing.Sm)
            ) {
                state.smsSources.forEach { source ->
                    SmsSourceCard(
                        source = source,
                        onToggle = { onToggle(source.id, it) },
                        onConfigure = { onConfigure(source.senderAddress, source.label) },
                        onDelete = { onDelete(source.id) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(MomoSpacing.Md))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MomoSpacing.Lg),
            horizontalArrangement = Arrangement.spacedBy(MomoSpacing.Sm)
        ) {
            OutlinedButton(
                onClick = onScan,
                modifier = Modifier.weight(1f),
                enabled = !state.scanningInbox,
                shape = MomoShapes.ButtonShape
            ) {
                if (state.scanningInbox) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MomoColors.Gold
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(MomoSpacing.Sm))
                Text("Scan Inbox", style = MomoTypography.LabelSmall)
            }
            AddSenderButton(
                modifier = Modifier.weight(1f),
                onAdd = onAddManual
            )
        }
    }
}

// ─────────────────── Connection ───────────────────

@Composable
private fun ConnectionSettingsContent(
    relayState: RelayState,
    relayUrl: String,
    apiKey: String,
    onBack: () -> Unit,
    onUpdateUrl: (String) -> Unit,
    onReconnect: () -> Unit
) {
    var showUrlDialog by remember { mutableStateOf(false) }

    if (showUrlDialog) {
        RelayUrlEditDialog(
            currentUrl = relayUrl,
            onSave = { url ->
                onUpdateUrl(url)
                showUrlDialog = false
            },
            onDismiss = { showUrlDialog = false }
        )
    }

    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SubScreenHeader(title = "Connection", onBack = onBack)

        Spacer(modifier = Modifier.height(MomoSpacing.Lg))

        Column(modifier = Modifier.padding(horizontal = MomoSpacing.Lg)) {
            // Status
            val statusColor = when (relayState.status) {
                RelayConnectionStatus.CONNECTED -> MomoColors.StatusConfirmed
                RelayConnectionStatus.CONNECTING, RelayConnectionStatus.RECONNECTING -> MomoColors.StatusPending
                RelayConnectionStatus.DISCONNECTED -> MomoColors.StatusFailed
            }
            val statusLabel = when (relayState.status) {
                RelayConnectionStatus.CONNECTED -> "Connected"
                RelayConnectionStatus.CONNECTING -> "Connecting…"
                RelayConnectionStatus.RECONNECTING -> "Reconnecting…"
                RelayConnectionStatus.DISCONNECTED -> "Disconnected"
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(MomoSpacing.Sm))
                Text(
                    text = statusLabel,
                    style = MomoTypography.BodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onReconnect) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reconnect",
                        tint = MomoColors.Gold
                    )
                }
            }

            // Relay URL
            if (relayUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(MomoSpacing.Md))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showUrlDialog = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = relayUrl,
                        style = MomoTypography.BodySmall,
                        color = MomoColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(MomoSpacing.Sm))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit relay URL",
                        tint = MomoColors.Gold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // API Key
            if (apiKey.isNotBlank()) {
                Spacer(modifier = Modifier.height(MomoSpacing.Xl))
                Text(
                    text = "API Key",
                    style = MomoTypography.TitleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MomoColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(MomoSpacing.Sm))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MomoColors.GroundMedium, MomoShapes.CardShape)
                        .padding(MomoSpacing.CardPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = apiKey,
                        style = MomoTypography.BodySmall,
                        color = MomoColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(MomoSpacing.Sm))
                    IconButton(onClick = { clipboard.setText(AnnotatedString(apiKey)) }) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy API key",
                            tint = MomoColors.Gold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────── Transaction Rules ───────────────────

@Composable
private fun RulesSettingsContent(
    expiryEnabled: Boolean,
    expiryHours: Long,
    scanningHistorical: Boolean,
    historicalScanResult: String?,
    onBack: () -> Unit,
    onToggleExpiry: (Boolean) -> Unit,
    onHoursChange: (Long) -> Unit,
    onScanHistorical: () -> Unit,
    onClearResult: () -> Unit
) {
    var hoursText by remember(expiryHours) { mutableStateOf(expiryHours.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SubScreenHeader(title = "Transaction Rules", onBack = onBack)

        Spacer(modifier = Modifier.height(MomoSpacing.Lg))

        Column(modifier = Modifier.padding(horizontal = MomoSpacing.Lg)) {
            // Expiry toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-expiry",
                        style = MomoTypography.TitleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MomoColors.TextPrimary
                    )
                    Text(
                        text = if (expiryEnabled) "Transactions expire after ${expiryHours}h"
                        else "Transactions never expire",
                        style = MomoTypography.LabelSmall,
                        color = MomoColors.TextSecondary
                    )
                }
                Switch(
                    checked = expiryEnabled,
                    onCheckedChange = onToggleExpiry,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MomoColors.Gold,
                        checkedThumbColor = MomoColors.OnGold
                    )
                )
            }

            if (expiryEnabled) {
                Spacer(modifier = Modifier.height(MomoSpacing.Md))
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { input ->
                        hoursText = input
                        input.toLongOrNull()?.let { hours ->
                            if (hours in 1..8760) onHoursChange(hours)
                        }
                    },
                    label = { Text("Expiry Period (hours)") },
                    placeholder = { Text("168") },
                    supportingText = {
                        val days = expiryHours / 24
                        val remainder = expiryHours % 24
                        Text(
                            if (days > 0) "${days}d ${remainder}h" else "${expiryHours}h",
                            color = MomoColors.TextSecondary
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(MomoSpacing.Xl))

            // Historical scan
            Text(
                text = "Past Transactions",
                style = MomoTypography.TitleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MomoColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(MomoSpacing.Sm))
            Text(
                text = "Scan your inbox for past money received messages from monitored senders (up to 2 months back).",
                style = MomoTypography.BodySmall,
                color = MomoColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(MomoSpacing.Md))

            GoldButton(
                text = if (scanningHistorical) "Scanning…" else "Scan Inbox for Past Transactions",
                onClick = onScanHistorical,
                enabled = !scanningHistorical,
                loading = scanningHistorical
            )

            if (historicalScanResult != null) {
                Spacer(modifier = Modifier.height(MomoSpacing.Sm))
                Text(
                    text = historicalScanResult,
                    style = MomoTypography.BodySmall,
                    color = MomoColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(MomoSpacing.Sm))
                TextButton(onClick = onClearResult) {
                    Text("Dismiss", color = MomoColors.Gold)
                }
            }
        }
    }
}

// ─────────────────── About ───────────────────

@Composable
private fun AboutSettingsContent(
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onReset: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        ResetConfirmDialog(
            onConfirm = {
                showResetDialog = false
                onReset()
            },
            onDismiss = { showResetDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SubScreenHeader(title = "About", onBack = onBack)

        Spacer(modifier = Modifier.height(MomoSpacing.Xl))

        Column(modifier = Modifier.padding(horizontal = MomoSpacing.Lg)) {
            Text(
                text = "MoMo Bridge",
                style = MomoTypography.TitleLarge,
                fontWeight = FontWeight.Bold,
                color = MomoColors.Gold
            )
            Spacer(modifier = Modifier.height(MomoSpacing.Xs))
            Text(
                text = "Version 1.0.0",
                style = MomoTypography.BodyMedium,
                color = MomoColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(MomoSpacing.Xl))

            GoldOutlineButton(
                text = "Help & Support",
                onClick = onHelp,
                fullWidth = true
            )

            Spacer(modifier = Modifier.height(MomoSpacing.Xl))

            HorizontalDivider(color = MomoColors.BorderSubtle, thickness = 1.dp)

            Spacer(modifier = Modifier.height(MomoSpacing.Lg))
            DangerButton(
                text = "Reset All Settings",
                onClick = { showResetDialog = true },
                fullWidth = true
            )
        }
    }
}

// ─────────────────── Dialogs ───────────────────

@Composable
private fun AddSenderButton(modifier: Modifier = Modifier, onAdd: (String, String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var addr by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = modifier,
        shape = MomoShapes.ButtonShape
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(MomoSpacing.Sm))
        Text("Add Manually", style = MomoTypography.LabelSmall)
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add Sender") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(MomoSpacing.Sm)) {
                    OutlinedTextField(
                        value = addr,
                        onValueChange = { addr = it },
                        label = { Text("Sender Address") },
                        placeholder = { Text("e.g. MobileMoney, T-CASH") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Label (optional)") },
                        placeholder = { Text("e.g. MTN Mobile Money") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onAdd(addr, label)
                    showDialog = false
                    addr = ""
                    label = ""
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ScanResultDialog(
    senders: List<ScannedSender>,
    loading: Boolean,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SMS Senders Found") },
        text = {
            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MomoSpacing.Lg),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MomoColors.Gold)
                }
            } else if (senders.isEmpty()) {
                Text(
                    "No SMS senders found in your inbox.",
                    style = MomoTypography.BodySmall,
                    color = MomoColors.TextSecondary
                )
            } else {
                Column {
                    Text(
                        "Tap a sender to add it to monitoring.",
                        style = MomoTypography.BodySmall,
                        color = MomoColors.TextSecondary
                    )
                    Spacer(Modifier.height(MomoSpacing.Sm))
                    Column(verticalArrangement = Arrangement.spacedBy(MomoSpacing.Xs)) {
                        senders.forEach { sender ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAdd(sender.address) }
                                    .padding(vertical = MomoSpacing.Sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        sender.address,
                                        style = MomoTypography.BodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MomoColors.TextPrimary
                                    )
                                    Text(
                                        "${sender.messageCount} messages",
                                        style = MomoTypography.LabelSmall,
                                        color = MomoColors.TextSecondary
                                    )
                                }
                                Spacer(Modifier.width(MomoSpacing.Sm))
                                Icon(Icons.Default.Add, contentDescription = null, tint = MomoColors.Gold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun RelayUrlEditDialog(
    currentUrl: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Relay Server URL") },
        text = {
            Column {
                Text(
                    "Change the relay server URL. The relay routes payment verification requests from websites to your app.",
                    style = MomoTypography.BodySmall,
                    color = MomoColors.TextSecondary
                )
                Spacer(Modifier.height(MomoSpacing.Lg))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Relay URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(url.trimEnd('/')) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ResetConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Everything?") },
        text = {
            Text("This will remove your relay connection and all sender configurations. You'll go back to the setup screen.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Reset", color = MomoColors.StatusFailed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
