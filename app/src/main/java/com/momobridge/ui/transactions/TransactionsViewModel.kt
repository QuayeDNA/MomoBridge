package com.momobridge.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momobridge.data.local.SmsTransactionEntity
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
    repository: TransactionRepository
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
    }

    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter: StateFlow<String?> = _statusFilter

    private val _networkFilter = MutableStateFlow<String?>(null)
    val networkFilter: StateFlow<String?> = _networkFilter

    private val _keyLabelFilter = MutableStateFlow<String?>(null)
    val keyLabelFilter: StateFlow<String?> = _keyLabelFilter

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage

    val keyLabels: StateFlow<List<String>> = repository.observeDistinctKeyLabels()
        .map { labels -> labels.filterNotNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allTransactions: StateFlow<List<SmsTransactionEntity>> = repository.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = repository.observeTotalCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val distinctNetworks: StateFlow<List<String>> = allTransactions.map { txns ->
        txns.map { it.network }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<SmsTransactionEntity>> = combine(
        allTransactions, _statusFilter, _networkFilter, _keyLabelFilter, _currentPage
    ) { all, statusF, netF, keyF, page ->
        var filtered = all
        if (statusF != null) filtered = filtered.filter { it.status == statusF }
        if (netF != null) filtered = filtered.filter { it.network == netF }
        if (keyF != null) filtered = filtered.filter { it.claimedByKeyLabel == keyF }
        filtered.take(PAGE_SIZE * page)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasMore: StateFlow<Boolean> = combine(
        allTransactions, _statusFilter, _networkFilter, _keyLabelFilter, _currentPage
    ) { all, statusF, netF, keyF, page ->
        var filtered = all
        if (statusF != null) filtered = filtered.filter { it.status == statusF }
        if (netF != null) filtered = filtered.filter { it.network == netF }
        if (keyF != null) filtered = filtered.filter { it.claimedByKeyLabel == keyF }
        filtered.size > PAGE_SIZE * page
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val pendingCount: StateFlow<Int> = repository.observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val confirmedCount: StateFlow<Int> = repository.observeConfirmedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val failedCount: StateFlow<Int> = repository.observeFailedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val expiredCount: StateFlow<Int> = repository.observeExpiredCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _selectedTransaction = MutableStateFlow<SmsTransactionEntity?>(null)
    val selectedTransaction: StateFlow<SmsTransactionEntity?> = _selectedTransaction

    fun selectTransaction(txn: SmsTransactionEntity) {
        _selectedTransaction.value = txn
    }

    fun dismissTransaction() {
        _selectedTransaction.value = null
    }

    fun loadMore() {
        _currentPage.value = _currentPage.value + 1
    }

    fun setFilter(status: String?) {
        _statusFilter.value = if (_statusFilter.value == status) null else status
        _currentPage.value = 1
    }

    fun clearFilter() {
        _statusFilter.value = null
        _networkFilter.value = null
        _currentPage.value = 1
    }

    fun setNetworkFilter(network: String) {
        _networkFilter.value = if (_networkFilter.value == network) null else network
        _currentPage.value = 1
    }

    fun clearNetworkFilter() {
        _networkFilter.value = null
        _currentPage.value = 1
    }

    fun setKeyLabelFilter(label: String?) {
        _keyLabelFilter.value = if (_keyLabelFilter.value == label) null else label
        _currentPage.value = 1
    }

    fun clearKeyLabelFilter() {
        _keyLabelFilter.value = null
        _currentPage.value = 1
    }
}
