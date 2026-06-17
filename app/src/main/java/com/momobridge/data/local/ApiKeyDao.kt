package com.momobridge.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiKeyDao {

    @Query("SELECT * FROM api_keys ORDER BY isActive DESC, createdAt DESC")
    fun observeAll(): Flow<List<ApiKeyEntity>>

    @Query("SELECT * FROM api_keys WHERE isActive = 1 ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<ApiKeyEntity>>

    @Query("SELECT * FROM api_keys WHERE isActive = 1")
    suspend fun getActiveKeys(): List<ApiKeyEntity>

    @Query("SELECT * FROM api_keys ORDER BY isActive DESC, createdAt DESC")
    suspend fun getAllKeys(): List<ApiKeyEntity>

    @Query("SELECT * FROM api_keys WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ApiKeyEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ApiKeyEntity): Long

    @Update
    suspend fun update(entity: ApiKeyEntity)

    @Query("UPDATE api_keys SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)

    @Query("UPDATE api_keys SET label = :label WHERE id = :id")
    suspend fun updateLabel(id: Long, label: String)

    @Query("UPDATE api_keys SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Long, timestamp: Long)

    @Query("DELETE FROM api_keys WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM api_keys WHERE isActive = 1")
    fun observeActiveCount(): Flow<Int>
}
