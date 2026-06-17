package com.momobridge.domain.parser

import com.momobridge.domain.model.ParsingRule

data class DetectedFields(
    val ref: String,
    val amount: String,
    val senderName: String,
    val senderPhone: String,
    val balance: String,
    val keyword: String
)

object AutoDetectUtils {

    fun detectFields(body: String): DetectedFields {
        val lower = body.lowercase()

        val mtnRef = Regex("Transaction\\s+ID:\\s*(\\d{8,15})", RegexOption.IGNORE_CASE).find(body)
        val tCashRef = Regex("^(\\d{18,22})", RegexOption.IGNORE_CASE).find(body)
        val tCashAltRef = Regex("Transaction\\s+ID:\\s*(\\d{18,22})", RegexOption.IGNORE_CASE).find(body)
        val digitLong = Regex("(\\d{12,25})").findAll(body).toList()

        val mtnAmount = Regex(
            "(?:Payment\\s+received|Cash\\s+In\\s+received)\\s+for\\s+(?:GH[₵S]?|₵|GHS)\\s*([\\d,]+\\.?\\d*)",
            RegexOption.IGNORE_CASE
        ).find(body)

        val tCashAmount = Regex(
            "(?:received|credited)\\s+(?:GH[₵S]?|₵|GHS)\\s*([\\d,]+\\.?\\d*)",
            RegexOption.IGNORE_CASE
        ).find(body)

        val amountGeneral = Regex("(?:GH[₵S]?|₵|GHS)\\s*([\\d,]+\\.?\\d*)", RegexOption.IGNORE_CASE)
            .findAll(body).toList()

        val bareAmounts = Regex("[\\d,]+\\.\\d{2}", RegexOption.IGNORE_CASE)
            .findAll(body).toList()

        val tCashNameTransferFrom = Regex(
            "Transfer\\s+From:\\s*\\d{10,15}-([A-Za-z\\s./]+?)(?=\\s+on\\s)",
            RegexOption.IGNORE_CASE
        ).find(body)

        val tCashNamePhoneDash = Regex(
            "from\\s+\\d{10,15}\\s*-\\s*([A-Za-z\\s./]+?)(?=\\s+on\\s|$)",
            RegexOption.IGNORE_CASE
        ).find(body)

        val mtnNameFrom = Regex(
            "(?:Payment\\s+received|Cash\\s+In\\s+received)\\s+for\\s+(?:GH[₵S]?|₵|GHS)\\s*[\\d,.]+\\s+from\\s+([A-Za-z0-9\\s.&/-]+?)(?:\\.\\s*(?:Current\\s+Balance|Transaction\\s+ID))",
            RegexOption.IGNORE_CASE
        ).find(body)

        val genericNameFrom = Regex(
            "from\\s+([A-Za-z\\s.]+?)\\s+(?:0\\d{9}|233\\d{9}|\\d{5,})",
            RegexOption.IGNORE_CASE
        ).find(body)

        val phoneMatch = Regex("(0\\d{9}|233\\d{9})", RegexOption.IGNORE_CASE)
            .findAll(body).toList()

        val transferFromPhone = Regex("Transfer\\s+From:\\s*(\\d{10,15})-", RegexOption.IGNORE_CASE).find(body)

        val mtnBalance = Regex(
            "(?:Current\\s+Balance)[\\s:]*(?:GH[₵S]?|₵|GHS)?\\s*([\\d,]+\\.?\\d*)",
            RegexOption.IGNORE_CASE
        ).find(body)

        val tCashBalance = Regex(
            "(?:Telecel\\s+Cash\\s+balance|your\\s+balance|balance)\\s+(?:is\\s+)?(?:GH[₵S]?|₵|GHS)?\\s*([\\d,]+\\.?\\d*)",
            RegexOption.IGNORE_CASE
        ).findAll(body).toList()

        val balanceMatches = mtnBalance ?: tCashBalance.firstOrNull()

        val keywordCandidates = listOf("received", "credited", "paid", "transferred", "cash in")
        val keyword = keywordCandidates.firstOrNull { lower.contains(it) } ?: "received"

        val detectedRef = mtnRef?.groupValues?.getOrNull(1)
            ?: tCashRef?.groupValues?.getOrNull(1)
            ?: tCashAltRef?.groupValues?.getOrNull(1)
            ?: digitLong.firstOrNull()?.value ?: ""

        val detectedAmount = mtnAmount?.groupValues?.getOrNull(1)
            ?: tCashAmount?.groupValues?.getOrNull(1)
            ?: amountGeneral.firstOrNull()?.groupValues?.getOrNull(1)
            ?: bareAmounts.firstOrNull()?.value?.replace(",", "") ?: ""

        val detectedSenderName = tCashNameTransferFrom?.groupValues?.getOrNull(1)?.trim()
            ?: tCashNamePhoneDash?.groupValues?.getOrNull(1)?.trim()
            ?: mtnNameFrom?.groupValues?.getOrNull(1)?.trim()
            ?: genericNameFrom?.groupValues?.getOrNull(1)?.trim()
            ?: ""

        val detectedSenderPhone = transferFromPhone?.groupValues?.getOrNull(1)
            ?: phoneMatch.firstOrNull()?.value ?: ""

        val detectedBalance = balanceMatches?.groupValues?.getOrNull(1)?.replace(",", "") ?: ""

        return DetectedFields(
            ref = detectedRef,
            amount = detectedAmount,
            senderName = detectedSenderName,
            senderPhone = detectedSenderPhone,
            balance = detectedBalance,
            keyword = keyword
        )
    }

