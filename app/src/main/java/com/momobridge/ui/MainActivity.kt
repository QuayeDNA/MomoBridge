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
import androidx.lifecycle.lifecycleScope
import com.momobridge.data.repository.TransactionRepository
import com.momobridge.service.SmsListenerService
import com.momobridge.ui.navigation.AppNavigation
import com.momobridge.ui.theme.MomoBridgeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var transactionRepository: TransactionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#0A0E1A")))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window?.setNavigationBarContrastEnforced(false)
        }

        if (intent?.getBooleanExtra("debug_dump", false) == true) {
            performDebugDump()
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
        if (intent.getBooleanExtra("debug_dump", false) == true) {
            performDebugDump()
        }
    }

    private fun performDebugDump() {
        lifecycleScope.launch {
            try {
                val allTxn = withContext(Dispatchers.IO) {
                    transactionRepository.observeTransactions().first()
                }
                val arr = JSONArray()
                for (txn in allTxn) {
                    arr.put(JSONObject().apply {
                        put("id", txn.id)
                        put("network", txn.network)
                        put("reference", txn.reference)
                        put("amount", txn.amount)
                        put("senderName", txn.senderName ?: JSONObject.NULL)
                        put("senderPhone", txn.senderPhone ?: JSONObject.NULL)
                        put("balanceAfter", txn.balanceAfter ?: JSONObject.NULL)
                        put("rawSms", txn.rawSms)
                        put("receivedAt", txn.receivedAt)
                        put("createdAt", txn.createdAt)
                        put("expiresAt", txn.expiresAt)
                        put("confirmedAt", txn.confirmedAt ?: JSONObject.NULL)
                        put("claimedByKeyLabel", txn.claimedByKeyLabel ?: JSONObject.NULL)
                        put("status", txn.status)
                    })
                }
                val root = JSONObject().apply {
                    put("exported_count", allTxn.size)
                    put("exported_at", System.currentTimeMillis())
                    put("transactions", arr)
                }
                val file = File(cacheDir, "debug_dump.json")
                withContext(Dispatchers.IO) {
                    file.writeText(root.toString(2))
                }
                android.util.Log.i("DebugDump", "Exported ${allTxn.size} transactions to ${file.absolutePath}")
            } catch (e: Exception) {
                android.util.Log.e("DebugDump", "Failed: ${e.message}")
            }
        }
    }
}
