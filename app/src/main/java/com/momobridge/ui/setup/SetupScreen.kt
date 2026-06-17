package com.momobridge.ui.setup

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VpnKey
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
import com.momobridge.ui.components.GoldButton
import com.momobridge.ui.components.GoldOutlineButton
import com.momobridge.ui.components.MomoTextField
import com.momobridge.ui.components.SectionHeader
import com.momobridge.ui.components.SectionSubtext
import com.momobridge.ui.components.StepIndicator
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isComplete) {
        onComplete()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MomoColors.GroundMedium
                ),
                navigationIcon = {
                    if (state.step > 0) {
                        IconButton(onClick = viewModel::goBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MomoColors.TextPrimary)
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
                .padding(MomoSpacing.Lg)
        ) {
            StepIndicator(
                currentStep = state.step,
                totalSteps = 4
            )

            when (state.step) {
                0 -> ProfileStep(state, viewModel)
                1 -> ApiKeysStep(state, viewModel)
                2 -> RelayConnectStep(state, viewModel)
                3 -> SenderConfigStep(state, viewModel)
            }
        }
    }
}

@Composable
private fun ProfileStep(state: SetupUiState, viewModel: SetupViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(MomoSpacing.Xl))
        Text(
            text = "👋",
            style = MomoTypography.TitleLarge
        )
        Spacer(modifier = Modifier.height(MomoSpacing.Lg))
        SectionHeader(title = "Welcome to MoMo Bridge")
        Spacer(modifier = Modifier.height(MomoSpacing.Xs))
        SectionSubtext(text = "Let's get started. What should we call you?")
        Spacer(modifier = Modifier.height(MomoSpacing.Xxl))

        MomoTextField(
            value = state.ownerName,
            onValueChange = viewModel::updateOwnerName,
            label = "Your Name",
            placeholder = "e.g. Dave"
        )

        if (state.error != null) {
            Spacer(modifier = Modifier.height(MomoSpacing.Lg))
            Text(
                text = state.error!!,
                color = MomoColors.StatusFailed,
                style = MomoTypography.BodySmall
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        GoldButton(
            text = "Next — Add a Store",
            onClick = viewModel::goToNextStep
        )
    }
}

@Composable
private fun ApiKeysStep(state: SetupUiState, viewModel: SetupViewModel) {
    val clipboard = LocalClipboardManager.current
    var copiedId by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SectionHeader(title = "Add Your First Store")
        Spacer(modifier = Modifier.height(MomoSpacing.Xs))
        SectionSubtext(text = "Each store or location gets its own API key. Websites use this key to send verification requests to your app.")
        Spacer(modifier = Modifier.height(MomoSpacing.Lg))

        // Label input + generate button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            MomoTextField(
                value = state.newKeyLabel,
                onValueChange = viewModel::updateNewKeyLabel,
                label = "Store Label",
                placeholder = "e.g. Main Shop",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(MomoSpacing.Sm))
            GoldButton(
                text = "Generate",
                onClick = viewModel::generateKey,
                enabled = state.newKeyLabel.isNotBlank(),
                fullWidth = false,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(MomoSpacing.Lg))

        // Generated keys list
        state.generatedKeys.forEach { keyInfo ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MomoColors.GroundLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MomoSpacing.Xs)
            ) {
                Column(modifier = Modifier.padding(MomoSpacing.Lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MomoColors.StatusConfirmed, CircleShape)
                        )
                        Spacer(Modifier.width(MomoSpacing.Sm))
                        Text(
                            text = keyInfo.label,
                            style = MomoTypography.BodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MomoColors.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(MomoSpacing.Sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = keyInfo.keyValue,
                            style = MomoTypography.BodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MomoColors.TextSecondary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(MomoSpacing.Sm))
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(keyInfo.keyValue))
                            copiedId = keyInfo.id
                        }) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = if (copiedId == keyInfo.id) MomoColors.StatusConfirmed else MomoColors.Gold
                            )
                        }
                    }
                }
            }
        }

        // Add another store link
        if (state.generatedKeys.isNotEmpty()) {
            Spacer(modifier = Modifier.height(MomoSpacing.Sm))
            Text(
                text = "+ Add another store",
                color = MomoColors.Gold,
                style = MomoTypography.LabelSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    viewModel.updateNewKeyLabel("")
                }
            )
        }

        if (state.error != null) {
            Spacer(modifier = Modifier.height(MomoSpacing.Lg))
            Text(
                text = state.error!!,
                color = MomoColors.StatusFailed,
                style = MomoTypography.BodySmall
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (state.generatedKeys.isNotEmpty()) {
            GoldButton(
                text = "Next — Connect Relay",
                onClick = viewModel::goToNextStep
            )
        }
    }
}

