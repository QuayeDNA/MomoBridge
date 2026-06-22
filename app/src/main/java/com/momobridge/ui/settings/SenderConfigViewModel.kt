package com.momobridge.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momobridge.data.repository.SmsSourceRepository
import com.momobridge.domain.model.SmsSource
import com.momobridge.domain.parser.AutoDetectUtils
import com.momobridge.domain.parser.DetectedFields
import com.momobridge.domain.parser.FieldExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SenderConfigUiState(
    val senderAddress: String = "",
    val label: String = "",
    val messages: List<InboxMessage> = emptyList(),
    val selectedMessage: InboxMessage? = null,
    val step: ConfigStep = ConfigStep.PICK_MESSAGE,
    val detectedRef: String = "",
    val detectedAmount: String = "",
    val detectedSenderName: String = "",
    val detectedSenderPhone: String = "",
    val detectedBalance: String = "",
    val detectedKeyword: String = "received",
    val manualRef: String = "",
    val manualAmount: String = "",
    val manualSenderName: String = "",
    val manualSenderPhone: String = "",
    val manualBalance: String = "",
    val editingRef: Boolean = false,
    val editingAmount: Boolean = false,
    val editingSenderName: Boolean = false,
    val editingSenderPhone: Boolean = false,
    val editingBalance: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

data class InboxMessage(
    val body: String,
    val timestamp: Long,
    val displayTime: String,
    val reference: String?,
    val amount: Double?,
    val preview: String
)

enum class ConfigStep {
    PICK_MESSAGE,
    CONFIRM_FIELDS
}

@HiltViewModel
class SenderConfigViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsSourceRepository: SmsSourceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SenderConfigUiState())
    val uiState: StateFlow<SenderConfigUiState> = _uiState.asStateFlow()

    fun initForSender(senderAddress: String, label: String, prefilledBody: String = "") {
        _uiState.value = _uiState.value.copy(senderAddress = senderAddress, label = label)
        scanInbox(senderAddress, prefilledBody)
    }

    private fun scanInbox(senderAddress: String, prefilledBody: String = "") {
        viewModelScope.launch {
            val messages = withContext(Dispatchers.IO) {
                val inboxMessages = mutableListOf<InboxMessage>()
                try {
                    val uri = Uri.parse("content://sms/inbox")
                    val projection = arrayOf(
                        Telephony.TextBasedSmsColumns.BODY,
                        Telephony.TextBasedSmsColumns.DATE
                    )
                    val selection = "${Telephony.TextBasedSmsColumns.ADDRESS} = ?"
                    val selectionArgs = arrayOf(senderAddress)
                    val cursor = context.contentResolver.query(
                        uri, projection, selection, selectionArgs,
                        "${Telephony.TextBasedSmsColumns.DATE} DESC LIMIT 50"
                    )
                    val df = SimpleDateFormat("dd MMM HH:mm", Locale.US)
                    cursor?.use {
                        while (it.moveToNext()) {
                            val body = it.getString(0) ?: continue
                            val timestamp = it.getLong(1)
                            val extracted = FieldExtractor.extract(body)
                            inboxMessages.add(
                                InboxMessage(
                                    body = body,
                                    timestamp = timestamp,
                                    displayTime = df.format(Date(timestamp)),
                                    reference = extracted.reference,
                                    amount = extracted.amount,
                                    preview = body.take(100).replace("\n", " ")
                                )
                            )
                        }
                    }
                } catch (_: Exception) { }
                inboxMessages
            }
            _uiState.value = _uiState.value.copy(messages = messages)

            if (prefilledBody.isNotBlank()) {
                val match = messages.firstOrNull { it.body == prefilledBody }
                if (match != null) {
                    selectMessage(match)
                }
            }
        }
    }

    fun selectMessage(msg: InboxMessage) {
        val fields = AutoDetectUtils.detectFields(msg.body)
        _uiState.value = _uiState.value.copy(
            selectedMessage = msg,
            step = ConfigStep.CONFIRM_FIELDS,
            detectedRef = fields.ref,
            detectedAmount = fields.amount,
            detectedSenderName = fields.senderName,
            detectedSenderPhone = fields.senderPhone,
            detectedBalance = fields.balance,
            detectedKeyword = fields.keyword,
            manualRef = fields.ref,
            manualAmount = fields.amount,
            manualSenderName = fields.senderName,
            manualSenderPhone = fields.senderPhone,
            manualBalance = fields.balance,
            editingRef = false,
            editingAmount = false,
            editingSenderName = false,
            editingSenderPhone = false,
            editingBalance = false,
            error = null
        )
    }

    fun goBackToPickMessage() {
        _uiState.value = _uiState.value.copy(
            step = ConfigStep.PICK_MESSAGE,
            selectedMessage = null,
            error = null
        )
    }

    fun saveRule() {
        val s = _uiState.value
        val body = s.selectedMessage?.body ?: return

        val ref = s.manualRef.ifBlank { s.detectedRef }
        val amount = s.manualAmount.ifBlank { s.detectedAmount }
        val senderName = s.manualSenderName.ifBlank { s.detectedSenderName }
        val senderPhone = s.manualSenderPhone.ifBlank { s.detectedSenderPhone }
        val balance = s.manualBalance.ifBlank { s.detectedBalance }

        if (ref.isBlank() || amount.isBlank()) {
            _uiState.value = s.copy(error = "Reference and amount are required")
            return
        }

        val fields = DetectedFields(
            ref = ref,
            amount = amount,
            senderName = senderName,
            senderPhone = senderPhone,
            balance = balance,
            keyword = s.detectedKeyword,
            actionVerbs = listOf(s.detectedKeyword)
        )

        _uiState.value = s.copy(isSaving = true, error = null)

        val rule = AutoDetectUtils.buildRule(body, fields)
        val source = SmsSource(
            senderAddress = s.senderAddress,
            label = s.label.ifBlank { s.senderAddress },
            enabled = true,
            parsingRule = rule,
            trainingMessages = listOf(body)
        )
        smsSourceRepository.addSource(source)
        _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
    }

    fun toggleEditRef() { toggle { it.copy(editingRef = !it.editingRef) } }
    fun toggleEditAmount() { toggle { it.copy(editingAmount = !it.editingAmount) } }
    fun toggleEditSenderName() { toggle { it.copy(editingSenderName = !it.editingSenderName) } }
    fun toggleEditSenderPhone() { toggle { it.copy(editingSenderPhone = !it.editingSenderPhone) } }
    fun toggleEditBalance() { toggle { it.copy(editingBalance = !it.editingBalance) } }

    private fun toggle(transform: (SenderConfigUiState) -> SenderConfigUiState) {
        _uiState.value = transform(_uiState.value)
    }

    fun updateManualRef(v: String) { _uiState.value = _uiState.value.copy(manualRef = v) }
    fun updateManualAmount(v: String) { _uiState.value = _uiState.value.copy(manualAmount = v) }
    fun updateManualSenderName(v: String) { _uiState.value = _uiState.value.copy(manualSenderName = v) }
    fun updateManualSenderPhone(v: String) { _uiState.value = _uiState.value.copy(manualSenderPhone = v) }
    fun updateManualBalance(v: String) { _uiState.value = _uiState.value.copy(manualBalance = v) }
    fun updateKeyword(v: String) { _uiState.value = _uiState.value.copy(detectedKeyword = v) }
}
