package com.momobridge.ui.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momobridge.service.RelayConnectionStatus
import com.momobridge.ui.components.GoldButton
import com.momobridge.ui.components.GoldOutlineButton
import com.momobridge.ui.components.MomoTextField
import com.momobridge.ui.components.StepIndicator
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoShapes
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoTypography
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val nameFocusRequester = remember { FocusRequester() }
    val storeLabelFocusRequester = remember { FocusRequester() }

    if (state.isComplete) {
        onComplete()
        return
    }

    LaunchedEffect(state.step) {
        focusManager.clearFocus()
        scrollState.animateScrollTo(0)
        delay(100)
        when (state.step) {
            0 -> nameFocusRequester.requestFocus()
            1 -> storeLabelFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(state.step) {
        if (state.step == 3) {
            viewModel.enterSendersStep()
        }
    }

    LaunchedEffect(state.newKeyLabel) {
        if (state.newKeyLabel.isEmpty() && state.step == 1) {
            delay(200)
            storeLabelFocusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MomoColors.GroundDark
                ),
                navigationIcon = {
                    if (state.step > 0) {
                        IconButton(onClick = { viewModel.goBack(); focusManager.clearFocus() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MomoColors.TextPrimary
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            StepIndicator(
                currentStep = state.step,
                totalSteps = 4,
                labels = listOf("Welcome", "Stores", "Relay", "Senders")
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = MomoSpacing.Lg)
            ) {
                when (state.step) {
                    0 -> WelcomeContent(state, nameFocusRequester, viewModel)
                    1 -> StoresContent(state, storeLabelFocusRequester, viewModel)
                    2 -> RelayContent(state, viewModel)
                    3 -> SendersContent(state, viewModel)
                }
            }

            Spacer(modifier = Modifier.height(MomoSpacing.Sm))

            SetupActionButtons(
                state = state,
                onPrimary = {
                    focusManager.clearFocus()
                    when (state.step) {
                        0, 1 -> viewModel.goToNextStep()
                        2 -> {
                            viewModel.connectToRelay()
                            viewModel.checkAndProceedFromRelay()
                        }
                        3 -> viewModel.goToNextStep()
                    }
                },
                onSecondary = {
                    focusManager.clearFocus()
                    when (state.step) {
                        1 -> viewModel.goBack()
                        2, 3 -> viewModel.goToNextStep()
                        else -> {}
                    }
                }
            )
        }
    }
}

// ── Shared step layout helpers ────────────────────────────────────────

@Composable
private fun StepHeadline(text: String) {
    Text(
        text = text,
        style = MomoTypography.TitleLarge,
        fontWeight = FontWeight.Bold,
        color = MomoColors.TextPrimary,
        modifier = Modifier.padding(bottom = MomoSpacing.Xs)
    )
}

@Composable
private fun StepBody(text: String) {
    Text(
        text = text,
        style = MomoTypography.BodySmall,
        color = MomoColors.TextSecondary,
        modifier = Modifier.padding(bottom = MomoSpacing.Xl)
    )
}

// ── Step 0 — Welcome / Profile ───────────────────────────────────────

@Composable
private fun WelcomeContent(
    state: SetupUiState,
    focusRequester: FocusRequester,
    viewModel: SetupViewModel
) {
    Spacer(modifier = Modifier.height(MomoSpacing.Xl))
    StepHeadline("Welcome to MoMo Bridge")
    StepBody("Your phone already gets the SMS. What should we call you?")

    MomoTextField(
        value = state.ownerName,
        onValueChange = viewModel::updateOwnerName,
        label = "Your Name",
        placeholder = "e.g. Dave",
        focusRequester = focusRequester,
        error = state.error,
        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
        onImeAction = viewModel::goToNextStep
    )
}

// ── Step 1 — Stores / API Keys ───────────────────────────────────────

@Composable
private fun StoresContent(
    state: SetupUiState,
    focusRequester: FocusRequester,
    viewModel: SetupViewModel
) {
    Spacer(modifier = Modifier.height(MomoSpacing.Xl))
    StepHeadline("Add Your First Store")
    StepBody("Each store gets its own API key. Websites use this key to send verification requests to your app.")

    MomoTextField(
        value = state.newKeyLabel,
        onValueChange = viewModel::updateNewKeyLabel,
        label = "Store Label",
        placeholder = "e.g. Main Shop",
        error = if (state.generatedKeys.isEmpty()) state.error else null,
        focusRequester = focusRequester,
        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
        onImeAction = { if (state.newKeyLabel.isNotBlank()) viewModel.generateKey() }
    )

    Spacer(modifier = Modifier.height(MomoSpacing.Sm))

    GoldButton(
        text = "Generate Key",
        onClick = {
            viewModel.generateKey()
        },
        enabled = state.newKeyLabel.isNotBlank()
    )

    if (state.generatedKeys.isNotEmpty()) {
        Spacer(modifier = Modifier.height(MomoSpacing.Xl))

        Card(
            colors = CardDefaults.cardColors(containerColor = MomoColors.GroundLight),
            shape = MomoShapes.CardShape
        ) {
            Column {
                state.generatedKeys.forEachIndexed { index, keyInfo ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MomoColors.BorderSubtle,
                            modifier = Modifier.padding(horizontal = MomoSpacing.Lg)
                        )
                    }
                    KeyRow(keyInfo = keyInfo)
                }
            }
        }

        Spacer(modifier = Modifier.height(MomoSpacing.Md))

        Text(
            text = "+ Add another store",
            color = MomoColors.Gold,
            style = MomoTypography.LabelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable {
                viewModel.updateNewKeyLabel("")
            }
        )

        if (state.error != null && state.generatedKeys.isNotEmpty()) {
            Spacer(modifier = Modifier.height(MomoSpacing.Sm))
            Text(
                text = state.error!!,
                color = MomoColors.StatusFailed,
                style = MomoTypography.BodySmall
            )
        }
    }
}

@Composable
private fun KeyRow(keyInfo: GeneratedKeyInfo) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MomoSpacing.Lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(MomoColors.StatusConfirmed, CircleShape)
        )
        Spacer(modifier = Modifier.width(MomoSpacing.Sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = keyInfo.label,
                style = MomoTypography.BodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MomoColors.TextPrimary
            )
            Text(
                text = keyInfo.keyValue,
                style = MomoTypography.BodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MomoColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(MomoSpacing.Sm))
        IconButton(onClick = {
            clipboard.setText(AnnotatedString(keyInfo.keyValue))
            copied = true
        }) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = if (copied) MomoColors.StatusConfirmed else MomoColors.Gold
            )
        }
    }
}

