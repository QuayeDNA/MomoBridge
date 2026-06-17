package com.momobridge.domain.parser

import com.momobridge.domain.model.ParsedTransaction
import com.momobridge.domain.model.ParsingRule

object SmsParser {

    fun parse(body: String, rule: ParsingRule, receivedAt: Long, sender: String = ""): ParsedTransaction? {
        if (!body.contains(rule.isCreditKeyword, ignoreCase = true)) return null

        val refMatch = Regex(rule.refPattern, RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.getOrNull(1) ?: return null
        val rawAmount = Regex(rule.amountPattern, RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.getOrNull(1) ?: return null
        val amount = rawAmount.replace(",", "").toDoubleOrNull() ?: return null

        return ParsedTransaction(
            reference = refMatch.uppercase().trim(),
            amount = amount,
            senderName = rule.senderNamePattern
                ?.let { Regex(it, RegexOption.IGNORE_CASE).find(body)?.groupValues?.getOrNull(1) }
                ?.trim(),
            senderPhone = rule.senderPhonePattern
                ?.let { Regex(it, RegexOption.IGNORE_CASE).find(body)?.groupValues?.getOrNull(1) },
            balanceAfter = rule.balancePattern
                ?.let { Regex(it, RegexOption.IGNORE_CASE).find(body)?.groupValues?.getOrNull(1) }
                ?.replace(",", "")?.toDoubleOrNull(),
            network = sender,
            receivedAt = receivedAt,
            rawSms = body
        )
    }
}
