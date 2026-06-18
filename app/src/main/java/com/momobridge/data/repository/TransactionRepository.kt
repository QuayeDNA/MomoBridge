package com.momobridge.data.repository

import android.content.SharedPreferences
import com.momobridge.data.local.SmsTransactionDao
import com.momobridge.data.local.SmsTransactionEntity
import com.momobridge.di.RegularPrefs
import com.momobridge.domain.model.ParsedTransaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val dao: SmsTransactionDao,
    @RegularPrefs private val prefs: SharedPreferences
) {
    companion object {
        const val KEY_EXPIRY_ENABLED = "transaction_expiry_enabled"
        const val KEY_EXPIRY_HOURS = "transaction_expiry_hours"
        const val DEFAULT_EXPIRY_HOURS = 168L // 7 days
    }

    fun isExpiryEnabled(): Boolean = prefs.getBoolean(KEY_EXPIRY_ENABLED, true)
    fun getExpiryHours(): Long = prefs.getLong(KEY_EXPIRY_HOURS, DEFAULT_EXPIRY_HOURS)
    fun setExpiryEnabled(enabled: Boolean) { prefs.edit().putBoolean(KEY_EXPIRY_ENABLED, enabled).apply() }
    fun setExpiryHours(hours: Long) { prefs.edit().putLong(KEY_EXPIRY_HOURS, hours).apply() }

    private fun computeExpiresAt(receivedAt: Long): Long {
        return if (isExpiryEnabled()) {
            receivedAt + (getExpiryHours() * 60 * 60 * 1000L)
        } else {
            Long.MAX_VALUE
        }
    }

    suspend fun saveTransaction(parsed: ParsedTransaction, status: String = SmsTransactionEntity.PENDING): Long {
        val expiresAt = computeExpiresAt(parsed.receivedAt)
        val entity = SmsTransactionEntity(
            network = parsed.network,
            reference = parsed.reference ?: "",
            amount = parsed.amount ?: 0.0,
            senderName = parsed.senderName,
            senderPhone = parsed.senderPhone,
            balanceAfter = parsed.balanceAfter,
            rawSms = parsed.rawSms,
            receivedAt = parsed.receivedAt,
            expiresAt = expiresAt,
            status = status
        )
        return dao.insertOrIgnore(entity)
    }

    suspend fun saveHistoricalTransaction(parsed: ParsedTransaction): Boolean {
        val expiresAt = computeExpiresAt(parsed.receivedAt)
        val status = if (parsed.confidence >= 0.6 &&
            parsed.reference != null && parsed.amount != null
        ) SmsTransactionEntity.PENDING else SmsTransactionEntity.FAILED
        val entity = SmsTransactionEntity(
            network = parsed.network,
            reference = parsed.reference ?: "",
            amount = parsed.amount ?: 0.0,
            senderName = parsed.senderName,
            senderPhone = parsed.senderPhone,
            balanceAfter = parsed.balanceAfter,
            rawSms = parsed.rawSms,
            receivedAt = parsed.receivedAt,
            expiresAt = expiresAt,
            status = status
        )
        val id = dao.insertOrIgnore(entity)
        return id != -1L
    }

    suspend fun findByReference(reference: String): SmsTransactionEntity? {
        return dao.findByReference(reference)
    }

    suspend fun confirmTransaction(reference: String): Boolean {
        val now = System.currentTimeMillis()
        val rows = dao.confirmTransaction(reference, SmsTransactionEntity.CONFIRMED, now)
        return rows > 0
    }

    suspend fun confirmTransactionWithLabel(reference: String, keyLabel: String): Boolean {
        val now = System.currentTimeMillis()
        val rows = dao.confirmTransactionWithLabel(reference, SmsTransactionEntity.CONFIRMED, now, keyLabel)
        return rows > 0
    }

    suspend fun updateStatusByReference(reference: String, status: String) {
        dao.updateStatusByReference(reference, status)
    }

    fun observeTransactions(): Flow<List<SmsTransactionEntity>> = dao.observeAll()

    fun observePendingCount(): Flow<Int> = dao.observeCountByStatus(SmsTransactionEntity.PENDING)

    fun observeConfirmedCount(): Flow<Int> = dao.observeCountByStatus(SmsTransactionEntity.CONFIRMED)

    fun observeFailedCount(): Flow<Int> = dao.observeCountByStatus(SmsTransactionEntity.FAILED)

    fun observeExpiredCount(): Flow<Int> = dao.observeCountByStatus(SmsTransactionEntity.EXPIRED)

    fun observeTotalCount(): Flow<Int> = dao.observeTotalCount()

    fun observeCountByStatus(status: String): Flow<Int> = dao.observeCountByStatus(status)

    fun observeDistinctKeyLabels(): Flow<List<String>> = dao.observeDistinctKeyLabels()
}
