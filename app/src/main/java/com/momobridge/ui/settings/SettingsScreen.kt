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
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momobridge.service.RelayConnectionStatus
import com.momobridge.service.RelayState
import com.momobridge.ui.components.DangerButton
import com.momobridge.ui.components.SectionHeader
import com.momobridge.ui.components.SectionSubtext
import com.momobridge.ui.components.SmsSourceCard
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoTypography

/**
 * Settings screen — flat layout.
 *
 * Structural rules that keep this screen from breaking again:
 *  1. Every section is a plain Column on the screen background. No section
 *     is wrapped in a Card, and no single composable is dropped into a bare
 *     Box. A Box with no contentAlignment and mismatched child sizing was
 *     the root cause of components rendering on top of each other in the
 *     previous version.
 *  2. Rows that mix a flexible Text with a fixed-size icon always put
 *     `Modifier.weight(1f)` on the Text *before* adding the icon, so the
 *     icon's width is reserved first and the text wraps/ellipsizes inside
 *     whatever is left — not the other way around.
 *  3. Each section is separated by a HorizontalDivider, not nested
 *     elevation. There is exactly one visual "layer" on this screen:
 *     the scaffold background. Inputs and inline cards (like the relay
 *     status row) are the only things that get a surface tint, and they
 *     never contain another Card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToHelp: () -> Unit,
    onNavigateToSenderConfig: (String, String) -> Unit,
    onReconfigure: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val relayState by viewModel.connectionState.collectAsStateWithLifecycle()

    var showReconfigureDialog by remember { mutableStateOf(false) }
    var showRelayUrlDialog by remember { mutableStateOf(false) }

    if (showReconfigureDialog) {
        ResetConfirmDialog(
            onConfirm = {
                viewModel.reconfigure()
                showReconfigureDialog = false
                onReconfigure()
            },
            onDismiss = { showReconfigureDialog = false }
        )
    }

    if (showRelayUrlDialog) {
        RelayUrlEditDialog(
            currentUrl = viewModel.relayUrl,
            onSave = { newUrl ->
                viewModel.updateRelayUrl(newUrl)
                showRelayUrlDialog = false
            },
            onDismiss = { showRelayUrlDialog = false }
        )
    }

    if (state.showScanResults) {
        ScanResultDialog(
            senders = state.scannedSenders,
            loading = state.scanningInbox,
            onAdd = { address -> viewModel.addSourceFromScan(address) },
            onDismiss = viewModel::hideScanResults
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MomoColors.GroundDark)
    ) {
        TopAppBar(
            title = { Text("Settings", style = MomoTypography.TitleLarge, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MomoColors.GroundDark),
            actions = {
                TextButton(onClick = onNavigateToHelp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null, tint = MomoColors.Gold)
                        Spacer(Modifier.width(MomoSpacing.Xs))
                        Text("Help", color = MomoColors.Gold, style = MomoTypography.LabelSmall)
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MomoSpacing.Lg)
        ) {
            Spacer(Modifier.height(MomoSpacing.Sm))

            SettingsSection(title = "Connection") {
                RelayStatusRow(
                    relayState = relayState,
                    onReconnect = viewModel::reconnect,
                    onEditUrl = { showRelayUrlDialog = true }
                )
            }

            SettingsLabeledDivider(label = "Connection")

            SettingsSection(title = "Transaction Expiry") {
                SectionSubtext(text = "Set how long a transaction remains valid. Disable for unlimited validity.")
                Spacer(Modifier.height(MomoSpacing.Md))
                ExpiryConfigRow(
                    expiryEnabled = state.expiryEnabled,
                    expiryHours = state.expiryHours,
                    onToggle = viewModel::setExpiryEnabled,
                    onHoursChange = viewModel::setExpiryHours
                )
            }

            SettingsLabeledDivider(label = "Transaction Expiry")

            SettingsSection(title = "Monitored Senders") {
                SectionSubtext(text = "Add SMS senders (e.g. MobileMoney, T-CASH) and configure how to parse their messages.")
                Spacer(Modifier.height(MomoSpacing.Md))

                if (state.smsSources.isEmpty()) {
                    EmptySendersNotice()
                    Spacer(Modifier.height(MomoSpacing.Md))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(MomoSpacing.Sm)) {
                        state.smsSources.forEach { source ->
                            SmsSourceCard(
                                source = source,
                                onToggle = { viewModel.toggleSource(source.id, it) },
                                onConfigure = { onNavigateToSenderConfig(source.senderAddress, source.label) },
                                onDelete = { viewModel.removeSource(source.id) }
                            )
                        }
                    }
                    Spacer(Modifier.height(MomoSpacing.Md))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MomoSpacing.Sm)
                ) {
                    OutlinedButton(
                        onClick = viewModel::scanInbox,
                        modifier = Modifier.weight(1f),
                        enabled = !state.scanningInbox
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
                        Spacer(Modifier.width(MomoSpacing.Sm))
                        Text("Scan Inbox", style = MomoTypography.LabelSmall)
                    }
                    AddSenderButton(
                        modifier = Modifier.weight(1f),
                        onAdd = { addr, label -> viewModel.addSourceManually(addr, label) }
                    )
                }
            }

            SettingsLabeledDivider(label = "Monitored Senders")

            SettingsSection(title = "Past Transactions") {
                SectionSubtext(text = "Scan your inbox for past money received messages from monitored senders (up to 2 months back).")
                Spacer(Modifier.height(MomoSpacing.Md))

                Button(
                    onClick = viewModel::scanHistoricalTransactions,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.scanningHistorical,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MomoColors.Gold,
                        contentColor = MomoColors.OnGold
                    )
                ) {
                    if (state.scanningHistorical) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MomoColors.OnGold
                        )
                        Spacer(Modifier.width(MomoSpacing.Sm))
                        Text("Scanning…")
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(MomoSpacing.Sm))
                        Text("Scan Inbox for Past Transactions")
                    }
                }

                if (state.historicalScanResult != null) {
                    Spacer(Modifier.height(MomoSpacing.Sm))
                    Text(
                        text = state.historicalScanResult!!,
                        style = MomoTypography.BodySmall,
                        color = MomoColors.TextSecondary
                    )
                }
            }

            SettingsLabeledDivider(label = "Past Transactions")

            Spacer(Modifier.height(MomoSpacing.Sm))
            DangerButton(
                text = "Reset All Settings",
                onClick = { showReconfigureDialog = true }
            )

            Spacer(Modifier.height(MomoSpacing.Xxl))
        }
    }
}

// ───────────────────────── Section scaffolding ─────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = title)
        Spacer(Modifier.height(MomoSpacing.Md))
        content()
    }
}

@Composable
private fun SettingsLabeledDivider(label: String) {
    Spacer(Modifier.height(MomoSpacing.Md))
    HorizontalDivider(color = MomoColors.BorderSubtle, thickness = 1.dp)
    Spacer(Modifier.height(MomoSpacing.Xs))
    Text(
        text = label,
        style = MomoTypography.LabelSmall,
        color = MomoColors.TextTertiary,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(MomoSpacing.Md))
}

// ───────────────────────── Connection ─────────────────────────

@Composable
private fun RelayStatusRow(
    relayState: RelayState,
    onReconnect: () -> Unit,
    onEditUrl: () -> Unit
) {
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(Modifier.width(MomoSpacing.Sm))
            Text(
                text = statusLabel,
                style = MomoTypography.BodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = statusColor,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onReconnect, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Reconnect", tint = MomoColors.Gold)
            }
        }

        if (relayState.url != null) {
            Spacer(Modifier.height(MomoSpacing.Sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditUrl() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = relayState.url,
                    style = MomoTypography.BodySmall,
                    color = MomoColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(MomoSpacing.Sm))
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit relay URL",
                    tint = MomoColors.Gold,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ───────────────────────── Transaction Expiry ─────────────────────────

@Composable
private fun ExpiryConfigRow(
    expiryEnabled: Boolean,
    expiryHours: Long,
    onToggle: (Boolean) -> Unit,
    onHoursChange: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enable Expiry",
                    style = MomoTypography.BodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MomoColors.TextPrimary
                )
                Text(
                    text = if (expiryEnabled) "Transactions expire after ${expiryHours}h" else "Transactions never expire",
                    style = MomoTypography.LabelSmall,
                    color = MomoColors.TextSecondary
                )
            }
            Spacer(Modifier.width(MomoSpacing.Sm))
            Switch(
                checked = expiryEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MomoColors.Gold,
                    checkedThumbColor = MomoColors.OnGold
                )
            )
        }

        if (expiryEnabled) {
            Spacer(Modifier.height(MomoSpacing.Md))
            var hoursText by remember(expiryHours) { mutableStateOf(expiryHours.toString()) }
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
    }
}

// ───────────────────────── Monitored Senders ─────────────────────────

@Composable
private fun EmptySendersNotice() {
    Text(
        text = "No senders configured yet. Tap \"Scan Inbox\" or \"Add Manually\" to get started.",
        style = MomoTypography.BodySmall,
        color = MomoColors.TextSecondary
    )
}

@Composable
private fun AddSenderButton(modifier: Modifier = Modifier, onAdd: (String, String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var addr by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }

    OutlinedButton(onClick = { showDialog = true }, modifier = modifier) {
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
                CircularProgressIndicator(color = MomoColors.Gold)
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

// ───────────────────────── Dialogs ─────────────────────────

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