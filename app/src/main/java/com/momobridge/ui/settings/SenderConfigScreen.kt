package com.momobridge.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momobridge.ui.components.GoldButton
import com.momobridge.ui.components.MomoTextField
import com.momobridge.ui.components.StepIndicator
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoShapes
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoTypography
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenderConfigScreen(
    senderAddress: String,
    label: String,
    prefilledSmsBody: String = "",
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: SenderConfigViewModel = hiltViewModel()
) {
    viewModel.initForSender(senderAddress, label, prefilledSmsBody)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.saved) {
        onSaved()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.label, style = MomoTypography.TitleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MomoColors.GroundDark
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MomoColors.TextPrimary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = MomoSpacing.Lg)
        ) {
            StepIndicator(
                currentStep = if (state.step == ConfigStep.PICK_MESSAGE) 0 else 1,
                totalSteps = 2
            )
            Spacer(modifier = Modifier.height(MomoSpacing.Lg))

            when (state.step) {
                ConfigStep.PICK_MESSAGE -> {
                    // ── Step 0 content (inline in ColumnScope for weight) ──
                    Text(
                        "Pick a sample message",
                        style = MomoTypography.BodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MomoColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Choose a recent payment SMS from ${state.label} to use as a template for detection.",
                        style = MomoTypography.BodySmall,
                        color = MomoColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(MomoSpacing.Lg))

                    if (state.messages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No messages found from this sender in your inbox.",
                                style = MomoTypography.BodyMedium,
                                color = MomoColors.TextTertiary
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = MomoSpacing.Lg),
                            verticalArrangement = Arrangement.spacedBy(MomoSpacing.Sm),
                            contentPadding = PaddingValues(bottom = MomoSpacing.Lg)
                        ) {
                            items(state.messages, key = { it.timestamp }) { msg ->
                                MessageCard(
                                    msg = msg,
                                    onClick = { viewModel.selectMessage(msg) }
                                )
                            }
                        }
                    }
                }

                ConfigStep.CONFIRM_FIELDS -> {
                    ConfirmFieldsStep(state, viewModel)
                }
            }
        }
    }
}

// ── Message Card (shared) ─────────────────────────────────────────────

