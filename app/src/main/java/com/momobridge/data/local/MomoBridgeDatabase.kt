package com.momobridge.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SmsTransactionEntity::class, ApiKeyEntity::class],
    version = 3,
    exportSchema = false
)
abstract class MomoBridgeDatabase : RoomDatabase() {
    abstract fun smsTransactionDao(): SmsTransactionDao
    abstract fun apiKeyDao(): ApiKeyDao
}
