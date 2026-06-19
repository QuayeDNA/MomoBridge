package com.momobridge.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsTransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: SmsTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(entity: SmsTransactionEntity): Long

    @Update
    suspend fun update(entity: SmsTransactionEntity)

    @Query("SELECT * FROM sms_transactions ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<SmsTransactionEntity>>

    @Query("SELECT COUNT(*) FROM sms_transactions WHERE status = :status")
    fun observeCountByStatus(status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM sms_transactions")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT * FROM sms_transactions WHERE reference = :reference LIMIT 1")
    suspend fun findByReference(reference: String): SmsTransactionEntity?

    @Query("UPDATE sms_transactions SET status = :status WHERE reference = :reference")
    suspend fun updateStatusByReference(reference: String, status: String)

    @Query("SELECT * FROM sms_transactions WHERE claimedByKeyLabel = :label ORDER BY receivedAt DESC")
    fun observeByClaimedKeyLabel(label: String): Flow<List<SmsTransactionEntity>>

    @Query("UPDATE sms_transactions SET status = :status, confirmedAt = :confirmedAt WHERE reference = :reference AND status = 'PENDING'")
    suspend fun confirmTransaction(reference: String, status: String, confirmedAt: Long): Int

    @Query("UPDATE sms_transactions SET status = :status, confirmedAt = :confirmedAt, claimedByKeyLabel = :keyLabel WHERE reference = :reference AND status = 'PENDING'")
    suspend fun confirmTransactionWithLabel(reference: String, status: String, confirmedAt: Long, keyLabel: String): Int

    @Query("SELECT * FROM sms_transactions WHERE status = 'PENDING' AND expiresAt < :now")
    suspend fun getExpiredPending(now: Long): List<SmsTransactionEntity>

    @Query("UPDATE sms_transactions SET status = 'EXPIRED' WHERE status = 'PENDING' AND expiresAt < :now")
    suspend fun markExpired(now: Long): Int

    @Query("DELETE FROM sms_transactions WHERE status = :status AND createdAt < :olderThan")
    suspend fun deleteOldRecords(status: String, olderThan: Long)

    @Query("SELECT DISTINCT claimedByKeyLabel FROM sms_transactions WHERE claimedByKeyLabel IS NOT NULL")
    fun observeDistinctKeyLabels(): Flow<List<String>>
}
