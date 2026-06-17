package com.momobridge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.momobridge.service.SmsListenerService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences("momobridge_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("setup_done", false)) return

        val serviceIntent = Intent(context, SmsListenerService::class.java)
        context.startForegroundService(serviceIntent)
    }
}