@Composable
private fun MessageCard(msg: InboxMessage, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MomoShapes.CardShape,
        colors = CardDefaults.cardColors(containerColor = MomoColors.GroundLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MomoSpacing.CardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (msg.amount != null) {
                        Text(
                            text = formatAmount(msg.amount),
                            style = MomoTypography.BodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MomoColors.Gold
                        )
                        Spacer(modifier = Modifier.width(MomoSpacing.Sm))
                    }
                    if (msg.reference != null) {
                        Text(
                            text = "Ref: ${msg.reference}",
                            style = MomoTypography.LabelSmall,
                            color = MomoColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = msg.preview,
                    style = MomoTypography.BodySmall,
                    color = MomoColors.TextTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = msg.displayTime,
                    style = MomoTypography.LabelSmall,
                    color = MomoColors.TextTertiary
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Select",
                tint = MomoColors.Gold.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatAmount(amount: Double): String {
    val fmt = NumberFormat.getCurrencyInstance(Locale("en", "GH"))
    fmt.maximumFractionDigits = 2
    return fmt.format(amount)
}

// ── Step 1 — Confirm Fields ──────────────────────────────────────────

@Composable
private fun ConfirmFieldsStep(state: SenderConfigUiState, viewModel: SenderConfigViewModel) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Back arrow to return to message picker
        Row(
            modifier = Modifier
                .clickable(onClick = viewModel::goBackToPickMessage),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to messages",
                tint = MomoColors.Gold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(MomoSpacing.Xs))
            Text(
                "Change message",
                style = MomoTypography.LabelSmall,
                color = MomoColors.Gold
            )
        }
        Spacer(modifier = Modifier.height(MomoSpacing.Md))

        Text(
            "Review detected fields",
            style = MomoTypography.BodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MomoColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "The app automatically detected these fields. Edit if needed.",
            style = MomoTypography.BodySmall,
            color = MomoColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(MomoSpacing.Lg))

        // Selected message preview
        state.selectedMessage?.let { msg ->
            Card(
                shape = MomoShapes.BadgeShape,
                colors = CardDefaults.cardColors(
                    containerColor = MomoColors.GroundLight
                )
            ) {
                Column(modifier = Modifier.padding(MomoSpacing.Md)) {
                    Text(
                        "Selected message",
                        style = MomoTypography.LabelSmall,
                        color = MomoColors.TextTertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        msg.preview,
                        style = MomoTypography.BodySmall,
                        color = MomoColors.TextPrimary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(MomoSpacing.Lg))
        }

        // Reference field
        FieldEditor(
            label = "Reference",
            value = state.detectedRef,
            manualValue = state.manualRef,
            editing = state.editingRef,
            onEdit = viewModel::toggleEditRef,
            onUpdate = viewModel::updateManualRef,
            isRequired = true
        )
        Spacer(modifier = Modifier.height(MomoSpacing.Sm))

        // Amount field
        FieldEditor(
            label = "Amount",
            value = state.detectedAmount,
            manualValue = state.manualAmount,
            editing = state.editingAmount,
            onEdit = viewModel::toggleEditAmount,
            onUpdate = viewModel::updateManualAmount,
            isRequired = true
        )
        Spacer(modifier = Modifier.height(MomoSpacing.Sm))

        // Optional fields (collapsible)
        var showOptional by remember { mutableStateOf(false) }
        Text(
            text = if (showOptional) "Hide optional fields \u25B2" else "More fields \u25BC",
            color = MomoColors.Gold,
            style = MomoTypography.LabelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { showOptional = !showOptional }
        )
        if (showOptional) {
            Spacer(modifier = Modifier.height(MomoSpacing.Sm))
            FieldEditor(
                label = "Sender Name",
                value = state.detectedSenderName,
                manualValue = state.manualSenderName,
                editing = state.editingSenderName,
                onEdit = viewModel::toggleEditSenderName,
                onUpdate = viewModel::updateManualSenderName,
                isRequired = false
            )
            Spacer(modifier = Modifier.height(MomoSpacing.Sm))
            FieldEditor(
                label = "Sender Phone",
                value = state.detectedSenderPhone,
                manualValue = state.manualSenderPhone,
                editing = state.editingSenderPhone,
                onEdit = viewModel::toggleEditSenderPhone,
                onUpdate = viewModel::updateManualSenderPhone,
                isRequired = false
            )
            Spacer(modifier = Modifier.height(MomoSpacing.Sm))
            FieldEditor(
                label = "Balance After",
                value = state.detectedBalance,
                manualValue = state.manualBalance,
                editing = state.editingBalance,
                onEdit = viewModel::toggleEditBalance,
                onUpdate = viewModel::updateManualBalance,
                isRequired = false
            )
            Spacer(modifier = Modifier.height(MomoSpacing.Sm))

            // Keyword selector
            Text(
                "Credit keyword",
                style = MomoTypography.LabelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MomoColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(MomoSpacing.Xs))
            Row(horizontalArrangement = Arrangement.spacedBy(MomoSpacing.Sm)) {
                listOf("received", "credited", "paid", "transferred").forEach { kw ->
                    val selected = state.detectedKeyword == kw
                    FilledTonalButton(
                        onClick = { viewModel.updateKeyword(kw) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (selected) MomoColors.Gold.copy(alpha = 0.2f)
                            else MomoColors.GroundLight,
                            contentColor = if (selected) MomoColors.Gold else MomoColors.TextSecondary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Text(
                            kw,
                            style = MomoTypography.LabelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        if (state.error != null) {
            Spacer(modifier = Modifier.height(MomoSpacing.Md))
            Text(
                state.error!!,
                color = MomoColors.StatusFailed,
                style = MomoTypography.BodySmall
            )
        }

        Spacer(modifier = Modifier.height(MomoSpacing.Xl))

        GoldButton(
            text = if (state.isSaving) "Saving\u2026" else "Save Rule",
            onClick = viewModel::saveRule,
            fullWidth = true,
            enabled = !state.isSaving
        )

        Spacer(modifier = Modifier.height(MomoSpacing.Lg))
    }
}

@Composable
private fun FieldEditor(
    label: String,
    value: String,
    manualValue: String,
    editing: Boolean,
    onEdit: () -> Unit,
    onUpdate: (String) -> Unit,
    isRequired: Boolean
) {
    val isEmpty = value.isBlank() && manualValue.isBlank()

    Card(
        shape = MomoShapes.BadgeShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isEmpty && isRequired)
                MomoColors.StatusFailed.copy(alpha = 0.08f)
            else
                MomoColors.GroundLight
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MomoSpacing.Md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label,
                        style = MomoTypography.LabelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isEmpty && isRequired)
                            MomoColors.StatusFailed
                        else
                            MomoColors.TextSecondary
                    )
                    if (isRequired) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "*",
                            color = MomoColors.Gold,
                            style = MomoTypography.LabelSmall
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (editing) {
                    MomoTextField(
                        value = manualValue,
                        onValueChange = onUpdate,
                        label = label,
                        placeholder = if (value.isNotBlank()) "Auto: $value" else "Enter $label"
                    )
                } else {
                    Text(
                        text = when {
                            value.isNotBlank() -> value
                            !isRequired -> "\u2014"
                            else -> "Not detected"
                        },
                        style = MomoTypography.BodyMedium,
                        color = when {
                            value.isNotBlank() -> MomoColors.TextPrimary
                            isRequired -> MomoColors.StatusFailed
                            else -> MomoColors.TextTertiary
                        }
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
