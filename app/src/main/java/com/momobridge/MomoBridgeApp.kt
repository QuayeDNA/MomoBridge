package com.momobridge

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.momobridge.service.RelayClient
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltAndroidApp
class MomoBridgeApp : Application() {

    @Inject lateinit var relayClient: RelayClient

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        connectRelayIfNeeded()
    }

    private fun connectRelayIfNeeded() {
        if (relayClient.isSetupDone()) {
            relayClient.connect()

            // Listen for claim confirmations and show notification
            relayClient.connectionState
                .onEach { /* could show toast on connected */ }
                .launchIn(scope)
        }
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
