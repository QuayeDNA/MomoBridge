package com.momobridge.ui.apikeys

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momobridge.ui.components.DangerButton
import com.momobridge.ui.components.GoldButton
import com.momobridge.ui.components.GoldOutlineButton
import com.momobridge.ui.components.MomoTextField
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeysScreen(
    viewModel: ApiKeysViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    var showRevoked by remember { mutableStateOf(false) }

    val activeKeys = state.keys.filter { it.entity.isActive }
    val revokedKeys = state.keys.filter { !it.entity.isActive }

    // Detail bottom sheet
    if (state.showDetailSheet && state.selectedKey != null) {
        val key = state.selectedKey!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = viewModel::hideDetail,
            sheetState = sheetState,
            containerColor = MomoColors.GroundMedium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MomoSpacing.Lg)
            ) {
                // Status badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (key.entity.isActive) MomoColors.StatusConfirmed
                                else MomoColors.StatusFailed,
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(MomoSpacing.Sm))
                    Text(
                        text = if (key.entity.isActive) "Active" else "Revoked",
                        style = MomoType.LabelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (key.entity.isActive) MomoColors.StatusConfirmed else MomoColors.StatusFailed
                    )
                }

                Spacer(Modifier.height(MomoSpacing.Lg))

                // Label (editable)
                if (state.editingLabel) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = state.editLabelText,
                            onValueChange = viewModel::updateEditLabelText,
                            label = { Text("Label") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(MomoSpacing.Sm))
                        TextButton(onClick = viewModel::saveLabel) {
                            Text("Save", color = MomoColors.Gold)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = key.entity.label,
                            style = MomoType.TitleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MomoColors.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = viewModel::startEditingLabel) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit label",
                                tint = MomoColors.Gold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(MomoSpacing.Lg))

                // Full key
                Card(
                    colors = CardDefaults.cardColors(containerColor = MomoColors.GroundLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MomoSpacing.CardPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = key.keyValue,
                            style = MomoType.BodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MomoColors.TextPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(MomoSpacing.Sm))
                        IconButton(onClick = { clipboard.setText(AnnotatedString(key.keyValue)) }) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = MomoColors.Gold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(MomoSpacing.Lg))

                // Dates
                val dateFormat = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }
                DetailRow("Created", dateFormat.format(Date(key.entity.createdAt)))
                if (key.entity.lastUsedAt != null) {
                    Spacer(Modifier.height(MomoSpacing.Sm))
                    DetailRow("Last used", dateFormat.format(Date(key.entity.lastUsedAt!!)))
                }

                Spacer(Modifier.height(MomoSpacing.Xxl))

                // Actions
                if (key.entity.isActive) {
                    DangerButton(
                        text = "Revoke Key",
                        onClick = viewModel::revokeKey
                    )
                } else {
                    GoldOutlineButton(
                        text = "Reactivate Key",
                        onClick = viewModel::reactivateKey
                    )
                    Spacer(Modifier.height(MomoSpacing.Sm))
                    DangerButton(
                        text = "Delete Permanently",
                        onClick = viewModel::deleteKey
                    )
                }

                Spacer(Modifier.height(MomoSpacing.Xxl))
            }
        }
    }

    // Add key dialog
    if (state.showAddDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideAddDialog,
            title = { Text("Add API Key") },
            text = {
                Column {
                    Text(
                        "Name this store or integration.",
                        style = MomoType.BodySmall,
                        color = MomoColors.TextSecondary
                    )
                    Spacer(Modifier.height(MomoSpacing.Lg))
                    MomoTextField(
                        value = state.newKeyLabel,
                        onValueChange = viewModel::updateNewKeyLabel,
                        label = "Store Label",
                        placeholder = "e.g. Accra Branch"
                    )
                    if (state.error != null) {
                        Spacer(Modifier.height(MomoSpacing.Sm))
                        Text(
                            state.error!!,
                            color = MomoColors.StatusFailed,
                            style = MomoType.BodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::createKey) {
                    Text("Generate", color = MomoColors.Gold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideAddDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("API Keys", style = MomoType.TitleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(MomoSpacing.Sm))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MomoColors.Highlight),
                            shape = CircleShape
                        ) {
                            Text(
                                text = state.activeCount.toString(),
                                style = MomoType.LabelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MomoColors.Gold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MomoColors.GroundMedium
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showAddDialog,
                containerColor = MomoColors.Gold,
                contentColor = MomoColors.OnGold
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add API Key")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = MomoSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(MomoSpacing.Sm)
        ) {
            // Active keys
            item {
                Spacer(Modifier.height(MomoSpacing.Sm))
                Text(
                    text = "Active",
                    style = MomoType.LabelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MomoColors.TextSecondary
                )
                Spacer(Modifier.height(MomoSpacing.Sm))
            }

            if (activeKeys.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MomoColors.GroundLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No API keys yet. Tap + to add one.",
                            modifier = Modifier.padding(MomoSpacing.CardPadding),
                            style = MomoType.BodySmall,
                            color = MomoColors.TextTertiary
                        )
                    }
                }
            }

            items(activeKeys, key = { it.entity.id }) { key ->
                KeyCard(
                    key = key,
                    isActive = true,
                    onClick = { viewModel.showDetail(key) }
                )
            }

            // Revoked keys (collapsible)
            if (revokedKeys.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(MomoSpacing.Lg))
                    HorizontalDivider(color = MomoColors.BorderSubtle)
                    Spacer(Modifier.height(MomoSpacing.Sm))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRevoked = !showRevoked },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Revoked (${revokedKeys.size})",
                            style = MomoType.LabelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MomoColors.TextTertiary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (showRevoked) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (showRevoked) "Collapse" else "Expand",
                            tint = MomoColors.TextTertiary
                        )
                    }
                    Spacer(Modifier.height(MomoSpacing.Sm))
                }

                items(revokedKeys, key = { it.entity.id }) { key ->
                    AnimatedVisibility(visible = showRevoked) {
                        KeyCard(
                            key = key,
                            isActive = false,
                            onClick = { viewModel.showDetail(key) }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(MomoSpacing.Xxl))
            }
        }
    }
}

@Composable
private fun KeyCard(
    key: ApiKeyWithValue,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MomoColors.GroundMedium else MomoColors.GroundMedium.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MomoSpacing.CardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status dot + icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isActive) MomoColors.Highlight else MomoColors.GroundLight,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = if (isActive) MomoColors.Gold else MomoColors.TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(Modifier.width(MomoSpacing.Md))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = key.entity.label,
                        style = MomoType.BodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) MomoColors.TextPrimary else MomoColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!isActive) {
                        Spacer(Modifier.width(MomoSpacing.Sm))
                        Text(
                            text = "Revoked",
                            style = MomoType.LabelSmall,
                            color = MomoColors.StatusFailed
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${key.keyValue.take(16)}...",
                    style = MomoType.LabelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MomoColors.TextTertiary
                )
                if (key.entity.lastUsedAt != null) {
                    Spacer(Modifier.height(2.dp))
                    val dateFormat = remember {
                        SimpleDateFormat("d MMM", Locale.getDefault())
                    }
                    Text(
                        text = "Last used ${dateFormat.format(Date(key.entity.lastUsedAt!!))}",
                        style = MomoType.LabelSmall,
                        color = MomoColors.TextTertiary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MomoType.LabelSmall,
            color = MomoColors.TextTertiary,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MomoType.BodySmall,
            color = MomoColors.TextPrimary
        )
    }
}
