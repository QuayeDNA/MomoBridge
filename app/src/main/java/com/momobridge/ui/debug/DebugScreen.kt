package com.momobridge.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momobridge.ui.theme.MomoColors
import com.momobridge.ui.theme.MomoSpacing
import com.momobridge.ui.theme.MomoTypography

@Composable
fun DebugScreen(
    onBack: () -> Unit,
    viewModel: DebugViewModel = hiltViewModel()
) {
    val state by viewModel.dumpState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (state is DebugViewModel.DumpState.Idle) {
            viewModel.dumpTransactions()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MomoColors.GroundDark)
            .verticalScroll(rememberScrollState())
            .padding(MomoSpacing.Lg)
    ) {
        Text(
            text = "Debug Data Dump",
            style = MomoTypography.TitleLarge,
            fontWeight = FontWeight.Bold,
            color = MomoColors.Gold
        )
        Spacer(modifier = Modifier.height(MomoSpacing.Lg))

        when (val s = state) {
            is DebugViewModel.DumpState.Idle -> {}
            is DebugViewModel.DumpState.Dumping -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MomoColors.Gold)
                }
                Spacer(modifier = Modifier.height(MomoSpacing.Md))
                Text(
                    text = "Dumping transactions to JSON...",
                    style = MomoTypography.BodyMedium,
                    color = MomoColors.TextSecondary
                )
            }
            is DebugViewModel.DumpState.Done -> {
                Text(
                    text = "Exported ${s.count} transactions",
                    style = MomoTypography.BodyMedium,
                    color = MomoColors.StatusConfirmed,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(MomoSpacing.Sm))
                Text(
                    text = "Path: ${s.filePath}",
                    style = MomoTypography.BodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MomoColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(MomoSpacing.Lg))
                Text(
                    text = """Run this command on the dev machine to extract:

adb exec-out run-as com.momobridge cat ${s.filePath.replace("/data/data/com.momobridge/", "/data/data/com.momobridge/")} > debug_dump.json""",
                    style = MomoTypography.BodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MomoColors.TextSecondary
                )
            }
            is DebugViewModel.DumpState.Error -> {
                Text(
                    text = "Error: ${s.message}",
                    style = MomoTypography.BodyMedium,
                    color = MomoColors.StatusFailed,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
