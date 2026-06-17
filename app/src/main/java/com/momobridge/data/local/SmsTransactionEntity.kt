package com.momobridge.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "sms_transactions",
    indices = [
        Index(value = ["reference"], unique = true)
    ]
)
data class SmsTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val network: String,
    val reference: String,
    val amount: Double,
    val senderName: String?,
    val senderPhone: String?,
    val balanceAfter: Double?,
    val rawSms: String,
    val receivedAt: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long,
    val confirmedAt: Long? = null,
    val claimedByKeyLabel: String? = null,
    val status: String = PENDING
) {
    companion object {
        const val PENDING = "PENDING"
        const val CONFIRMED = "CONFIRMED"
        const val FAILED = "FAILED"
        const val EXPIRED = "EXPIRED"
    }
}
