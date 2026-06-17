package com.momobridge.ui.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenderConfigScreen(
    senderAddress: String,
    label: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: SenderConfigViewModel = hiltViewModel()
) {
    viewModel.initForSender(senderAddress, label)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.saved) {
        onSaved()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure ${state.label}") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    Text(
                        text = "Back",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onBack() }
                            .padding(start = 16.dp)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (state.step) {
                ConfigStep.PICK_MESSAGE -> {
                    Text(
                        "Pick a sample SMS to use as template",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(12.dp))
                    if (state.messages.isEmpty()) {
                        Text(
                            "No messages found from this sender in the inbox.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.messages) { msg ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectMessage(msg) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        msg.body,
                                        maxLines = 3,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        msg.displayTime,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                ConfigStep.CONFIRM_FIELDS -> {
                    FieldConfirmCard(
                        label = "Reference",
                        value = state.detectedRef,
                        manualValue = state.manualRef,
                        editing = state.editingRef,
                        onEdit = viewModel::toggleEditRef,
                        onUpdate = viewModel::updateManualRef
                    )
                    Spacer(Modifier.height(8.dp))
                    FieldConfirmCard(
                        label = "Amount",
                        value = state.detectedAmount,
                        manualValue = state.manualAmount,
                        editing = state.editingAmount,
                        onEdit = viewModel::toggleEditAmount,
                        onUpdate = viewModel::updateManualAmount
                    )
                    Spacer(Modifier.height(8.dp))
                    FieldConfirmCard(
                        label = "Sender Name",
                        value = state.detectedSenderName,
                        manualValue = state.manualSenderName,
                        editing = state.editingSenderName,
                        onEdit = viewModel::toggleEditSenderName,
                        onUpdate = viewModel::updateManualSenderName,
                        optional = true
                    )
                    Spacer(Modifier.height(8.dp))
                    FieldConfirmCard(
                        label = "Sender Phone",
                        value = state.detectedSenderPhone,
                        manualValue = state.manualSenderPhone,
                        editing = state.editingSenderPhone,
                        onEdit = viewModel::toggleEditSenderPhone,
                        onUpdate = viewModel::updateManualSenderPhone,
                        optional = true
                    )
                    Spacer(Modifier.height(8.dp))
                    FieldConfirmCard(
                        label = "Balance After",
                        value = state.detectedBalance,
                        manualValue = state.manualBalance,
                        editing = state.editingBalance,
                        onEdit = viewModel::toggleEditBalance,
                        onUpdate = viewModel::updateManualBalance,
                        optional = true
                    )
                    Spacer(Modifier.height(8.dp))
                    FieldConfirmCard(
                        label = "Credit Keyword",
                        value = state.detectedKeyword,
                        manualValue = state.manualKeyword,
                        editing = state.editingKeyword,
                        onEdit = viewModel::toggleEditKeyword,
                        onUpdate = viewModel::updateManualKeyword,
                        optional = false
                    )
                    if (state.error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            state.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = viewModel::buildAndSave,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Configuration")
                    }
                }

                ConfigStep.DONE -> {
                    Text(
                        "Configuration saved!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "This sender is now being monitored with your custom parsing rules.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onSaved, modifier = Modifier.fillMaxWidth()) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldConfirmCard(
    label: String,
    value: String,
    manualValue: String,
    editing: Boolean,
    onEdit: () -> Unit,
    onUpdate: (String) -> Unit,
    optional: Boolean = false
) {
    val isEmpty = value.isBlank() && manualValue.isBlank()
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isEmpty && !optional)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEmpty && !optional)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                if (editing) {
                    OutlinedTextField(
                        value = manualValue,
                        onValueChange = onUpdate,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (value.isNotBlank()) "Auto: $value" else "Enter $label") }
                    )
                } else {
                    Text(
                        if (value.isNotBlank()) value else if (optional) "(skip)" else "Not detected",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (!editing) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit $label",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
