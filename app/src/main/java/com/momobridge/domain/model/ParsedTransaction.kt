package com.momobridge.domain.model

data class ParsedTransaction(
    val network: String,
    val reference: String,
    val amount: Double,
    val senderName: String?,
    val senderPhone: String?,
    val balanceAfter: Double?,
    val receivedAt: Long,
    val rawSms: String
)
