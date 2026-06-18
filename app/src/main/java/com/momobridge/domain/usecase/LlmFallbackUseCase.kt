package com.momobridge.domain.usecase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.momobridge.domain.model.ParsedTransaction
import com.momobridge.service.RelayClient
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmFallbackUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val relayClient: RelayClient,
    private val okHttpClient: OkHttpClient
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun isAvailable(): Boolean {
        val relayUrl = relayClient.getRelayUrl()
        if (relayUrl.isBlank()) return false
        if (!isNetworkAvailable()) return false
        return true
    }

    suspend fun extract(
        body: String,
        receivedAt: Long,
        sender: String
    ): ParsedTransaction? {
        val relayUrl = relayClient.getRelayUrl()
        if (relayUrl.isBlank()) return null
        if (!isNetworkAvailable()) return null

        val httpUrl = relayUrl
            .replace("wss://", "https://")
            .replace("ws://", "http://")
            .trimEnd('/')

        return try {
            val requestBody = JSONObject().apply {
                put("body", body)
            }.toString()

            val response = okHttpClient.newCall(
                Request.Builder()
                    .url("$httpUrl/llm-extract")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody.toRequestBody(jsonMediaType))
                    .build()
            ).execute()

            val responseBody = response.body?.string() ?: return null
            val json = JSONObject(responseBody)

            if (!json.optBoolean("success", false)) return null

            val data = json.getJSONObject("data")
            val ref = data.optString("reference", "")
            val amount = data.optDouble("amount", Double.NaN)

            if (ref.isBlank() || amount.isNaN()) return null

            ParsedTransaction(
                reference = ref.uppercase().trim(),
                amount = amount,
                senderName = data.optString("senderName", "").ifBlank { null },
                senderPhone = data.optString("senderPhone", "").ifBlank { null },
                balanceAfter = if (data.has("balanceAfter") && !data.isNull("balanceAfter"))
                    data.optDouble("balanceAfter") else null,
                network = sender,
                receivedAt = receivedAt,
                rawSms = body,
                confidence = 1.0,
                parsedBy = LLM_TAG
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        const val LLM_TAG = "LLM_FALLBACK"
    }
}