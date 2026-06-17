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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.momobridge.ui.components.GoldButton
import com.momobridge.ui.components.GoldOutlineButton
import com.momobridge.ui.components.StepIndicator
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoShapes
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoTypography

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
                title = { Text("Configure ${state.label}", style = MomoTypography.TitleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MomoColors.GroundMedium
                ),
                navigationIcon = {
                    Text(
                        text = "Back",
                        color = MomoColors.Gold,
                        style = MomoTypography.LabelSmall,
                        modifier = Modifier
                            .clickable { onBack() }
                            .padding(start = MomoSpacing.Lg)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(MomoSpacing.ScreenPadding)
        ) {
            StepIndicator(
                currentStep = when (state.step) {
                    ConfigStep.PICK_MESSAGE -> 0
                    ConfigStep.CONFIRM_FIELDS -> 1
                    ConfigStep.VALIDATE -> 2
                    ConfigStep.DONE -> 3
                },
                totalSteps = 4
            )
            Spacer(modifier = Modifier.height(MomoSpacing.Lg))

            when (state.step) {
                ConfigStep.PICK_MESSAGE -> {
                    PickMessageStep(state, viewModel)
                }

                ConfigStep.CONFIRM_FIELDS -> {
                    ConfirmFieldsStep(state, viewModel)
                }

                ConfigStep.VALIDATE -> {
                    ValidateStep(state, viewModel)
                }

                ConfigStep.DONE -> {
                    DoneStep(onSaved)
                }
            }
        }
    }
}