@Composable
private fun RelayConnectStep(state: SetupUiState, viewModel: SetupViewModel) {
    val relayState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isConnected = relayState.status == com.momobridge.service.RelayConnectionStatus.CONNECTED
    var showAdvanced by remember { mutableStateOf(false) }
    val isDefaultUrl = state.relayUrl == com.momobridge.service.RelayClient.DEFAULT_RELAY_URL

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SectionHeader(title = "Connect to Relay")
        Spacer(modifier = Modifier.height(MomoSpacing.Xs))
        SectionSubtext(text = "Your app connects to a shared relay server. The relay routes payment verification requests from websites to your app using your API keys.")
        Spacer(modifier = Modifier.height(MomoSpacing.Lg))

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
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            when (relayState.status) {
                                com.momobridge.service.RelayConnectionStatus.CONNECTED -> MomoColors.StatusConfirmed
                                com.momobridge.service.RelayConnectionStatus.CONNECTING,
                                com.momobridge.service.RelayConnectionStatus.RECONNECTING -> MomoColors.StatusPending
                                com.momobridge.service.RelayConnectionStatus.DISCONNECTED -> MomoColors.StatusFailed
                            },
                            CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(MomoSpacing.Md))

        Text(
            text = when (relayState.status) {
                com.momobridge.service.RelayConnectionStatus.CONNECTED -> "Connected to relay"
                com.momobridge.service.RelayConnectionStatus.CONNECTING -> "Connecting to relay..."
                com.momobridge.service.RelayConnectionStatus.RECONNECTING -> "Reconnecting..."
                com.momobridge.service.RelayConnectionStatus.DISCONNECTED -> "Not connected"
            },
            style = MomoTypography.BodySmall,
            color = when (relayState.status) {
                com.momobridge.service.RelayConnectionStatus.CONNECTED -> MomoColors.StatusConfirmed
                else -> MomoColors.TextTertiary
            }
        )

        Spacer(modifier = Modifier.height(MomoSpacing.Md))
        Text(
            text = if (showAdvanced) "Hide advanced" else "Use a different relay server?",
            color = MomoColors.Gold,
            style = MomoTypography.LabelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { showAdvanced = !showAdvanced }
        )

        if (showAdvanced) {
            Spacer(modifier = Modifier.height(MomoSpacing.Sm))
            MomoTextField(
                value = state.relayUrl,
                onValueChange = viewModel::updateRelayUrl,
                label = "Custom Relay URL",
                placeholder = com.momobridge.service.RelayClient.DEFAULT_RELAY_URL
            )
            Spacer(modifier = Modifier.height(MomoSpacing.Sm))
            Text(
                text = "Point to your own relay server if you're self-hosting.",
                style = MomoTypography.BodySmall,
                color = MomoColors.TextTertiary
            )
        }

        if (state.connectionError != null) {
            Spacer(modifier = Modifier.height(MomoSpacing.Md))
            Text(
                text = state.connectionError!!,
                color = MomoColors.StatusFailed,
                style = MomoTypography.BodySmall
            )
        }

        Spacer(modifier = Modifier.height(MomoSpacing.Xxl))
        GoldButton(
            text = if (state.isConnecting) "Connecting..." else "Connect",
            onClick = {
                viewModel.connectToRelay()
                viewModel.checkAndProceedFromRelay()
            },
            loading = state.isConnecting,
            enabled = !state.isConnecting && state.relayUrl.isNotBlank()
        )

        Spacer(modifier = Modifier.height(MomoSpacing.Xl))
        GoldOutlineButton(
            text = "Next — Configure Senders",
            onClick = viewModel::checkAndProceedFromRelay,
            enabled = isConnected
        )
    }
}

@Composable
private fun SenderConfigStep(state: SetupUiState, viewModel: SetupViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SectionHeader(title = "Monitor Senders")
        Spacer(modifier = Modifier.height(MomoSpacing.Xs))
        SectionSubtext(text = "Select which SMS senders to monitor for payments. You can configure parsing rules later in Settings.")
        Spacer(modifier = Modifier.height(MomoSpacing.Lg))

        if (state.scanningInbox) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = MomoColors.Gold,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(MomoSpacing.Md))
                Text(
                    text = "Scanning SMS inbox...",
                    style = MomoTypography.BodyMedium,
                    color = MomoColors.TextSecondary
                )
            }
        }

        state.scannedSenders.forEach { sender ->
            val selected = state.selectedSenders.contains(sender.address)
            Card(
                shape = com.momobridge.ui.theme.MomoShapes.CardShape,
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
                        .padding(MomoSpacing.CardPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (selected) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = if (selected) "Selected" else "Not selected",
                        tint = if (selected) MomoColors.StatusConfirmed else MomoColors.TextTertiary
                    )
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

        Spacer(modifier = Modifier.height(MomoSpacing.Xxl))

        GoldButton(
            text = "Finish Setup",
            onClick = viewModel::goToNextStep
        )

        Spacer(modifier = Modifier.height(MomoSpacing.Sm))

        GoldOutlineButton(
            text = "Skip — I'll configure later",
            onClick = viewModel::goToNextStep
        )
    }
}
