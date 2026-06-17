package com.momobridge.domain.parser

import com.momobridge.domain.model.ParsedTransaction
import com.momobridge.domain.model.ParsingRule

object SmsParser {

    private const val HEURISTIC_CONFIDENCE_THRESHOLD = 0.6

    fun parse(body: String, rule: ParsingRule, receivedAt: Long, sender: String = ""): ParsedTransaction? {
        val action = SmsClassifier.classify(body)
        if (action != SmsAction.RECEIVED) return null

        val ruleResult = tryParseWithRule(body, rule, receivedAt, sender)
        if (ruleResult != null) return ruleResult

        val heuristicResult = tryParseHeuristic(body, receivedAt, sender)
        if (heuristicResult != null) return heuristicResult

        return null
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
        if (extracted.confidence < HEURISTIC_CONFIDENCE_THRESHOLD) return null

        val ref = extracted.reference ?: return null
        val amount = extracted.amount ?: return null

        return ParsedTransaction(
            reference = ref.uppercase().trim(),
            amount = amount,
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
