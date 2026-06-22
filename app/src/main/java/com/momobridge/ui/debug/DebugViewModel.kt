package com.momobridge.ui.debug

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momobridge.data.local.SmsTransactionEntity
import com.momobridge.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TransactionRepository
) : ViewModel() {

    sealed class DumpState {
        data object Idle : DumpState()
        data object Dumping : DumpState()
        data class Done(val filePath: String, val count: Int) : DumpState()
        data class Error(val message: String) : DumpState()
    }

    private val _dumpState = MutableStateFlow<DumpState>(DumpState.Idle)
    val dumpState: StateFlow<DumpState> = _dumpState

    private val transactions = repository.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun dumpTransactions() {
        viewModelScope.launch {
            _dumpState.value = DumpState.Dumping
            try {
                val allTxn = transactions.value
                val arr = JSONArray()
                for (txn in allTxn) {
                    arr.put(txnToJson(txn))
                }

                val root = JSONObject().apply {
                    put("exported_count", allTxn.size)
                    put("exported_at", System.currentTimeMillis())
                    put("transactions", arr)
                }

                val file = File(context.cacheDir, "debug_dump.json")
                file.writeText(root.toString(2))

                _dumpState.value = DumpState.Done(
                    filePath = file.absolutePath,
                    count = allTxn.size
                )
            } catch (e: Exception) {
                _dumpState.value = DumpState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun txnToJson(txn: SmsTransactionEntity): JSONObject = JSONObject().apply {
        put("id", txn.id)
        put("network", txn.network)
        put("reference", txn.reference)
        put("amount", txn.amount)
        put("senderName", txn.senderName ?: JSONObject.NULL)
        put("senderPhone", txn.senderPhone ?: JSONObject.NULL)
        put("balanceAfter", txn.balanceAfter ?: JSONObject.NULL)
        put("rawSms", txn.rawSms)
        put("receivedAt", txn.receivedAt)
        put("createdAt", txn.createdAt)
        put("expiresAt", txn.expiresAt)
        put("confirmedAt", txn.confirmedAt ?: JSONObject.NULL)
        put("claimedByKeyLabel", txn.claimedByKeyLabel ?: JSONObject.NULL)
        put("status", txn.status)
    }
}