// ── Step 2 — Relay Connection ────────────────────────────────────────

@Composable
private fun RelayContent(
    state: SetupUiState,
    viewModel: SetupViewModel
) {
    val relayState by viewModel.connectionState.collectAsStateWithLifecycle()
    var showAdvanced by remember { mutableStateOf(false) }

    Spacer(modifier = Modifier.height(MomoSpacing.Xl))
    StepHeadline("Connect to Relay")
    StepBody("The relay routes verification requests from your website to your phone. One click to join.")

    // Relay status card
    Card(
        colors = CardDefaults.cardColors(containerColor = MomoColors.GroundLight),
        shape = MomoShapes.CardShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MomoSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Shared Relay",
                    style = MomoTypography.BodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MomoColors.TextPrimary
                )
                Text(
                    text = state.relayUrl,
                    style = MomoTypography.BodySmall,
                    color = MomoColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(MomoSpacing.Sm))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        when (relayState.status) {
                            RelayConnectionStatus.CONNECTED -> MomoColors.StatusConfirmed
                            RelayConnectionStatus.CONNECTING,
                            RelayConnectionStatus.RECONNECTING -> MomoColors.StatusPending
                            RelayConnectionStatus.DISCONNECTED -> MomoColors.StatusFailed
                        },
                        CircleShape
                    )
            )
        }
    }

    Spacer(modifier = Modifier.height(MomoSpacing.Sm))

    Text(
        text = when (relayState.status) {
            RelayConnectionStatus.CONNECTED -> "Connected to relay"
            RelayConnectionStatus.CONNECTING -> "Connecting to relay\u2026"
            RelayConnectionStatus.RECONNECTING -> "Reconnecting\u2026"
            RelayConnectionStatus.DISCONNECTED -> "Not connected"
        },
        style = MomoTypography.BodySmall,
        color = when (relayState.status) {
            RelayConnectionStatus.CONNECTED -> MomoColors.StatusConfirmed
            else -> MomoColors.TextTertiary
        }
    )

    Spacer(modifier = Modifier.height(MomoSpacing.Lg))

    // Advanced toggle
    Text(
        text = if (showAdvanced) "Hide advanced" else "Use a different relay server?",
        color = MomoColors.Gold,
        style = MomoTypography.LabelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clickable { showAdvanced = !showAdvanced }
    )

    AnimatedVisibility(
        visible = showAdvanced,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column {
            Spacer(modifier = Modifier.height(MomoSpacing.Sm))
            MomoTextField(
                value = state.relayUrl,
                onValueChange = viewModel::updateRelayUrl,
                label = "Custom Relay URL",
                placeholder = com.momobridge.service.RelayClient.DEFAULT_RELAY_URL
            )
            Spacer(modifier = Modifier.height(MomoSpacing.Xs))
            Text(
                text = "Point to your own relay server if you\u2019re self-hosting.",
                style = MomoTypography.BodySmall,
                color = MomoColors.TextTertiary
            )
        }
    }

    if (state.connectionError != null) {
        Spacer(modifier = Modifier.height(MomoSpacing.Md))
        Text(
            text = state.connectionError!!,
            color = MomoColors.StatusFailed,
            style = MomoTypography.BodySmall
        )
    }

    if (state.error != null) {
        Spacer(modifier = Modifier.height(MomoSpacing.Sm))
        Text(
            text = state.error!!,
            color = MomoColors.StatusFailed,
            style = MomoTypography.BodySmall
        )
    }
}

