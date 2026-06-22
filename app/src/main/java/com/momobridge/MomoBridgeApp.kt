package com.momobridge

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.momobridge.service.RelayClient
import com.momobridge.service.SmsListenerService
import com.momobridge.worker.ServiceHeartbeatWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MomoBridgeApp : Application(), Configuration.Provider {

    @Inject lateinit var relayClient: RelayClient

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        createEventChannel()
        connectRelayIfNeeded()
        startListenerService()
        scheduleServiceHeartbeat()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.WARN)
            .build()

    private fun connectRelayIfNeeded() {
        if (relayClient.isSetupDone()) {
            relayClient.connect()
        }
    }

    private fun startListenerService() {
        try {
            val intent = Intent(this, SmsListenerService::class.java)
            startForegroundService(intent)
        } catch (e: Exception) {
            android.util.Log.w("MomoBridgeApp", "Could not start listener service", e)
        }
    }

    private fun scheduleServiceHeartbeat() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<ServiceHeartbeatWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(constraints)
         .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "service_heartbeat",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_description)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "momobridge_sms_listener"
        const val CHANNEL_ID_EVENTS = "momobridge_events"
    }

    fun createEventChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID_EVENTS,
            "Transaction Events",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Claim and transaction notifications"
            setShowBadge(true)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
