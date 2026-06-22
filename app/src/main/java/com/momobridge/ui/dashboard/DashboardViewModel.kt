package com.momobridge.ui.dashboard

import android.content.Context
import android.content.SharedPreferences
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momobridge.data.local.SmsTransactionEntity
import com.momobridge.data.repository.SmsSourceRepository
import com.momobridge.data.repository.TransactionRepository
import com.momobridge.di.RegularPrefs
import com.momobridge.domain.usecase.RecoverySmsUseCase
import com.momobridge.domain.usecase.ScanInboxUseCase
import com.momobridge.service.RelayClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @RegularPrefs private val prefs: SharedPreferences,
    private val repository: TransactionRepository,
    private val smsSourceRepository: SmsSourceRepository,
    private val relayClient: RelayClient,
    private val scanInboxUseCase: ScanInboxUseCase,
    private val recoverySmsUseCase: RecoverySmsUseCase
) : ViewModel() {

    val transactions: StateFlow<List<SmsTransactionEntity>> = repository.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = repository.observeTotalCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingCount: StateFlow<Int> = repository.observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val confirmedCount: StateFlow<Int> = repository.observeConfirmedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val failedCount: StateFlow<Int> = repository.observeFailedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val expiredCount: StateFlow<Int> = repository.observeExpiredCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val connectionState = relayClient.connectionState

    val ownerName: String = prefs.getString("owner_name", "") ?: ""

    private val _selectedTransaction = MutableStateFlow<SmsTransactionEntity?>(null)
    val selectedTransaction: StateFlow<SmsTransactionEntity?> = _selectedTransaction

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _scanningHistorical = MutableStateFlow(false)
    val scanningHistorical: StateFlow<Boolean> = _scanningHistorical

    private val _isListenerAlive = MutableStateFlow(true)
    val isListenerAlive: StateFlow<Boolean> = _isListenerAlive

    private val _showBatteryBanner = MutableStateFlow(false)
    val showBatteryBanner: StateFlow<Boolean> = _showBatteryBanner

    init {
        autoScanIfNeeded()
        viewModelScope.launch {
            repository.observeTransactions().collect {
                if (_isLoading.value) _isLoading.value = false
            }
        }
        runRecoveryScan()
        startListenerHealthChecker()
        checkBatteryOptimization()
    }

    fun dismissBatteryBanner() {
        _showBatteryBanner.value = false
        prefs.edit().putBoolean("battery_banner_dismissed", true).apply()
    }

    fun selectTransaction(txn: SmsTransactionEntity) {
        _selectedTransaction.value = txn
    }

    fun dismissTransaction() {
        _selectedTransaction.value = null
    }

    private fun autoScanIfNeeded() {
        if (prefs.getBoolean("historical_scan_done", false)) return
        val hasRules = smsSourceRepository.getSources().any { it.parsingRule != null && it.enabled }
        if (!hasRules) return
        viewModelScope.launch { scanHistoricalTransactions() }
    }

    private suspend fun scanHistoricalTransactions() {
        val sources = smsSourceRepository.getSources().filter { it.enabled }
        if (sources.isEmpty()) return

        _scanningHistorical.value = true

        val result = scanInboxUseCase.scanHistoricalTransactions(
            contentResolver = context.contentResolver,
            sources = sources
        ) { parsed ->
            repository.saveHistoricalTransaction(parsed)
        }

        if (result.found > 0 || sources.any { it.parsingRule != null }) {
            prefs.edit().putBoolean("historical_scan_done", true).apply()
        }

        _scanningHistorical.value = false
    }

    private fun runRecoveryScan() {
        val sources = smsSourceRepository.getSources().filter { it.enabled }
        if (sources.isEmpty()) return

        viewModelScope.launch {
            val saved = recoverySmsUseCase.recoverRecent(
                contentResolver = context.contentResolver,
                sources = sources,
                referenceExists = { ref -> repository.findByReference(ref) != null },
                saveTransaction = { parsed -> repository.saveHistoricalTransaction(parsed) }
            )
            if (saved > 0) {
                android.util.Log.d("DashboardVM", "Recovery scan saved $saved transactions")
            }
        }
    }

    private fun startListenerHealthChecker() {
        viewModelScope.launch {
            while (true) {
                delay(10_000L)
                val lastAlive = prefs.getLong("listener_last_alive", 0L)
                _isListenerAlive.value = System.currentTimeMillis() - lastAlive < 90_000L
            }
        }
    }

    private fun checkBatteryOptimization() {
        if (prefs.getBoolean("battery_banner_dismissed", false)) return
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        _showBatteryBanner.value = !pm.isIgnoringBatteryOptimizations(context.packageName)
    }
}
