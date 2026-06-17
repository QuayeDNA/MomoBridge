package com.momobridge.service

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.momobridge.MomoBridgeApp
import com.momobridge.R
import com.momobridge.data.repository.SmsSourceRepository
import com.momobridge.di.RegularPrefs
import com.momobridge.domain.parser.SmsParser
import com.momobridge.domain.usecase.ProcessSmsUseCase
import com.momobridge.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsListenerService : Service() {

    @Inject lateinit var processSmsUseCase: ProcessSmsUseCase
    @Inject lateinit var smsSourceRepository: SmsSourceRepository
    @Inject @RegularPrefs lateinit var regularPrefs: SharedPreferences

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        if (intent != null) {
            val sender = intent.getStringExtra("sender") ?: return START_STICKY
            val body = intent.getStringExtra("body") ?: return START_STICKY
            val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())

            val rule = smsSourceRepository.findRule(sender)
            if (rule == null) {
                stopSelf()
                return START_STICKY
            }

            val parsed = SmsParser.parse(body, rule, timestamp, sender)
            if (parsed == null) {
                stopSelf()
                return START_STICKY
            }

            scope.launch {
                processSmsUseCase(parsed)
            }
        }

        return START_STICKY
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

    override fun onDestroy() {
        super.onDestroy()
        val restartIntent = Intent(this, SmsListenerService::class.java)
        startService(restartIntent)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
