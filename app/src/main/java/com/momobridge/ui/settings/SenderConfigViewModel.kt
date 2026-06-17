package com.momobridge.ui.settings

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momobridge.data.repository.SmsSourceRepository
import com.momobridge.domain.model.ParsingRule
import com.momobridge.domain.model.SenderType
import com.momobridge.domain.model.SmsSource
import com.momobridge.domain.parser.AutoDetectUtils
import com.momobridge.domain.parser.DetectedFields
import com.momobridge.domain.parser.SmsParser
import com.momobridge.domain.usecase.RuleValidationResult
import com.momobridge.domain.usecase.ValidateParsingRuleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SenderConfigUiState(
    val senderAddress: String = "",
    val label: String = "",
    val senderType: SenderType = SenderType.INCOMING,
    val messages: List<InboxMessage> = emptyList(),
    val validationMessages: List<InboxMessage> = emptyList(),
    val selectedMessage: InboxMessage? = null,
    val selectedValidationMessage: InboxMessage? = null,
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
    val manualKeyword: String = "received",
    val editingRef: Boolean = false,
    val editingAmount: Boolean = false,
    val editingSenderName: Boolean = false,
    val editingSenderPhone: Boolean = false,
    val editingBalance: Boolean = false,
    val editingKeyword: Boolean = false,
    val step: ConfigStep = ConfigStep.PICK_MESSAGE,
    val saved: Boolean = false,
    val error: String? = null,
    val validationResult: RuleValidationResult? = null
)

data class InboxMessage(
    val body: String,
    val timestamp: Long,
    val displayTime: String
)

enum class ConfigStep {
    PICK_MESSAGE,
    CONFIRM_FIELDS,
    VALIDATE,
    DONE
}

@HiltViewModel
class SenderConfigViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsSourceRepository: SmsSourceRepository,
    private val validateParsingRuleUseCase: ValidateParsingRuleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SenderConfigUiState())
    val uiState: StateFlow<SenderConfigUiState> = _uiState.asStateFlow()

    private var currentRule: ParsingRule? = null

    fun initForSender(senderAddress: String, label: String) {
        _uiState.value = _uiState.value.copy(senderAddress = senderAddress, label = label)
        scanInbox(senderAddress)
    }

    private fun scanInbox(senderAddress: String) {
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
                        "${Telephony.TextBasedSmsColumns.DATE} DESC LIMIT 20"
                    )
                    cursor?.use {
                        while (it.moveToNext()) {
                            val body = it.getString(0) ?: continue
                            val timestamp = it.getLong(1)
                            val df = java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.US)
                            inboxMessages.add(InboxMessage(body, timestamp, df.format(java.util.Date(timestamp))))
                        }
                    }
                } catch (_: Exception) { }
                inboxMessages
            }
            _uiState.value = _uiState.value.copy(
                messages = messages,
                validationMessages = messages.drop(1).take(5)
            )
        }
    }

    fun selectMessage(msg: InboxMessage) {
        _uiState.value = _uiState.value.copy(selectedMessage = msg, step = ConfigStep.CONFIRM_FIELDS)
        val fields = AutoDetectUtils.detectFields(msg.body)
        _uiState.value = _uiState.value.copy(
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
            manualKeyword = fields.keyword,
            editingRef = false,
            editingAmount = false,
            editingSenderName = false,
            editingSenderPhone = false,
            editingBalance = false,
            editingKeyword = false,
            error = null
        )
    }

    fun goToValidation() {
        val s = _uiState.value
        val body = s.selectedMessage?.body ?: return
        val keyword = s.manualKeyword.ifBlank { s.detectedKeyword }
        val fields = DetectedFields(
            ref = s.manualRef.ifBlank { s.detectedRef },
            amount = s.manualAmount.ifBlank { s.detectedAmount },
            senderName = s.manualSenderName.ifBlank { s.detectedSenderName },
            senderPhone = s.manualSenderPhone.ifBlank { s.detectedSenderPhone },
            balance = s.manualBalance.ifBlank { s.detectedBalance },
            keyword = keyword,
            actionVerbs = listOf(keyword)
        )

        if (fields.ref.isBlank() || fields.amount.isBlank()) {
            _uiState.value = s.copy(error = "Reference and amount are required")
            return
        }

        currentRule = AutoDetectUtils.buildRule(body, fields)
        _uiState.value = s.copy(
            step = ConfigStep.VALIDATE,
            error = null,
            validationResult = null,
            selectedValidationMessage = null
        )
    }

    fun selectValidationMessage(msg: InboxMessage) {
        val rule = currentRule ?: return
        val result = validateParsingRuleUseCase.validate(
            rule = rule,
            sampleMessages = listOf(msg.body),
            sender = _uiState.value.senderAddress
        )
        _uiState.value = _uiState.value.copy(
            selectedValidationMessage = msg,
            validationResult = result
        )
    }

    fun saveAfterValidation() {
        val s = _uiState.value
        val rule = currentRule ?: return
        val trainingBodies = mutableListOf<String>()
        s.selectedMessage?.let { trainingBodies.add(it.body) }
        s.selectedValidationMessage?.let { if (it.body != s.selectedMessage?.body) trainingBodies.add(it.body) }

        val source = SmsSource(
            senderAddress = s.senderAddress,
            label = s.label.ifBlank { s.senderAddress },
            type = s.senderType,
            enabled = true,
            parsingRule = rule,
            trainingMessages = trainingBodies
        )

        smsSourceRepository.addSource(source)
        _uiState.value = s.copy(step = ConfigStep.DONE, saved = true, error = null)
    }

    fun skipValidationAndSave() {
        saveAfterValidation()
    }

    fun goBackToEdit() {
        _uiState.value = _uiState.value.copy(
            step = ConfigStep.CONFIRM_FIELDS,
            validationResult = null,
            selectedValidationMessage = null
        )
    }

    fun toggleEditRef() { toggle { it.copy(editingRef = !it.editingRef) } }
    fun toggleEditAmount() { toggle { it.copy(editingAmount = !it.editingAmount) } }
    fun toggleEditSenderName() { toggle { it.copy(editingSenderName = !it.editingSenderName) } }
    fun toggleEditSenderPhone() { toggle { it.copy(editingSenderPhone = !it.editingSenderPhone) } }
    fun toggleEditBalance() { toggle { it.copy(editingBalance = !it.editingBalance) } }
    fun toggleEditKeyword() { toggle { it.copy(editingKeyword = !it.editingKeyword) } }

    private fun toggle(transform: (SenderConfigUiState) -> SenderConfigUiState) {
        _uiState.value = transform(_uiState.value)
    }

    fun updateManualRef(v: String) { _uiState.value = _uiState.value.copy(manualRef = v) }
    fun updateManualAmount(v: String) { _uiState.value = _uiState.value.copy(manualAmount = v) }
    fun updateManualSenderName(v: String) { _uiState.value = _uiState.value.copy(manualSenderName = v) }
    fun updateManualSenderPhone(v: String) { _uiState.value = _uiState.value.copy(manualSenderPhone = v) }
    fun updateManualBalance(v: String) { _uiState.value = _uiState.value.copy(manualBalance = v) }
    fun updateManualKeyword(v: String) { _uiState.value = _uiState.value.copy(manualKeyword = v) }
}
