package com.momobridge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.momobridge.service.SmsListenerService

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val activeSenderIds = getActiveSenderIds(context)
        if (activeSenderIds.isEmpty()) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val sender = sms.originatingAddress ?: continue
            if (activeSenderIds.any { sender.equals(it, ignoreCase = true) }) {
                val serviceIntent = Intent(context, SmsListenerService::class.java).apply {
                    putExtra("sender", sender)
                    putExtra("body", sms.messageBody)
                    putExtra("timestamp", sms.timestampMillis)
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }

    private fun getActiveSenderIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences("momobridge_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("sms_sources", null) ?: return emptySet()
        return try {
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<com.momobridge.domain.model.SmsSource>>() {}.type
            val sources: List<com.momobridge.domain.model.SmsSource> = gson.fromJson(json, type)
                ?: emptyList()
            sources.filter { it.enabled }.map { it.senderAddress }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }
}
