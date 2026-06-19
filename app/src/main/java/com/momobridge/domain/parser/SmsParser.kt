package com.momobridge.domain.parser

import com.momobridge.domain.model.ParsedTransaction
import com.momobridge.domain.model.ParsingRule

object SmsParser {

    fun parse(body: String, rule: ParsingRule, receivedAt: Long, sender: String = ""): ParsedTransaction? {
        val action = SmsClassifier.classify(body)
        if (action != SmsAction.RECEIVED) return null

        val ruleResult = tryParseWithRule(body, rule, receivedAt, sender)
        if (ruleResult != null) return ruleResult

        val heuristicResult = tryParseHeuristic(body, receivedAt, sender)
        return heuristicResult
    }

    fun parseHeuristic(body: String, receivedAt: Long, sender: String = ""): ParsedTransaction? {
        val action = SmsClassifier.classify(body)
        if (action != SmsAction.RECEIVED) return null
        return tryParseHeuristic(body, receivedAt, sender)
    }

    private fun tryParseWithRule(
        body: String,
        rule: ParsingRule,
        receivedAt: Long,
        sender: String
    ): ParsedTransaction? {
        val refMatch = Regex(rule.refPattern, RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.getOrNull(1) ?: return null

        val rawAmount = Regex(rule.amountPattern, RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.getOrNull(1) ?: return null

        val amount = rawAmount.replace(",", "").toDoubleOrNull() ?: return null

        val name = if (rule.senderNamePattern == "USE_HEURISTIC") {
            val extracted = FieldExtractor.extract(body)
            extracted.senderName
        } else {
            rule.senderNamePattern
                ?.let { Regex(it, RegexOption.IGNORE_CASE).find(body)?.groupValues?.getOrNull(1) }
                ?.trim()
                ?: FieldExtractor.extract(body).senderName
        }

        val phone = if (rule.senderPhonePattern == "USE_HEURISTIC") {
            val extracted = FieldExtractor.extract(body)
            extracted.senderPhone
        } else {
            rule.senderPhonePattern
                ?.let { Regex(it, RegexOption.IGNORE_CASE).find(body)?.groupValues?.getOrNull(1) }
        }

        val balance = rule.balancePattern
            ?.let { Regex(it, RegexOption.IGNORE_CASE).find(body)?.groupValues?.getOrNull(1) }
            ?.replace(",", "")?.toDoubleOrNull()

        return ParsedTransaction(
            reference = refMatch.uppercase().trim(),
            amount = amount,
            senderName = name,
            senderPhone = phone,
            balanceAfter = balance,
            network = sender,
            receivedAt = receivedAt,
            rawSms = body,
            confidence = 1.0,
            parsedBy = "rule"
        )
    }

    private fun tryParseHeuristic(
        body: String,
        receivedAt: Long,
        sender: String
    ): ParsedTransaction? {
        val extracted = FieldExtractor.extract(body)

        return ParsedTransaction(
            reference = extracted.reference?.uppercase()?.trim(),
            amount = extracted.amount,
            senderName = extracted.senderName,
            senderPhone = extracted.senderPhone,
            balanceAfter = extracted.balanceAfter,
            network = sender,
            receivedAt = receivedAt,
            rawSms = body,
            confidence = extracted.confidence,
            parsedBy = "heuristic"
        )
    }
}
