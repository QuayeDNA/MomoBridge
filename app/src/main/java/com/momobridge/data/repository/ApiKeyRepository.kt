package com.momobridge.data.repository

import android.content.SharedPreferences
import com.momobridge.data.local.ApiKeyDao
import com.momobridge.data.local.ApiKeyEntity
import com.momobridge.di.SecurePrefs
import kotlinx.coroutines.flow.Flow
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyRepository @Inject constructor(
    private val dao: ApiKeyDao,
    @SecurePrefs private val securePrefs: SharedPreferences
) {
    companion object {
        private const val PREFIX = "mb_"
        private const val HEX_CHARS = 32
        private const val KEY_VALUE_PREFIX = "api_key_value_"
        private val random = SecureRandom()
    }

    private fun generateKeyValue(): String {
        val hex = ByteArray(HEX_CHARS).let { bytes ->
            random.nextBytes(bytes)
            bytes.joinToString("") { "%02x".format(it) }
        }
        return PREFIX + hex
    }

    private fun hashKey(key: String): String {
        return key.take(12)
    }

    fun observeAll(): Flow<List<ApiKeyEntity>> = dao.observeAll()

    fun observeActive(): Flow<List<ApiKeyEntity>> = dao.observeActive()

    fun observeActiveCount(): Flow<Int> = dao.observeActiveCount()

    suspend fun getActiveKeys(): List<ApiKeyEntity> = dao.getActiveKeys()

    suspend fun getAllKeys(): List<ApiKeyEntity> = dao.getAllKeys()

    suspend fun getKeyValue(id: Long): String {
        return securePrefs.getString("$KEY_VALUE_PREFIX$id", "") ?: ""
    }

    suspend fun getKeyValueByHash(hash: String): String? {
        val allVals = securePrefs.all.filterKeys { it.startsWith(KEY_VALUE_PREFIX) }
        for ((key, value) in allVals) {
            if (hashKey(value.toString()) == hash) {
                return value.toString()
            }
        }
        return null
    }

    suspend fun getKeyValueMap(): Map<Long, String> {
        val entities = dao.getAllKeys()
        val map = mutableMapOf<Long, String>()
        for (entity in entities) {
            val value = securePrefs.getString("$KEY_VALUE_PREFIX${entity.id}", "") ?: ""
            if (value.isNotEmpty()) {
                map[entity.id] = value
            }
        }
        return map
    }

    suspend fun createKey(label: String): ApiKeyEntity {
        val keyValue = generateKeyValue()
        val keyHash = hashKey(keyValue)
        val entity = ApiKeyEntity(
            label = label,
            keyHash = keyHash
        )
        val id = dao.insert(entity)
        val entityWithId = entity.copy(id = id)
        securePrefs.edit().putString("$KEY_VALUE_PREFIX$id", keyValue).apply()
        return entityWithId
    }

    suspend fun revokeKey(id: Long) {
        dao.setActive(id, false)
    }

    suspend fun reactivateKey(id: Long) {
        dao.setActive(id, true)
    }

    suspend fun permanentlyDelete(id: Long) {
        dao.delete(id)
        securePrefs.edit().remove("$KEY_VALUE_PREFIX$id").apply()
    }

    suspend fun updateLabel(id: Long, label: String) {
        dao.updateLabel(id, label)
    }

    suspend fun markUsed(id: Long) {
        dao.updateLastUsed(id, System.currentTimeMillis())
    }

    suspend fun findById(id: Long): ApiKeyEntity? = dao.findById(id)

    suspend fun findEntityByKeyValue(keyValue: String): ApiKeyEntity? {
        val hash = hashKey(keyValue)
        val allVals = securePrefs.all.filterKeys { it.startsWith(KEY_VALUE_PREFIX) }
        for ((prefKey, value) in allVals) {
            if (value.toString() == keyValue) {
                val idStr = prefKey.removePrefix(KEY_VALUE_PREFIX)
                val id = idStr.toLongOrNull() ?: continue
                return dao.findById(id)
            }
        }
        return null
    }

    suspend fun getAllActiveKeyValues(): List<String> {
        val active = dao.getActiveKeys()
        return active.mapNotNull { entity ->
            securePrefs.getString("$KEY_VALUE_PREFIX${entity.id}", "")
        }
    }
}
