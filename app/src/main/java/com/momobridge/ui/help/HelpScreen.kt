package com.momobridge.ui.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Troubleshoot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoShapes
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoTypography

data class HelpTopic(
    val icon: ImageVector,
    val title: String,
    val content: String
)

val helpTopics = listOf(
    HelpTopic(
        icon = Icons.Default.Info,
        title = "About MoMo Bridge",
        content = "MoMo Bridge is a standalone Android app that acts as a local verification authority for Mobile Money payments. It intercepts incoming MoMo SMS confirmations from Ghanaian telecom providers (MTN, Telecel, AT, T-CASH), parses them, and stores them locally. When a customer makes a payment and enters their reference on a website, the app checks its local database and confirms or rejects the payment in real time."
    ),
    HelpTopic(
        icon = Icons.Default.Settings,
        title = "Setting Up",
        content = "To get started:\n1. Generate your unique API key — this identifies your app to the relay.\n2. Enter your relay server URL (e.g., from Render).\n3. The app connects to the relay via WebSocket.\n4. Configure which SMS senders to monitor (e.g., MobileMoney, T-CASH).\n5. For each sender, configure parsing rules so the app knows how to extract payment details.\n6. Done — your app is now monitoring payments and ready to verify claims."
    ),
    HelpTopic(
        icon = Icons.Default.PhoneAndroid,
        title = "Monitoring Senders",
        content = "The app intercepts SMS messages from configured senders only. When a new SMS arrives from a monitored sender, it:\n1. Reads the message body\n2. Extracts the transaction reference and amount using your configured parsing rules\n3. Saves the transaction locally with status PENDING\n\nThe transaction stays in your local Room database. No data ever leaves your device except through claim verification requests from your relay server."
    ),
    HelpTopic(
        icon = Icons.Default.CheckCircle,
        title = "Verifying Payments",
        content = "When a customer makes a payment and enters their reference on your website:\n1. The website sends the reference and amount to the relay server\n2. The relay forwards the request to your app via WebSocket\n3. The app checks its local database:\n   • Reference found + amount matches → CONFIRMED\n   • Reference not found → INVALID\n   • Already confirmed → ALREADY CONFIRMED\n   • Amount doesn't match → AMOUNT MISMATCH\n   • 24 hours passed → EXPIRED\n4. The result goes back: App → Relay → Website\n5. The website credits the customer's wallet"
    ),
    HelpTopic(
        icon = Icons.Default.Troubleshoot,
        title = "Troubleshooting",
        content = "Common issues:\n\n• \"Phone offline\" — Your app is not connected to the relay. Check your internet connection and verify the relay URL in Settings.\n\n• \"No messages found\" — The app may not have READ_SMS permission. Go to Settings → Apps → MoMo Bridge → Permissions and enable SMS.\n\n• \"Transaction not found\" — The SMS may not have been parsed correctly. Check the sender's parsing rules in Settings.\n\n• \"Amount mismatch\" — The amount on the website doesn't match the parsed amount. This can happen with partial payments.\n\n• \"Wrong data extracted\" — Reconfigure the sender's parsing rules. Delete the sender from Settings and set it up again."
    ),
    HelpTopic(
        icon = Icons.AutoMirrored.Filled.ContactSupport,
        title = "Contact Support",
        content = "For help, bugs, or feature requests, please email support@momobridge.com or visit our Help Center at momobridge.com/help."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Support") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MomoColors.GroundMedium
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MomoColors.TextPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(MomoSpacing.ScreenPadding)
                .verticalScroll(rememberScrollState())
        ) {
            helpTopics.forEach { topic ->
                HelpCard(topic = topic)
                Spacer(modifier = Modifier.height(MomoSpacing.Sm))
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "App Version 1.0.0",
                style = MomoTypography.LabelSmall,
                color = MomoColors.TextTertiary,
                modifier = Modifier.padding(vertical = MomoSpacing.Xl)
            )
        }
    }
}

@Composable
private fun HelpCard(topic: HelpTopic) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = MomoShapes.CardShape,
        colors = CardDefaults.cardColors(containerColor = MomoColors.GroundMedium),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(MomoSpacing.CardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = topic.icon,
                    contentDescription = null,
                    tint = MomoColors.Gold
                )
                Spacer(modifier = Modifier.width(MomoSpacing.Md))
                Text(
                    text = topic.title,
                    style = MomoTypography.BodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MomoColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MomoColors.TextTertiary
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Text(
                    text = topic.content,
                    style = MomoTypography.BodySmall,
                    color = MomoColors.TextSecondary,
                    modifier = Modifier.padding(top = MomoSpacing.Sm)
                )
            }
        }
    }
}
