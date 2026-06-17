package com.momobridge.ui.dashboard

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momobridge.data.local.SmsTransactionEntity
import com.momobridge.data.repository.SmsSourceRepository
import com.momobridge.data.repository.TransactionRepository
import com.momobridge.di.RegularPrefs
import com.momobridge.domain.usecase.ScanInboxUseCase
import com.momobridge.service.RelayClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.Dispatchers
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
    private val scanInboxUseCase: ScanInboxUseCase
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

    init {
        autoScanIfNeeded()
        viewModelScope.launch {
            repository.observeTransactions().collect {
                if (_isLoading.value) _isLoading.value = false
            }
        }
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
}
