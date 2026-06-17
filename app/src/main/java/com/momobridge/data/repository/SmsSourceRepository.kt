package com.momobridge.data.repository

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.momobridge.di.RegularPrefs
import com.momobridge.domain.model.ParsingRule
import com.momobridge.domain.model.SmsSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsSourceRepository @Inject constructor(
    @RegularPrefs private val prefs: SharedPreferences
) {
    private val gson = Gson()

    fun getSources(): List<SmsSource> {
        val json = prefs.getString(KEY_SOURCES, null) ?: return emptyList()
        val type = object : TypeToken<List<SmsSource>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getEnabledSenderAddresses(): List<String> =
        getSources().filter { it.enabled }.map { it.senderAddress }

    fun findRule(senderAddress: String): ParsingRule? {
        return getSources().firstOrNull { it.senderAddress.equals(senderAddress, ignoreCase = true) }
            ?.parsingRule
    }

    fun addSource(source: SmsSource) {
        val sources = getSources().toMutableList()
        sources.removeAll { it.senderAddress.equals(source.senderAddress, ignoreCase = true) }
        sources.add(source)
        saveSources(sources)
    }

    fun removeSource(id: String) {
        val sources = getSources().toMutableList()
        sources.removeAll { it.id == id }
        saveSources(sources)
    }

    fun toggleSource(id: String, enabled: Boolean) {
        val sources = getSources().toMutableList()
        val idx = sources.indexOfFirst { it.id == id }
        if (idx >= 0) {
            sources[idx] = sources[idx].copy(enabled = enabled)
            saveSources(sources)
        }
    }

    fun updateRule(id: String, rule: ParsingRule) {
        val sources = getSources().toMutableList()
        val idx = sources.indexOfFirst { it.id == id }
        if (idx >= 0) {
            sources[idx] = sources[idx].copy(parsingRule = rule)
            saveSources(sources)
        }
    }

    fun hasAnyRule(): Boolean = getSources().any { it.parsingRule != null }

    fun clearAll() {
        prefs.edit().remove(KEY_SOURCES).apply()
    }

    private fun saveSources(sources: List<SmsSource>) {
        prefs.edit().putString(KEY_SOURCES, gson.toJson(sources)).apply()
    }

    companion object {
        private const val KEY_SOURCES = "sms_sources"
    }
}
