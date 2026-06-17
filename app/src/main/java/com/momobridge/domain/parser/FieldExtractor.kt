package com.momobridge.domain.parser

import kotlin.math.min

data class ExtractedFields(
    val reference: String? = null,
    val amount: Double? = null,
    val senderName: String? = null,
    val senderPhone: String? = null,
    val balanceAfter: Double? = null,
    val keyword: String = "received",
    val confidence: Double = 0.0
)

object FieldExtractor {

    private val currencyPattern = Regex(
        "(?:GH[₵S]?|₵|GHS|¢|\\bGhc\\b|\\bGHc\\b|\\bGH¢\\b)\\s*([\\d,]+(?:\\.\\d{1,2})?)",
        RegexOption.IGNORE_CASE
    )
    private val bareAmountPattern = Regex("\\b([\\d,]+\\.\\d{2})\\b")

    private val ghanaPhonePattern = Regex(
        "\\b(0(?:2[0-9]|3[0-9]|5[0-9]|4[0-9]|5[0-9]|7[0-9]|9[0-9])\\d{7})\\b|\\b(233[0-9]{9})\\b|\\b(\\+233[0-9]{9})\\b"
    )

    private val refLabelPattern = Regex(
        "(?:Ref(?:erence)?[\\s:#]*|Transaction\\s*(?:ID|number|ref)[\\s:#]*)(\\w+)",
        RegexOption.IGNORE_CASE
    )
    private val digitSequencePattern = Regex("\\b(\\d{12,25})\\b")

    private val transferFromPattern = Regex(
        "Transfer\\s+From:\\s*(\\d{10,15})-([A-Za-z\\s./&'-]+?)(?=\\s+on\\s)",
        RegexOption.IGNORE_CASE
    )

    private val fromPhoneNamePattern = Regex(
        "(?:from|by)[\\s:]*(\\d{10,15})\\s*-\\s*([A-Za-z\\s./&'-]+?)(?=\\s+on\\s|\\s+\\.|\\s*$)",
        RegexOption.IGNORE_CASE
    )

    private val receivedFromPattern = Regex(
        "received\\s+(?:GH[₵S]?|₵|GHS)?\\s*[\\d,.]++\\s+from\\s+(.+?)(?:\\.\\s*(?:Your|Current|Telecel|New))",
        RegexOption.IGNORE_CASE
    )

    private val genericFromNamePattern = Regex(
        "(?:from|by)[\\s:]+([A-Za-z\\s.&/'-]+?)(?=\\s+(?:0\\d{9}|233\\d{9}|\\d{5,}|\\())",
        RegexOption.IGNORE_CASE
    )

    private val balancePattern = Regex(
        "(?:balance|your\\s+balance|new\\s+balance|current\\s+balance|Telecel\\s+Cash\\s+balance)" +
            "[\\s:]*.*?(?:GH[₵S]?|₵|GHS|¢)?\\s*([\\d,]+\\.?\\d*)",
        RegexOption.IGNORE_CASE
    )

    fun extract(body: String): ExtractedFields {
        val reference = extractReference(body)
        val amount = extractAmount(body)
        val phone = extractPhone(body)
        val name = extractSenderName(body)
        val balance = extractBalance(body)
        val keyword = detectKeyword(body)

        val confidence = computeConfidence(body, reference, amount, phone, name)

        return ExtractedFields(
            reference = reference,
            amount = amount,
            senderPhone = phone,
            senderName = name,
            balanceAfter = balance,
            keyword = keyword,
            confidence = confidence
        )
    }

    private fun extractReference(body: String): String? {
        val labeled = refLabelPattern.find(body)?.groupValues?.getOrNull(1)
        if (labeled != null) return labeled

        val veryLong = digitSequencePattern.find(body)?.groupValues?.getOrNull(1)
        if (veryLong != null) return veryLong

        return null
    }

    private fun extractAmount(body: String): Double? {
        val receivedAmount = Regex(
            "(?:received|credited)\\s+(?:GH[₵S]?|₵|GHS)?\\s*([\\d,]+\\.?\\d*)",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.getOrNull(1)

        if (receivedAmount != null) {
            val parsed = receivedAmount.replace(",", "").toDoubleOrNull()
            if (parsed != null && parsed > 0) return parsed
        }

        val currencyMatches = currencyPattern.findAll(body).toList()
        if (currencyMatches.isNotEmpty()) {
            val parsed = currencyMatches.first().groupValues[1].replace(",", "").toDoubleOrNull()
            if (parsed != null && parsed > 0) return parsed
        }

        return null
    }

    private fun extractPhone(body: String): String? {
        val tfPhone = transferFromPattern.find(body)?.groupValues?.getOrNull(1)
        if (tfPhone != null && isValidPhone(tfPhone)) return tfPhone

        val fromPhone = fromPhoneNamePattern.find(body)?.groupValues?.getOrNull(1)
        if (fromPhone != null && isValidPhone(fromPhone)) return fromPhone

        val sentToPhone = Regex("sent\\s+to\\s+\\s*(\\d{10,15})", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.getOrNull(1)
        if (sentToPhone != null && isValidPhone(sentToPhone)) return sentToPhone

        val paidToPhone = Regex("paid\\s+(?:GH[₵S]?|₵|GHS)?\\s*[\\d,.]+\\s+to\\s+(\\d{10,15})", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.getOrNull(1)
        if (paidToPhone != null && isValidPhone(paidToPhone)) return paidToPhone

        val genericPhone = ghanaPhonePattern.find(body)?.value
        if (genericPhone != null) return genericPhone

        return null
    }

    private fun isValidPhone(phone: String): Boolean {
        if (phone.length < 10 || phone.length > 13) return false

        return ghanaPhonePattern.matches(phone)
    }

    private fun extractSenderName(body: String): String? {
        val tfName = transferFromPattern.find(body)?.groupValues?.getOrNull(2)?.trim()
        if (tfName != null && tfName.isNotBlank()) return tfName

        val fromName = fromPhoneNamePattern.find(body)?.groupValues?.getOrNull(2)?.trim()
        if (fromName != null && fromName.isNotBlank()) return fromName

        val receivedFrom = receivedFromPattern.find(body)?.groupValues?.getOrNull(1)?.trim()
        if (receivedFrom != null && receivedFrom.isNotBlank() && receivedFrom.length < 60) {
            return receivedFrom
        }

        val generic = genericFromNamePattern.find(body)?.groupValues?.getOrNull(1)?.trim()
        if (generic != null && generic.isNotBlank() && generic.length < 60) return generic

        return null
    }

    private fun extractBalance(body: String): Double? {
        val match = balancePattern.find(body)?.groupValues?.getOrNull(1)
        if (match != null) {
            val parsed = match.replace(",", "").toDoubleOrNull()
            if (parsed != null && parsed > 0) return parsed
        }
        return null
    }

    private fun detectKeyword(body: String): String {
        val lower = body.lowercase()
        val candidates = listOf("received", "credited", "deposited", "cash in", "transferred")
        return candidates.firstOrNull { lower.contains(it) } ?: "received"
    }

    private fun computeConfidence(
        body: String,
        reference: String?,
        amount: Double?,
        phone: String?,
        name: String?
    ): Double {
        var score = 0.0

        if (reference != null) score += 0.25
        if (amount != null && amount > 0) score += 0.35
        if (phone != null) score += 0.15
        if (name != null && name.isNotBlank()) score += 0.1

        val hasReceived = listOf("received", "credited", "deposited")
            .any { body.lowercase().contains(it) }
        if (hasReceived) score += 0.15

        return min(1.0, score)
    }
}
