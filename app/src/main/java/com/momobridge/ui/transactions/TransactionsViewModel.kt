package com.momobridge.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momobridge.data.local.ApiKeyEntity
import com.momobridge.data.local.SmsTransactionEntity
import com.momobridge.data.repository.ApiKeyRepository
import com.momobridge.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    repository: TransactionRepository,
    apiKeyRepository: ApiKeyRepository
) : ViewModel() {

    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter: StateFlow<String?> = _statusFilter

    private val _keyLabelFilter = MutableStateFlow<String?>(null)
    val keyLabelFilter: StateFlow<String?> = _keyLabelFilter

    val keyLabels: StateFlow<List<String>> = apiKeyRepository.observeActive()
        .map { keys -> keys.map { it.label } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allTransactions: StateFlow<List<SmsTransactionEntity>> = repository.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<SmsTransactionEntity>> = combine(
        allTransactions, _statusFilter, _keyLabelFilter
    ) { all, statusF, keyF ->
        var filtered = all
        if (statusF != null) filtered = filtered.filter { it.status == statusF }
        if (keyF != null) filtered = filtered.filter { it.claimedByKeyLabel == keyF }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount: StateFlow<Int> = repository.observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val confirmedCount: StateFlow<Int> = repository.observeConfirmedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val failedCount: StateFlow<Int> = repository.observeFailedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val expiredCount: StateFlow<Int> = repository.observeExpiredCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setFilter(status: String?) {
        _statusFilter.value = if (_statusFilter.value == status) null else status
    }

    fun clearFilter() {
        _statusFilter.value = null
    }

    fun setKeyLabelFilter(label: String?) {
        _keyLabelFilter.value = if (_keyLabelFilter.value == label) null else label
    }

    fun clearKeyLabelFilter() {
        _keyLabelFilter.value = null
    }
}
