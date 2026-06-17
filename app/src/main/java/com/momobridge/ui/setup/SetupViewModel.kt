package com.momobridge.ui.setup

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Telephony
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momobridge.data.repository.ApiKeyRepository
import com.momobridge.data.repository.SmsSourceRepository
import com.momobridge.di.RegularPrefs
import com.momobridge.domain.model.SmsSource
import com.momobridge.domain.parser.AutoDetectUtils
import com.momobridge.domain.usecase.ScanInboxUseCase
import com.momobridge.service.RelayClient
import com.momobridge.service.RelayConnectionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class GeneratedKeyInfo(
    val id: Long,
    val label: String,
    val keyValue: String
)

data class ScannedSenderInfo(
    val address: String,
    val messageCount: Int
)

data class SetupUiState(
    val step: Int = 0,
    val ownerName: String = "",
    val generatedKeys: List<GeneratedKeyInfo> = emptyList(),
    val newKeyLabel: String = "",
    val relayUrl: String = "",
    val isComplete: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val scannedSenders: List<ScannedSenderInfo> = emptyList(),
    val scanningInbox: Boolean = false,
    val selectedSenders: Set<String> = emptySet(),
    val existingSources: List<SmsSource> = emptyList(),
    val isConnecting: Boolean = false,
    val connectionError: String? = null
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @RegularPrefs private val prefs: SharedPreferences,
    private val smsSourceRepository: SmsSourceRepository,
    private val relayClient: RelayClient,
    private val apiKeyRepository: ApiKeyRepository,
    private val scanInboxUseCase: ScanInboxUseCase
) : ViewModel() {

    val connectionState = relayClient.connectionState

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        if (relayClient.isSetupDone()) {
            _uiState.value = SetupUiState(isComplete = true)
        } else {
            val savedUrl = relayClient.getRelayUrl()
            _uiState.value = _uiState.value.copy(
                relayUrl = savedUrl.ifBlank { RelayClient.DEFAULT_RELAY_URL }
            )
        }
    }

    fun updateOwnerName(name: String) {
        _uiState.value = _uiState.value.copy(ownerName = name, error = null)
    }

    fun updateNewKeyLabel(label: String) {
        _uiState.value = _uiState.value.copy(newKeyLabel = label, error = null)
    }

    fun updateRelayUrl(url: String) {
        _uiState.value = _uiState.value.copy(relayUrl = url, connectionError = null, error = null)
    }

    // ── Step Navigation ─────────────────────────────────────────────────

    fun goToNextStep() {
        val state = _uiState.value
        when (state.step) {
            0 -> {
                if (state.ownerName.isBlank()) {
                    _uiState.value = state.copy(error = "Please enter your name")
                    return
                }
                prefs.edit().putString("owner_name", state.ownerName.trim()).apply()
                _uiState.value = state.copy(step = 1, error = null)
            }
            1 -> {
                if (state.generatedKeys.isEmpty()) {
                    _uiState.value = state.copy(error = "Add at least one store")
                    return
                }
                // Save primary key for relay connection
                relayClient.saveApiKey(state.generatedKeys.first().keyValue)
                _uiState.value = state.copy(step = 2, error = null)
            }
            2 -> {
                if (state.relayUrl.isBlank()) {
                    _uiState.value = state.copy(error = "Relay URL is required")
                    return
                }
                _uiState.value = state.copy(step = 3, error = null)
            }
            3 -> finishSetup()
        }
    }

    fun goBack() {
        val prevStep = _uiState.value.step - 1
        _uiState.value = _uiState.value.copy(step = prevStep.coerceAtLeast(0), error = null)
    }

    // ── API Key Generation ──────────────────────────────────────────────

    fun generateKey() {
        val state = _uiState.value
        if (state.newKeyLabel.isBlank()) {
            _uiState.value = state.copy(error = "Enter a label for this store")
            return
        }
        viewModelScope.launch {
            val entity = apiKeyRepository.createKey(state.newKeyLabel.trim())
            val keyValue = apiKeyRepository.getKeyValue(entity.id)
            val info = GeneratedKeyInfo(entity.id, entity.label, keyValue)
            _uiState.value = _uiState.value.copy(
                generatedKeys = _uiState.value.generatedKeys + info,
                newKeyLabel = "",
                error = null
            )
        }
    }

    // ── Relay Connection ────────────────────────────────────────────────

    fun connectToRelay() {
        val state = _uiState.value
        if (state.relayUrl.isBlank()) {
            _uiState.value = state.copy(connectionError = "Relay URL is required")
            return
        }
        if (state.generatedKeys.isEmpty()) {
            _uiState.value = state.copy(connectionError = "No API keys available")
            return
        }

        _uiState.value = state.copy(isConnecting = true, connectionError = null)

        relayClient.saveRelayUrl(state.relayUrl.trimEnd('/'))
        relayClient.connect()
    }

    fun checkAndProceedFromRelay() {
        checkConnectionAndProceed()
    }

    private fun checkConnectionAndProceed() {
        viewModelScope.launch {
            var attempts = 0
            while (attempts < 15) {
                val state = relayClient.connectionState.value
                when (state.status) {
                    RelayConnectionStatus.CONNECTED -> {
                        _uiState.value = _uiState.value.copy(
                            isConnecting = false,
                            error = null,
                            step = 3,
                            existingSources = smsSourceRepository.getSources()
                        )
                        scanInbox()
                        return@launch
                    }
                    RelayConnectionStatus.DISCONNECTED -> { }
                    else -> { }
                }
                kotlinx.coroutines.delay(1000)
                attempts++
            }
            relayClient.disconnect()
            _uiState.value = _uiState.value.copy(
                isConnecting = false,
                connectionError = "Could not connect to relay. Check the URL and try again."
            )
        }
    }

    // ── Sender Configuration ────────────────────────────────────────────

    fun toggleSender(address: String) {
        val current = _uiState.value.selectedSenders.toMutableSet()
        if (current.contains(address)) current.remove(address) else current.add(address)
        _uiState.value = _uiState.value.copy(selectedSenders = current)
    }

    fun finishSetup() {
        val state = _uiState.value
        for (addr in state.selectedSenders) {
            val exists = state.existingSources.any {
                it.senderAddress.equals(addr, ignoreCase = true)
            }
            if (!exists) {
                val source = SmsSource(
                    senderAddress = addr,
                    label = addr,
                    enabled = true,
                    parsingRule = null
                )
                smsSourceRepository.addSource(source)
            }
        }
        prefs.edit().putBoolean("historical_scan_done", false).apply()
        relayClient.markSetupDone()
        viewModelScope.launch {
            autoConfigureRules(state.selectedSenders.toList())
        }
        _uiState.value = _uiState.value.copy(isComplete = true)
    }

    private suspend fun autoConfigureRules(senderAddresses: List<String>) {
        withContext(Dispatchers.IO) {
            for (addr in senderAddresses) {
                try {
                    val uri = Uri.parse("content://sms/inbox")
                    val projection = arrayOf(
                        Telephony.TextBasedSmsColumns.BODY,
                        Telephony.TextBasedSmsColumns.DATE
                    )
                    val selection = "${Telephony.TextBasedSmsColumns.ADDRESS} = ?"
                    val selectionArgs = arrayOf(addr)
                    val cursor = context.contentResolver.query(
                        uri, projection, selection, selectionArgs,
                        "${Telephony.TextBasedSmsColumns.DATE} DESC"
                    )
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val body = it.getString(0) ?: return@use
                            val fields = AutoDetectUtils.detectFields(body)
                            if (fields.ref.isNotBlank() && fields.amount.isNotBlank()) {
                                val rule = AutoDetectUtils.buildRule(body, fields)
                                val source = SmsSource(
                                    senderAddress = addr,
                                    label = addr,
                                    enabled = true,
                                    parsingRule = rule
                                )
                                smsSourceRepository.addSource(source)
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
        }
    }

    private fun scanInbox() {
        _uiState.value = _uiState.value.copy(scanningInbox = true)
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                scanInboxUseCase.scanSenders(context.contentResolver, limit = 20)
            }
            val senders = results
                .filter { it.totalMessages >= 2 }
                .map { ScannedSenderInfo(it.senderAddress, it.totalMessages) }
            _uiState.value = _uiState.value.copy(
                scannedSenders = senders,
                scanningInbox = false
            )
        }
    }
}
