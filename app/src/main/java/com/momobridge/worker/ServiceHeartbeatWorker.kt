package com.momobridge.worker

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.momobridge.service.SmsListenerService

class ServiceHeartbeatWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        private const val KEY_LISTENER_LAST_ALIVE = "listener_last_alive"
        private const val STALE_THRESHOLD_MS = 20 * 60 * 1000L
    }

    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("momobridge_prefs", Context.MODE_PRIVATE)
        val lastAlive = prefs.getLong(KEY_LISTENER_LAST_ALIVE, 0L)
        val now = System.currentTimeMillis()

        if (now - lastAlive > STALE_THRESHOLD_MS) {
            try {
                val intent = Intent(applicationContext, SmsListenerService::class.java)
                applicationContext.startForegroundService(intent)
            } catch (_: Exception) { }
        }

        return Result.success()
    }
}
