package com.momobridge.ui.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momobridge.data.repository.SmsSourceRepository
import com.momobridge.data.repository.TransactionRepository
import com.momobridge.di.RegularPrefs
import com.momobridge.domain.model.SmsSource
import com.momobridge.domain.usecase.ScanInboxUseCase
import com.momobridge.service.RelayClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
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
    val reprocessing: Boolean = false,
    val reprocessResult: String? = null,
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
    @ApplicationContext private val context: Context,
    private val scanInboxUseCase: ScanInboxUseCase
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
        viewModelScope.launch {
            transactionRepository.recalculateExpiry()
            transactionRepository.purgeOldExpired()
        }
    }

    fun setExpiryHours(hours: Long) {
        transactionRepository.setExpiryHours(hours)
        _uiState.value = _uiState.value.copy(expiryHours = hours)
        viewModelScope.launch {
            transactionRepository.recalculateExpiry()
            transactionRepository.purgeOldExpired()
        }
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
            val results = withContext(Dispatchers.IO) {
                scanInboxUseCase.scanSenders(context.contentResolver, limit = 50)
            }
            val senders = results.map { r ->
                ScannedSender(
                    address = r.senderAddress,
                    displayName = r.senderAddress,
                    messageCount = r.totalMessages,
                    sampleBody = "${r.receivedCount} received, ${r.sentCount} sent, ${r.nonTxCount} other"
                )
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
        val sources = smsSourceRepository.getSources().filter { it.enabled }
        if (sources.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                historicalScanResult = "No senders configured. Add senders first under Monitored Senders."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            scanningHistorical = true, historicalScanResult = null
        )

        viewModelScope.launch {
            val result = scanInboxUseCase.scanHistoricalTransactions(
                contentResolver = context.contentResolver,
                sources = sources
            ) { parsed ->
                transactionRepository.saveHistoricalTransaction(parsed)
            }

            val msg = buildString {
                append("Found ${result.found} past transaction(s). ")
                append("Skipped ${result.skipped} non-transaction messages.")
                if (result.heuristicCount > 0) {
                    append(" ${result.heuristicCount} extracted via heuristic (low confidence).")
                }
                if (result.found == 0) {
                    append(" No matching transactions found.")
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

    // ── Reprocess Existing ──────────────────────────────────────────────────

    fun reprocessExistingTransactions() {
        _uiState.value = _uiState.value.copy(reprocessing = true, reprocessResult = null)
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                transactionRepository.reprocessExistingReferences()
            }
            _uiState.value = _uiState.value.copy(
                reprocessing = false,
                reprocessResult = "Reprocessed $count transaction(s). Reference IDs updated."
            )
        }
    }

    fun clearReprocessResult() {
        _uiState.value = _uiState.value.copy(reprocessResult = null)
    }

    // ── Reconfigure ──────────────────────────────────────────────────────

    fun reconfigure() {
        relayClient.resetSetup()
        smsSourceRepository.clearAll()
        _uiState.value = SettingsUiState()
    }
}
