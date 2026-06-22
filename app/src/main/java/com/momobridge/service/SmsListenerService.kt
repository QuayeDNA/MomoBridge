package com.momobridge.service

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.momobridge.MomoBridgeApp
import com.momobridge.R
import com.momobridge.data.local.SmsTransactionEntity
import com.momobridge.data.repository.SmsSourceRepository
import com.momobridge.data.repository.TransactionRepository
import com.momobridge.di.RegularPrefs
import com.momobridge.domain.usecase.LlmFallbackUseCase
import com.momobridge.domain.parser.SmsParser
import com.momobridge.domain.usecase.ProcessSmsUseCase
import com.momobridge.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsListenerService : Service() {

    @Inject lateinit var processSmsUseCase: ProcessSmsUseCase
    @Inject lateinit var smsSourceRepository: SmsSourceRepository
    @Inject lateinit var llmFallbackUseCase: LlmFallbackUseCase
    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var relayClient: RelayClient
    @Inject @RegularPrefs lateinit var regularPrefs: SharedPreferences

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        startExpiryChecker()
        startHealthHeartbeat()

        // When service restarts without SMS data, ensure relay is connected
        if (intent == null && relayClient.isSetupDone()) {
            val state = relayClient.connectionState.value
            if (state.status == RelayConnectionStatus.DISCONNECTED) {
                relayClient.connect()
            }
        }

        if (intent != null) {
            val sender = intent.getStringExtra("sender") ?: return START_STICKY
            val body = intent.getStringExtra("body") ?: return START_STICKY
            val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())

            val rule = smsSourceRepository.findRule(sender)
            val parsed = if (rule != null) {
                SmsParser.parse(body, rule, timestamp, sender)
            } else {
                SmsParser.parseHeuristic(body, timestamp, sender)
            }
            if (parsed == null) {
                stopSelf()
                return START_STICKY
            }

            scope.launch {
                val isGood = parsed.confidence >= 0.6 &&
                    parsed.reference != null && parsed.amount != null
                val status = if (isGood) SmsTransactionEntity.PENDING else SmsTransactionEntity.FAILED
                processSmsUseCase(parsed, status)

                if (isGood) {
                    smsSourceRepository.resetFailureCount(sender)
                } else {
                    smsSourceRepository.incrementFailureCount(sender)
                    val source = smsSourceRepository.findSourceByAddress(sender)
                    if (source != null && source.consecutiveParseFailures >= 3) {
                        postRetrainNotification(sender, source.label, body)
                        // Layer 5: try LLM fallback
                        if (llmFallbackUseCase.isAvailable()) {
                            val llmResult = llmFallbackUseCase.extract(body, timestamp, sender)
                            if (llmResult != null) {
                                processSmsUseCase(llmResult, SmsTransactionEntity.PENDING)
                            }
                        }
                    }
                }
            }
        }

        return START_STICKY
    }

    private fun postRetrainNotification(senderAddress: String, label: String, smsBody: String) {
        val notification = NotificationCompat.Builder(this, MomoBridgeApp.CHANNEL_ID_EVENTS)
            .setContentTitle("SMS format may have changed")
            .setContentText("The $label format seems to have changed. Tap to update.")
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(
                android.app.PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, com.momobridge.ui.MainActivity::class.java).apply {
                        putExtra(EXTRA_RETRAIN_SENDER, senderAddress)
                        putExtra(EXTRA_RETRAIN_LABEL, label)
                        putExtra(EXTRA_RETRAIN_BODY, smsBody)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(RETRAIN_NOTIFICATION_ID, notification)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, MomoBridgeApp.CHANNEL_ID)
        .setContentTitle(getString(R.string.service_notification_title))
        .setContentText(getString(R.string.service_notification_text))
        .setSmallIcon(R.drawable.ic_notification)
        .setOngoing(true)
        .setContentIntent(
            android.app.PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()

    private fun startExpiryChecker() {
        scope.launch {
            while (true) {
                delay(60_000L)
                try {
                    val expired = transactionRepository.getExpiredPending()
                    if (expired.isNotEmpty()) {
                        expired.forEach { txn ->
                            notificationHelper.notifyTransactionExpired(txn)
                        }
                        transactionRepository.markExpired()
                    }
                } catch (_: Exception) { }
            }
        }
    }

    private fun startHealthHeartbeat() {
        scope.launch {
            while (true) {
                delay(30_000L)
                regularPrefs.edit().putLong(KEY_LISTENER_LAST_ALIVE, System.currentTimeMillis()).apply()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val restartIntent = Intent(this, SmsListenerService::class.java)
        startService(restartIntent)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val RETRAIN_NOTIFICATION_ID = 1002
        private const val KEY_LISTENER_LAST_ALIVE = "listener_last_alive"
        const val EXTRA_RETRAIN_SENDER = "retrain_sender_address"
        const val EXTRA_RETRAIN_LABEL = "retrain_label"
        const val EXTRA_RETRAIN_BODY = "retrain_sms_body"
    }
}