@Composable
private fun PickMessageStep(state: SenderConfigUiState, viewModel: SenderConfigViewModel) {
    Text(
        "Pick a sample SMS to use as template",
        style = MomoTypography.BodyMedium,
        color = MomoColors.TextSecondary
    )
    Spacer(Modifier.height(MomoSpacing.Md))
    if (state.messages.isEmpty()) {
        Text(
            "No messages found from this sender in the inbox.",
            style = MomoTypography.BodyMedium,
            color = MomoColors.StatusFailed
        )
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(MomoSpacing.Sm)) {
        items(state.messages) { msg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectMessage(msg) },
                shape = MomoShapes.CardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MomoColors.GroundMedium
                )
            ) {
                Column(Modifier.padding(MomoSpacing.CardPadding)) {
                    Text(
                        msg.body,
                        maxLines = 3,
                        style = MomoTypography.BodySmall,
                        color = MomoColors.TextPrimary
                    )
                    Spacer(Modifier.height(MomoSpacing.Xs))
                    Text(
                        msg.displayTime,
                        style = MomoTypography.LabelSmall,
                        color = MomoColors.TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmFieldsStep(state: SenderConfigUiState, viewModel: SenderConfigViewModel) {
    FieldConfirmCard(
        label = "Reference",
        value = state.detectedRef,
        manualValue = state.manualRef,
        editing = state.editingRef,
        onEdit = viewModel::toggleEditRef,
        onUpdate = viewModel::updateManualRef
    )
    Spacer(Modifier.height(MomoSpacing.Sm))
    FieldConfirmCard(
        label = "Amount",
        value = state.detectedAmount,
        manualValue = state.manualAmount,
        editing = state.editingAmount,
        onEdit = viewModel::toggleEditAmount,
        onUpdate = viewModel::updateManualAmount
    )
    Spacer(Modifier.height(MomoSpacing.Sm))
    FieldConfirmCard(
        label = "Sender Name",
        value = state.detectedSenderName,
        manualValue = state.manualSenderName,
        editing = state.editingSenderName,
        onEdit = viewModel::toggleEditSenderName,
        onUpdate = viewModel::updateManualSenderName,
        optional = true
    )
    Spacer(Modifier.height(MomoSpacing.Sm))
    FieldConfirmCard(
        label = "Sender Phone",
        value = state.detectedSenderPhone,
        manualValue = state.manualSenderPhone,
        editing = state.editingSenderPhone,
        onEdit = viewModel::toggleEditSenderPhone,
        onUpdate = viewModel::updateManualSenderPhone,
        optional = true
    )
    Spacer(Modifier.height(MomoSpacing.Sm))
    FieldConfirmCard(
        label = "Balance After",
        value = state.detectedBalance,
        manualValue = state.manualBalance,
        editing = state.editingBalance,
        onEdit = viewModel::toggleEditBalance,
        onUpdate = viewModel::updateManualBalance,
        optional = true
    )
    Spacer(Modifier.height(MomoSpacing.Sm))
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
        Spacer(Modifier.height(MomoSpacing.Sm))
        Text(
            state.error!!,
            color = MomoColors.StatusFailed,
            style = MomoTypography.BodySmall
        )
    }
    Spacer(Modifier.height(MomoSpacing.Lg))
    GoldButton(
        text = "Next — Validate Rule",
        onClick = viewModel::goToValidation,
        fullWidth = true
    )
}

@Composable
private fun ValidateStep(state: SenderConfigUiState, viewModel: SenderConfigViewModel) {
    Text(
        "Test with another message from same sender",
        style = MomoTypography.BodyMedium,
        color = MomoColors.TextSecondary
    )
    Spacer(Modifier.height(MomoSpacing.Sm))
    Text(
        "Pick another message to verify your rule extracts fields correctly.",
        style = MomoTypography.BodySmall,
        color = MomoColors.TextTertiary
    )
    Spacer(Modifier.height(MomoSpacing.Md))

    if (state.validationResult != null && state.selectedValidationMessage != null) {
        val result = state.validationResult
        if (result.succeeded > 0) {
            Card(
                shape = MomoShapes.CardShape,
                colors = CardDefaults.cardColors(containerColor = MomoColors.GroundLight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MomoSpacing.CardPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Pass",
                        tint = MomoColors.StatusConfirmed
                    )
                    Spacer(Modifier.width(MomoSpacing.Sm))
                    Column {
                        Text(
                            "Validation passed!",
                            style = MomoTypography.BodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MomoColors.StatusConfirmed
                        )
                        Text(
                            "The rule correctly extracted fields from this message.",
                            style = MomoTypography.BodySmall,
                            color = MomoColors.TextSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(MomoSpacing.Lg))
            GoldButton(
                text = "Save Configuration",
                onClick = viewModel::saveAfterValidation,
                fullWidth = true
            )
        } else {
            Card(
                shape = MomoShapes.CardShape,
                colors = CardDefaults.cardColors(containerColor = MomoColors.GroundLight)
            ) {
                Column(modifier = Modifier.padding(MomoSpacing.CardPadding)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fail",
                            tint = MomoColors.StatusFailed
                        )
                        Spacer(Modifier.width(MomoSpacing.Sm))
                        Text(
                            "Validation failed",
                            style = MomoTypography.BodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MomoColors.StatusFailed
                        )
                    }
                    Spacer(Modifier.height(MomoSpacing.Sm))
                    result.failures.forEach { failure ->
                        Text(
                            failure.reason,
                            style = MomoTypography.BodySmall,
                            color = MomoColors.TextSecondary
                        )
                        Spacer(Modifier.height(MomoSpacing.Xs))
                        Text(
                            "Message: ${failure.messageBody}",
                            style = MomoTypography.LabelSmall,
                            color = MomoColors.TextTertiary,
                            maxLines = 2
                        )
                    }
                }
            }
            Spacer(Modifier.height(MomoSpacing.Lg))
            GoldOutlineButton(
                text = "Go Back to Edit",
                onClick = viewModel::goBackToEdit,
                fullWidth = true
            )
        }
    } else {
        if (state.validationMessages.isEmpty()) {
            Text(
                "No other messages available to validate against.",
                style = MomoTypography.BodyMedium,
                color = MomoColors.TextSecondary
            )
            Spacer(Modifier.height(MomoSpacing.Md))
            GoldOutlineButton(
                text = "Skip & Save Anyway",
                onClick = viewModel::skipValidationAndSave,
                fullWidth = true
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(MomoSpacing.Sm)) {
                items(state.validationMessages) { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectValidationMessage(msg) },
                        shape = MomoShapes.CardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MomoColors.GroundMedium
                        )
                    ) {
                        Column(Modifier.padding(MomoSpacing.CardPadding)) {
                            Text(
                                msg.body,
                                maxLines = 2,
                                style = MomoTypography.BodySmall,
                                color = MomoColors.TextPrimary
                            )
                            Spacer(Modifier.height(MomoSpacing.Xs))
                            Text(
                                msg.displayTime,
                                style = MomoTypography.LabelSmall,
                                color = MomoColors.TextTertiary
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(MomoSpacing.Md))
            GoldOutlineButton(
                text = "Skip Validation & Save",
                onClick = viewModel::skipValidationAndSave,
                fullWidth = true
            )
        }
    }
}

@Composable
private fun DoneStep(onSaved: () -> Unit) {
    Text(
        "Configuration saved!",
        style = MomoTypography.TitleMedium,
        fontWeight = FontWeight.Bold,
        color = MomoColors.StatusConfirmed
    )
    Spacer(Modifier.height(MomoSpacing.Sm))
    Text(
        "This sender is now being monitored with your custom parsing rules.",
        style = MomoTypography.BodyMedium,
        color = MomoColors.TextSecondary
    )
    Spacer(Modifier.height(MomoSpacing.Lg))
    GoldButton(
        text = "Done",
        onClick = onSaved,
        fullWidth = true
    )
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
        shape = MomoShapes.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isEmpty && !optional)
                MomoColors.GroundLight
            else
                MomoColors.GroundMedium
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(MomoSpacing.CardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MomoTypography.LabelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEmpty && !optional)
                        MomoColors.StatusFailed
                    else
                        MomoColors.TextSecondary
                )
                if (editing) {
                    OutlinedTextField(
                        value = manualValue,
                        onValueChange = onUpdate,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MomoShapes.InputShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MomoColors.BorderAccent,
                            unfocusedBorderColor = MomoColors.BorderSubtle,
                            cursorColor = MomoColors.Gold,
                            focusedTextColor = MomoColors.TextPrimary,
                            unfocusedTextColor = MomoColors.TextPrimary,
                            focusedContainerColor = MomoColors.GroundLight,
                            unfocusedContainerColor = MomoColors.GroundLight
                        ),
                        placeholder = { Text(if (value.isNotBlank()) "Auto: $value" else "Enter $label", style = MomoTypography.BodyMedium, color = MomoColors.TextTertiary) }
                    )
                } else {
                    Text(
                        if (value.isNotBlank()) value else if (optional) "(skip)" else "Not detected",
                        style = MomoTypography.BodyMedium,
                        color = MomoColors.TextPrimary
                    )
                }
            }
            if (!editing) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit $label",
                        tint = MomoColors.Gold
                    )
                }
            }
        }
    }
}
