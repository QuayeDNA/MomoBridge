package com.momobridge.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.momobridge.MomoBridgeApp
import com.momobridge.R
import com.momobridge.data.local.SmsTransactionEntity
import com.momobridge.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun notifyClaimConfirmed(result: ClaimResult) {
        val ref = result.reference ?: ""
        val amount = result.amount ?: 0.0
        val sender = result.senderName?.let { "from $it" } ?: ""
        val label = result.claimedByKeyLabel?.let { " via $it" } ?: ""

        val text = buildString {
            append("GH\u20B5${"%.2f".format(amount)}")
            if (sender.isNotEmpty()) append(" $sender")
            if (label.isNotEmpty()) append(label)
            append(" confirmed")
        }

        val notification = NotificationCompat.Builder(context, MomoBridgeApp.CHANNEL_ID_EVENTS)
            .setContentTitle("Payment Confirmed")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(mainIntent())
            .build()

        val id = abs(ref.hashCode()) % 10000 + 3000
        manager.notify(id, notification)
    }

    fun notifyClaimAlreadyConfirmed(result: ClaimResult) {
        val ref = result.reference ?: ""
        val amount = result.amount ?: 0.0
        val text = "GH\u20B5${"%.2f".format(amount)} was already claimed"

        val notification = NotificationCompat.Builder(context, MomoBridgeApp.CHANNEL_ID_EVENTS)
            .setContentTitle("Already Confirmed")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(mainIntent())
            .build()

        val id = abs(ref.hashCode()) % 10000 + 4000
        manager.notify(id, notification)
    }

    fun notifyTransactionExpired(txn: SmsTransactionEntity) {
        val text = "GH\u20B5${"%.2f".format(txn.amount)} has expired — no claim was made"

        val notification = NotificationCompat.Builder(context, MomoBridgeApp.CHANNEL_ID_EVENTS)
            .setContentTitle("Transaction Expired")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(mainIntent())
            .build()

        val id = abs(txn.reference.hashCode()) % 10000 + 5000
        manager.notify(id, notification)
    }

    fun notifyClaimError(ref: String) {
        val text = "Ref: $ref — could not confirm"

        val notification = NotificationCompat.Builder(context, MomoBridgeApp.CHANNEL_ID_EVENTS)
            .setContentTitle("Verification Failed")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(mainIntent())
            .build()

        val id = abs(ref.hashCode()) % 10000 + 6000
        manager.notify(id, notification)
    }

    private fun mainIntent(): PendingIntent {
        return PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
