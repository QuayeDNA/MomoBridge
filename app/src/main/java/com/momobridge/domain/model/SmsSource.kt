package com.momobridge.domain.model

import java.util.UUID

data class SmsSource(
    val id: String = UUID.randomUUID().toString(),
    val senderAddress: String,
    val label: String,
    val enabled: Boolean = true,
    val parsingRule: ParsingRule? = null
)

data class ParsingRule(
    val refPattern: String,
    val amountPattern: String,
    val senderPhonePattern: String? = null,
    val senderNamePattern: String? = null,
    val balancePattern: String? = null,
    val isCreditKeyword: String = "received",
    val exampleMessage: String = ""
)