// ── Step 3 — Sender Configuration ────────────────────────────────────

@Composable
private fun SendersContent(
    state: SetupUiState,
    viewModel: SetupViewModel
) {
    Spacer(modifier = Modifier.height(MomoSpacing.Xl))
    StepHeadline("Monitor SMS Senders")
    StepBody("Select which payment SMS providers to watch. We found these in your inbox:")

    if (state.scanningInbox) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                color = MomoColors.Gold,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(MomoSpacing.Md))
            Text(
                text = "Scanning SMS inbox\u2026",
                style = MomoTypography.BodyMedium,
                color = MomoColors.TextSecondary
            )
        }
    }

    state.scannedSenders.forEach { sender ->
        val selected = state.selectedSenders.contains(sender.address)
        Card(
            shape = MomoShapes.CardShape,
            colors = CardDefaults.cardColors(
                containerColor = if (selected) MomoColors.Highlight else MomoColors.GroundMedium
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MomoSpacing.Xs)
                .clickable { viewModel.toggleSender(sender.address) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MomoSpacing.Lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            if (selected) MomoColors.StatusConfirmed else MomoColors.TextTertiary.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Text(
                            text = "\u2713",
                            color = MomoColors.OnGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(MomoSpacing.Md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sender.address,
                        style = MomoTypography.BodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MomoColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${sender.messageCount} messages found",
                        style = MomoTypography.LabelSmall,
                        color = MomoColors.TextSecondary
                    )
                }
            }
        }
    }

    if (state.scannedSenders.isEmpty() && !state.scanningInbox) {
        Text(
            text = "No SMS senders found in your inbox. You can configure senders later in Settings.",
            style = MomoTypography.BodySmall,
            color = MomoColors.TextTertiary,
            modifier = Modifier.padding(vertical = MomoSpacing.Lg)
        )
    }
}

// ── Bottom action buttons ────────────────────────────────────────────

@Composable
private fun SetupActionButtons(
    state: SetupUiState,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit
) {
    val primaryText: String
    val secondaryText: String
    val showSecondary: Boolean
    val primaryLoading: Boolean
    val primaryEnabled: Boolean

    when (state.step) {
        0 -> {
            primaryText = "Next \u2014 Add a Store \u2192"
            secondaryText = ""
            showSecondary = false
            primaryLoading = false
            primaryEnabled = state.ownerName.isNotBlank()
        }
        1 -> {
            primaryText = "Next \u2192"
            secondaryText = "\u2190 Back"
            showSecondary = true
            primaryLoading = false
            primaryEnabled = state.generatedKeys.isNotEmpty()
        }
        2 -> {
            primaryText = if (state.isConnecting) "Connecting\u2026" else "Connect"
            secondaryText = "Skip \u2192"
            showSecondary = true
            primaryLoading = state.isConnecting
            primaryEnabled = !state.isConnecting && state.relayUrl.isNotBlank()
        }
        3 -> {
            primaryText = "Finish Setup"
            secondaryText = "Skip"
            showSecondary = true
            primaryLoading = false
            primaryEnabled = true
        }
        else -> {
            primaryText = ""
            secondaryText = ""
            showSecondary = false
            primaryLoading = false
            primaryEnabled = false
        }
    }

    if (showSecondary) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MomoSpacing.Lg)
                .padding(bottom = MomoSpacing.Xl),
            horizontalArrangement = Arrangement.spacedBy(MomoSpacing.Sm)
        ) {
            GoldOutlineButton(
                text = secondaryText,
                onClick = onSecondary,
                enabled = !primaryLoading,
                modifier = Modifier.weight(1f)
            )
            GoldButton(
                text = primaryText,
                onClick = onPrimary,
                enabled = primaryEnabled,
                loading = primaryLoading,
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        GoldButton(
            text = primaryText,
            onClick = onPrimary,
            enabled = primaryEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MomoSpacing.Lg)
                .padding(bottom = MomoSpacing.Xl)
        )
    }
}
