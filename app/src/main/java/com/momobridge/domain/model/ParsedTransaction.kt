package com.momobridge.domain.model

data class ParsedTransaction(
    val network: String,
    val reference: String,
    val amount: Double,
    val senderName: String?,
    val senderPhone: String?,
    val balanceAfter: Double?,
    val receivedAt: Long,
    val rawSms: String,
    val confidence: Double = 1.0,
    val parsedBy: String = "rule"
)
