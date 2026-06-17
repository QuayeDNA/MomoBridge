package com.momobridge.domain.model

import java.util.UUID

enum class SenderType {
    INCOMING,
    OUTGOING,
    BOTH
}

data class SmsSource(
    val id: String = UUID.randomUUID().toString(),
    val senderAddress: String,
    val label: String,
    val type: SenderType = SenderType.INCOMING,
    val enabled: Boolean = true,
    val parsingRule: ParsingRule? = null,
    val trainingMessages: List<String> = emptyList()
)

data class ParsingRule(
    val refPattern: String,
    val amountPattern: String,
    val senderPhonePattern: String? = null,
    val senderNamePattern: String? = null,
    val balancePattern: String? = null,
    val actionVerbs: List<String> = listOf("received"),
    val exampleMessage: String = ""
) {
    @Deprecated("Use actionVerbs instead", ReplaceWith("actionVerbs.firstOrNull() ?: \"received\""))
    val isCreditKeyword: String get() = actionVerbs.firstOrNull() ?: "received"
}
