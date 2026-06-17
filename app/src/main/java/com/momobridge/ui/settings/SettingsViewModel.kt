package com.momobridge.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momobridge.data.repository.SmsSourceRepository
import com.momobridge.data.repository.TransactionRepository
import com.momobridge.di.RegularPrefs
import com.momobridge.domain.model.SmsSource
import com.momobridge.domain.parser.SmsParser
import com.momobridge.service.RelayClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(
    val smsSources: List<SmsSource> = emptyList(),
    val showScanResults: Boolean = false,
    val scannedSenders: List<ScannedSender> = emptyList(),
    val scanningInbox: Boolean = false,
    val scanningHistorical: Boolean = false,
    val historicalScanResult: String? = null,
    val expiryEnabled: Boolean = true,
    val expiryHours: Long = 168
)

data class ScannedSender(
    val address: String,
    val displayName: String,
    val messageCount: Int,
    val sampleBody: String
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @RegularPrefs private val prefs: SharedPreferences,
    private val smsSourceRepository: SmsSourceRepository,
    private val transactionRepository: TransactionRepository,
    private val relayClient: RelayClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val connectionState = relayClient.connectionState
    val apiKey: String get() = relayClient.getApiKey()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(
            smsSources = smsSourceRepository.getSources(),
            expiryEnabled = transactionRepository.isExpiryEnabled(),
            expiryHours = transactionRepository.getExpiryHours()
        )
    }

    val relayUrl: String get() = relayClient.getRelayUrl()

    fun updateRelayUrl(url: String) {
        relayClient.saveRelayUrl(url.trimEnd('/'))
        relayClient.disconnect()
        relayClient.connect()
    }

    fun reconnect() {
        relayClient.disconnect()
        relayClient.connect()
    }

    // ── Expiry Configuration ──────────────────────────────────────────────

    fun setExpiryEnabled(enabled: Boolean) {
        transactionRepository.setExpiryEnabled(enabled)
        _uiState.value = _uiState.value.copy(expiryEnabled = enabled)
    }

    fun setExpiryHours(hours: Long) {
        transactionRepository.setExpiryHours(hours)
        _uiState.value = _uiState.value.copy(expiryHours = hours)
    }

    // ── SMS Source Management ─────────────────────────────────────────────

    fun toggleSource(id: String, enabled: Boolean) {
        smsSourceRepository.toggleSource(id, enabled)
        refresh()
    }

    fun removeSource(id: String) {
        smsSourceRepository.removeSource(id)
        refresh()
    }

    fun addSourceManually(address: String, label: String) {
        if (address.isBlank()) return
        val source = SmsSource(
            senderAddress = address.trim(),
            label = label.ifBlank { address.trim() },
            enabled = true,
            parsingRule = null
        )
        smsSourceRepository.addSource(source)
        refresh()
    }

    fun scanInbox() {
        _uiState.value = _uiState.value.copy(scanningInbox = true, showScanResults = true)
        viewModelScope.launch {
            val senders = withContext(Dispatchers.IO) {
                val map = mutableMapOf<String, ScannedSender>()
                try {
                    val uri = Uri.parse("content://sms/inbox")
                    val projection = arrayOf(
                        Telephony.TextBasedSmsColumns.ADDRESS,
                        Telephony.TextBasedSmsColumns.BODY
                    )
                    val cursor = context.contentResolver.query(
                        uri, projection, null, null,
                        "${Telephony.TextBasedSmsColumns.DATE} DESC"
                    )
                    cursor?.use {
                        while (it.moveToNext()) {
                            val address = it.getString(0) ?: continue
                            val body = it.getString(1) ?: continue
                            val existing = map[address]
                            if (existing != null) {
                                map[address] = existing.copy(messageCount = existing.messageCount + 1)
                            } else {
                                map[address] = ScannedSender(
                                    address = address,
                                    displayName = address,
                                    messageCount = 1,
                                    sampleBody = body.take(200)
                                )
                            }
                        }
                    }
                } catch (_: Exception) { }
                map.values.sortedByDescending { it.messageCount }.take(50)
            }
            _uiState.value = _uiState.value.copy(
                scannedSenders = senders,
                scanningInbox = false
            )
        }
    }

    fun hideScanResults() {
        _uiState.value = _uiState.value.copy(showScanResults = false, scannedSenders = emptyList())
    }

    fun addSourceFromScan(address: String) {
        val existing = smsSourceRepository.getSources().any {
            it.senderAddress.equals(address, ignoreCase = true)
        }
        if (!existing) {
            val source = SmsSource(
                senderAddress = address,
                label = address,
                enabled = true,
                parsingRule = null
            )
            smsSourceRepository.addSource(source)
        }
        refresh()
    }

    // ── Historical Transaction Scan ──────────────────────────────────────────

    fun scanHistoricalTransactions() {
        val sources = smsSourceRepository.getSources().filter { it.enabled && it.parsingRule != null }
        if (sources.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                historicalScanResult = "No senders with parsing rules configured. Configure rules first under Monitored Senders."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            scanningHistorical = true, historicalScanResult = null
        )

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val twoMonthsAgo = System.currentTimeMillis() - (62L * 24 * 60 * 60 * 1000)
                var found = 0
                var skipped = 0

                for (source in sources) {
                    val rule = source.parsingRule!!
                    try {
                        val uri = Uri.parse("content://sms/inbox")
                        val projection = arrayOf(
                            Telephony.TextBasedSmsColumns.BODY,
                            Telephony.TextBasedSmsColumns.DATE
                        )
                        val selection = "${Telephony.TextBasedSmsColumns.ADDRESS} = ? " +
                            "AND ${Telephony.TextBasedSmsColumns.DATE} >= ?"
                        val selectionArgs = arrayOf(source.senderAddress, twoMonthsAgo.toString())
                        val cursor = context.contentResolver.query(
                            uri, projection, selection, selectionArgs,
                            "${Telephony.TextBasedSmsColumns.DATE} ASC"
                        )
                        cursor?.use {
                            while (it.moveToNext()) {
                                val body = it.getString(0) ?: continue
                                val timestamp = it.getLong(1)
                                val parsed = SmsParser.parse(
                                    body, rule, timestamp, source.senderAddress
                                )
                                if (parsed == null) {
                                    skipped++
                                    continue
                                }
                                val saved = transactionRepository.saveHistoricalTransaction(parsed)
                                if (saved) found++
                            }
                        }
                    } catch (_: Exception) { }
                }
                Pair(found, skipped)
            }

            val msg = buildString {
                append("Found ${result.first} past transaction(s). ")
                append("Skipped ${result.second} non-transaction messages.")
                if (result.first == 0) {
                    append(" No matching transactions found in the last 2 months.")
                }
            }
            _uiState.value = _uiState.value.copy(
                scanningHistorical = false,
                historicalScanResult = msg
            )
        }
    }

    fun clearHistoricalScanResult() {
        _uiState.value = _uiState.value.copy(historicalScanResult = null)
    }

    // ── Reconfigure ──────────────────────────────────────────────────────

    fun reconfigure() {
        relayClient.resetSetup()
        smsSourceRepository.clearAll()
        _uiState.value = SettingsUiState()
    }
}
