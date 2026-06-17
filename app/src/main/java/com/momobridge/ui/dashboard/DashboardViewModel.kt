package com.momobridge.ui.dashboard

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Telephony
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momobridge.data.local.SmsTransactionEntity
import com.momobridge.data.repository.SmsSourceRepository
import com.momobridge.data.repository.TransactionRepository
import com.momobridge.di.RegularPrefs
import com.momobridge.domain.parser.SmsParser
import com.momobridge.service.RelayClient
import com.momobridge.service.SmsListenerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val relayClient: RelayClient
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

    private val _scanningHistorical = MutableStateFlow(false)
    val scanningHistorical: StateFlow<Boolean> = _scanningHistorical

    init {
        autoScanIfNeeded()
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
        val sources = smsSourceRepository.getSources().filter { it.enabled && it.parsingRule != null }
        if (sources.isEmpty()) return

        _scanningHistorical.value = true

        withContext(Dispatchers.IO) {
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
                            val saved = repository.saveHistoricalTransaction(parsed)
                            if (saved) found++
                        }
                    }
                } catch (_: Exception) { }
            }

            _scanningHistorical.value = false
            prefs.edit().putBoolean("historical_scan_done", true).apply()
        }
    }
}