    fun buildRule(body: String, fields: DetectedFields): ParsingRule {
        val lower = body.lowercase()
        val ref = fields.ref
        val amount = fields.amount
        val keyword = fields.keyword
        val isDigitRef = ref.all { it.isDigit() }

        val isMtnFormat = lower.contains("payment received") || lower.contains("cash in received")
        val isTCashTransferFrom = lower.contains("transfer from")
        val isTCashPhoneDash = !isTCashTransferFrom && Regex("from\\s+\\d{10,15}\\s*-").containsMatchIn(body)
        val isTCashAlt = Regex("^\\d{18,22}").containsMatchIn(body) || Regex("Transaction\\s+ID:\\s*\\d{18,}").containsMatchIn(body)

        val refPattern = when {
            isMtnFormat -> "Transaction\\s+ID:\\s*(\\d{8,15})"
            isTCashAlt || isTCashTransferFrom || isTCashPhoneDash -> "(\\d{${ref.length}})"
            isDigitRef -> "(\\d{${ref.length}})"
            else -> "([A-Z0-9]{${ref.length}})"
        }

        val amountPattern = when {
            isMtnFormat -> "(?:Payment\\s+received|Cash\\s+In\\s+received)\\s+for\\s+(?:GH[₵S]?|₵|GHS)\\s*([\\d,]+\\.?\\d*)"
            lower.contains("received") || lower.contains("credited") -> "(?:received|credited)\\s+(?:GH[₵S]?|₵|GHS)\\s*([\\d,]+\\.?\\d*)"
            else -> "(?:GH[₵S]?|₵|GHS)\\s*([\\d,]+\\.?\\d*)"
        }

        val senderNamePattern = when {
            fields.senderName.isNotBlank() && isMtnFormat ->
                "(?:Payment\\s+received|Cash\\s+In\\s+received)\\s+for\\s+(?:GH[₵S]?|₵|GHS)\\s*[\\d,.]+\\s+from\\s+([A-Za-z0-9\\s.&/-]+?)(?:\\.\\s*(?:Current\\s+Balance|Transaction\\s+ID))"
            fields.senderName.isNotBlank() && isTCashTransferFrom ->
                "Transfer\\s+From:\\s*\\d{10,15}-([A-Za-z\\s./]+?)(?=\\s+on\\s)"
            fields.senderName.isNotBlank() && isTCashPhoneDash ->
                "from\\s+\\d{10,15}\\s*-\\s*([A-Za-z\\s./]+?)(?=\\s+on\\s|\\$)"
            fields.senderName.isNotBlank() ->
                "(?:from|by|sender)[:\\s]+([A-Za-z\\s.]+?)(?=\\s+(?:0\\d{9}|233\\d{9}|\\d{5,}|\\())"
            else -> null
        }

        val senderPhonePattern = when {
            fields.senderPhone.isNotBlank() && isTCashTransferFrom ->
                "Transfer\\s+From:\\s*(\\d{10,15})-"
            fields.senderPhone.isNotBlank() && isTCashPhoneDash ->
                "from\\s+(\\d{10,15})\\s*-"
            fields.senderPhone.isNotBlank() ->
                "(0\\d{9}|233\\d{9})"
            else -> null
        }

        val balancePattern = if (fields.balance.isNotBlank()) {
            when {
                isMtnFormat -> "(?:Current\\s+Balance)[\\s:]*(?:GH[₵S]?|₵|GHS)?\\s*([\\d,]+\\.?\\d*)"
                else -> "(?:Telecel\\s+Cash\\s+balance|your\\s+balance|balance)\\s+(?:is\\s+)?(?:GH[₵S]?|₵|GHS)?\\s*([\\d,]+\\.?\\d*)"
            }
        } else null

        return ParsingRule(
            refPattern = refPattern,
            amountPattern = amountPattern,
            senderNamePattern = senderNamePattern,
            senderPhonePattern = senderPhonePattern,
            balancePattern = balancePattern,
            isCreditKeyword = keyword,
            exampleMessage = body
        )
    }
}
