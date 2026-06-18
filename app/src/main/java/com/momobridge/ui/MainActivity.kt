package com.momobridge.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.momobridge.service.SmsListenerService
import com.momobridge.ui.navigation.AppNavigation
import com.momobridge.ui.theme.MomoBridgeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#0A0E1A")))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window?.setNavigationBarContrastEnforced(false)
        }
        val retrainSender = intent?.getStringExtra(SmsListenerService.EXTRA_RETRAIN_SENDER)
        val retrainLabel = intent?.getStringExtra(SmsListenerService.EXTRA_RETRAIN_LABEL)
        val retrainBody = intent?.getStringExtra(SmsListenerService.EXTRA_RETRAIN_BODY)
        setContent {
            MomoBridgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        retrainSenderAddress = retrainSender,
                        retrainLabel = retrainLabel,
                        retrainSmsBody = retrainBody
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // handled via initial intent extras on fresh launch
    }
}
