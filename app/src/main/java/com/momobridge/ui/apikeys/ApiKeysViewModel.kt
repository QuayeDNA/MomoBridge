package com.momobridge.ui.apikeys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momobridge.data.local.ApiKeyEntity
import com.momobridge.data.repository.ApiKeyRepository
import com.momobridge.service.RelayClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ApiKeyWithValue(
    val entity: ApiKeyEntity,
    val keyValue: String
)

data class ApiKeysUiState(
    val keys: List<ApiKeyWithValue> = emptyList(),
    val activeCount: Int = 0,
    val newKeyLabel: String = "",
    val showAddDialog: Boolean = false,
    val selectedKey: ApiKeyWithValue? = null,
    val showDetailSheet: Boolean = false,
    val editingLabel: Boolean = false,
    val editLabelText: String = "",
    val error: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class ApiKeysViewModel @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository,
    private val relayClient: RelayClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApiKeysUiState())
    val uiState: StateFlow<ApiKeysUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                apiKeyRepository.observeAll(),
                apiKeyRepository.observeActiveCount()
            ) { keys, activeCount ->
                val withValues = keys.map { entity ->
                    ApiKeyWithValue(
                        entity = entity,
                        keyValue = apiKeyRepository.getKeyValue(entity.id)
                    )
                }
                _uiState.value = _uiState.value.copy(
                    keys = withValues,
                    activeCount = activeCount,
                    isLoading = false
                )
            }.collect { }
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            newKeyLabel = "",
            error = null
        )
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun updateNewKeyLabel(label: String) {
        _uiState.value = _uiState.value.copy(newKeyLabel = label, error = null)
    }

    fun createKey() {
        val label = _uiState.value.newKeyLabel.trim()
        if (label.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter a label")
            return
        }
        viewModelScope.launch {
            apiKeyRepository.createKey(label)
            _uiState.value = _uiState.value.copy(
                showAddDialog = false,
                newKeyLabel = "",
                error = null
            )
        }
    }

    fun showDetail(key: ApiKeyWithValue) {
        _uiState.value = _uiState.value.copy(
            selectedKey = key,
            showDetailSheet = true,
            editingLabel = false,
            editLabelText = key.entity.label
        )
    }

    fun hideDetail() {
        _uiState.value = _uiState.value.copy(
            showDetailSheet = false,
            selectedKey = null,
            editingLabel = false
        )
    }

    fun startEditingLabel() {
        val key = _uiState.value.selectedKey ?: return
        _uiState.value = _uiState.value.copy(
            editingLabel = true,
            editLabelText = key.entity.label
        )
    }

    fun updateEditLabelText(text: String) {
        _uiState.value = _uiState.value.copy(editLabelText = text)
    }

    fun saveLabel() {
        val key = _uiState.value.selectedKey ?: return
        val label = _uiState.value.editLabelText.trim()
        if (label.isBlank()) return
        viewModelScope.launch {
            apiKeyRepository.updateLabel(key.entity.id, label)
            _uiState.value = _uiState.value.copy(editingLabel = false)
        }
    }

    fun revokeKey() {
        val key = _uiState.value.selectedKey ?: return
        viewModelScope.launch {
            apiKeyRepository.revokeKey(key.entity.id)
            relayClient.revokeKeyOnRelay(key.keyValue)
            _uiState.value = _uiState.value.copy(showDetailSheet = false, selectedKey = null)
        }
    }

    fun reactivateKey() {
        val key = _uiState.value.selectedKey ?: return
        viewModelScope.launch {
            apiKeyRepository.reactivateKey(key.entity.id)
            // Reconnect to register the key with relay
            relayClient.disconnect()
            relayClient.connect()
            _uiState.value = _uiState.value.copy(showDetailSheet = false, selectedKey = null)
        }
    }

    fun deleteKey() {
        val key = _uiState.value.selectedKey ?: return
        viewModelScope.launch {
            apiKeyRepository.permanentlyDelete(key.entity.id)
            _uiState.value = _uiState.value.copy(showDetailSheet = false, selectedKey = null)
        }
    }
}
