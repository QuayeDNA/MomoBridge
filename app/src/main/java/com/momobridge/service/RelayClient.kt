package com.momobridge.service

import android.content.SharedPreferences
import android.util.Log
import com.momobridge.data.repository.ApiKeyRepository
import com.momobridge.di.RegularPrefs
import com.momobridge.di.SecurePrefs
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class RelayConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

data class RelayState(
    val status: RelayConnectionStatus = RelayConnectionStatus.DISCONNECTED,
    val url: String? = null
)

@Singleton
class RelayClient @Inject constructor(
    @SecurePrefs private val securePrefs: SharedPreferences,
    @RegularPrefs private val prefs: SharedPreferences,
    private val claimHandler: ClaimHandler,
    private val apiKeyRepository: ApiKeyRepository,
    private val notificationHelper: NotificationHelper
) {

    companion object {
        private const val TAG = "RelayClient"
        private const val HEARTBEAT_INTERVAL = 30_000L
        private const val BASE_RECONNECT_DELAY = 1_000L
        private const val MAX_RECONNECT_DELAY = 60_000L
        private const val KEY_API_KEY = "api_key"
        private const val KEY_RELAY_URL = "relay_url"
        private const val KEY_SETUP_DONE = "setup_done"

        const val DEFAULT_RELAY_URL = "https://momobridge-relay.onrender.com"
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectAttempt = 0
    private var shouldReconnect = false

    private val _connectionState = MutableStateFlow(RelayState())
    val connectionState: StateFlow<RelayState> = _connectionState

    fun getApiKey(): String = securePrefs.getString(KEY_API_KEY, "") ?: ""

    fun saveApiKey(key: String) {
        securePrefs.edit().putString(KEY_API_KEY, key).apply()
    }

    fun getRelayUrl(): String = prefs.getString(KEY_RELAY_URL, "") ?: ""

    fun saveRelayUrl(url: String) {
        prefs.edit().putString(KEY_RELAY_URL, url).apply()
    }

    fun isSetupDone(): Boolean = prefs.getBoolean(KEY_SETUP_DONE, false)

    fun markSetupDone() {
        prefs.edit().putBoolean(KEY_SETUP_DONE, true).apply()
    }

    fun resetSetup() {
        securePrefs.edit().remove(KEY_API_KEY).apply()
        prefs.edit()
            .remove(KEY_RELAY_URL)
            .remove(KEY_SETUP_DONE)
            .remove("historical_scan_done")
            .apply()
        disconnect()
    }

    fun connect() {
        val relayUrl = getRelayUrl()
        if (relayUrl.isEmpty()) {
            _connectionState.value = RelayState(RelayConnectionStatus.DISCONNECTED, null)
            return
        }

        // Use the primary key from legacy pref, OR fall back to any active key
        val primaryKey = getApiKey()

        shouldReconnect = true
        _connectionState.value = RelayState(RelayConnectionStatus.CONNECTING, relayUrl)
        reconnectAttempt = 0

        doConnect(relayUrl, primaryKey)
    }

    fun disconnect() {
        shouldReconnect = false
        heartbeatJob?.cancel()
        webSocket?.close(1000, "client shutdown")
        webSocket = null
        _connectionState.value = RelayState(RelayConnectionStatus.DISCONNECTED, getRelayUrl())
    }

    fun sendPong() {
        webSocket?.let {
            try {
                it.send(gson.toJson(mapOf("type" to "pong")))
            } catch (_: Exception) { }
        }
    }

    fun revokeKeyOnRelay(keyValue: String) {
        sendJson(mapOf(
            "type" to "revoke_key",
            "apiKey" to keyValue
        ))
    }

    private fun doConnect(url: String, primaryKey: String) {
        val wsUrl = url.trimEnd('/').replace("http://", "ws://").replace("https://", "wss://")

        // If no primary key, use any active key for the WebSocket handshake
        val handshakeKey = if (primaryKey.isNotEmpty()) primaryKey else {
            try {
                kotlinx.coroutines.runBlocking {
                    val keys = apiKeyRepository.getAllActiveKeyValues()
                    keys.firstOrNull() ?: primaryKey
                }
            } catch (_: Exception) { primaryKey }
        }

        if (handshakeKey.isEmpty()) {
            Log.w(TAG, "No API keys available — cannot connect")
            _connectionState.value = RelayState(RelayConnectionStatus.DISCONNECTED, url)
            return
        }

        val request = Request.Builder()
            .url("$wsUrl/ws?apiKey=$handshakeKey")
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                _connectionState.value = RelayState(RelayConnectionStatus.CONNECTED, url)
                reconnectAttempt = 0
                startHeartbeat()

                // Authenticate with ALL active keys
                scope.launch {
                    try {
                        val allKeys = apiKeyRepository.getAllActiveKeyValues()
                        val authMsg = mutableMapOf<String, Any>(
                            "type" to "auth",
                            "apiKey" to handshakeKey
                        )
                        if (allKeys.isNotEmpty()) {
                            authMsg["allKeys"] = allKeys
                        } else if (handshakeKey.isNotEmpty()) {
                            authMsg["allKeys"] = listOf(handshakeKey)
                        }
                        sendJson(authMsg)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send auth: ${e.message}")
                    }
                }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code $reason")
                ws.close(code, reason)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code $reason")
                handleDisconnect()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
                handleDisconnect()
            }
        })
    }

    private fun handleMessage(text: String) {
        try {
            val msg = gson.fromJson(text, Map::class.java)
            val type = msg["type"] as? String ?: return

            when (type) {
                "auth_ok" -> {
                    Log.d(TAG, "Authenticated with relay")
                }
                "revoke_ok" -> {
                    Log.d(TAG, "Key revoked on relay")
                }
                "claim_request" -> {
                    val claimId = msg["claimId"] as? String ?: return
                    val reference = msg["reference"] as? String ?: return
                    val apiKey = msg["apiKey"] as? String
                    handleClaimRequest(claimId, reference, apiKey)
                }
                "pong" -> { }
                "error" -> {
                    Log.w(TAG, "Relay error: ${msg["message"]}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message: ${e.message}")
        }
    }

    private fun handleClaimRequest(claimId: String, reference: String, apiKey: String?) {
        scope.launch {
            // Look up the key label if apiKey was provided
            var keyLabel: String? = null
            if (apiKey != null) {
                val entity = apiKeyRepository.findEntityByKeyValue(apiKey)
                if (entity != null) {
                    keyLabel = entity.label
                    apiKeyRepository.markUsed(entity.id)
                }
            }

            val result = claimHandler.handleClaim(reference, keyLabel)

            if (result.confirmed) {
                notificationHelper.notifyClaimConfirmed(result)
            } else {
                val msg = (result.message ?: "").lowercase()
                if (msg.contains("already")) {
                    notificationHelper.notifyClaimAlreadyConfirmed(result)
                } else {
                    notificationHelper.notifyClaimError(reference)
                }
            }

            sendJson(mapOf(
                "type" to "claim_response",
                "claimId" to claimId,
                "confirmed" to result.confirmed,
                "message" to result.message,
                "transaction" to mapOf(
                    "reference" to result.reference,
                    "amount" to result.amount,
                    "network" to result.network,
                    "senderName" to result.senderName
                )
            ))
        }
    }

    private fun handleDisconnect() {
        heartbeatJob?.cancel()
        val url = getRelayUrl()
        _connectionState.value = RelayState(RelayConnectionStatus.RECONNECTING, url)

        if (shouldReconnect) {
            val delay = calculateBackoff()
            reconnectAttempt++
            scope.launch {
                delay(delay)
                if (shouldReconnect) {
                    val relayUrl = getRelayUrl()
                    if (relayUrl.isNotEmpty()) {
                        val key = getApiKey()
                        doConnect(relayUrl, key)
                    }
                }
            }
        }
    }

    private fun calculateBackoff(): Long {
        val delay = BASE_RECONNECT_DELAY * (1L shl minOf(reconnectAttempt, 6))
        return minOf(delay, MAX_RECONNECT_DELAY)
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                delay(HEARTBEAT_INTERVAL)
                sendJson(mapOf("type" to "ping"))
            }
        }
    }

    private fun sendJson(data: Map<*, *>) {
        webSocket?.let {
            try {
                it.send(gson.toJson(data))
            } catch (_: Exception) { }
        }
    }

    fun markHistoricalScanDone() {
        prefs.edit().putBoolean("historical_scan_done", true).apply()
    }

    fun isHistoricalScanDone(): Boolean = prefs.getBoolean("historical_scan_done", false)

    fun getDeviceId(): String {
        var id = prefs.getString("device_id", "")
        if (id.isNullOrEmpty()) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }
}
