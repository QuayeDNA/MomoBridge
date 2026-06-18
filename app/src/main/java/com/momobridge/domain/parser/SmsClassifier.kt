package com.momobridge.domain.parser

enum class SmsAction {
    RECEIVED,
    SENT,
    WITHDRAWN,
    BILL_PAYMENT,
    PURCHASE,
    LOAN,
    NON_TX
}

object SmsClassifier {

    private val moneyPattern = Regex(
        "(?:GH[₵S]?|₵|GHS|¢|\\bGhc|\\bGHc|\\bGH¢)\\s*([\\d,]+(?:\\.\\d{1,2})?)",
        RegexOption.IGNORE_CASE
    )
    private val bareAmountPattern = Regex("\\b[\\d,]+\\.\\d{2}\\b")
    private val digitRunPattern = Regex("\\b\\d{8,22}\\b")

    private val nonTxPatterns = listOf(
        Regex("dial\\s+\\*", RegexOption.IGNORE_CASE),
        Regex("DO NOT SHARE", RegexOption.IGNORE_CASE),
        Regex("enter code", RegexOption.IGNORE_CASE),
        Regex("verification code", RegexOption.IGNORE_CASE),
        Regex("login code", RegexOption.IGNORE_CASE),
        Regex("once?\\s*time\\s*pin", RegexOption.IGNORE_CASE),
        Regex("congrats", RegexOption.IGNORE_CASE),
        Regex("mashup", RegexOption.IGNORE_CASE),
        Regex("pulse", RegexOption.IGNORE_CASE),
        Regex("just4u", RegexOption.IGNORE_CASE),
        Regex("xtratime", RegexOption.IGNORE_CASE),
        Regex("free 300mb", RegexOption.IGNORE_CASE),
        Regex("never run out", RegexOption.IGNORE_CASE),
        Regex("stay connected", RegexOption.IGNORE_CASE),
        Regex("recharge now", RegexOption.IGNORE_CASE),
        Regex("system upgrade", RegexOption.IGNORE_CASE),
        Regex("scheduled maintenance", RegexOption.IGNORE_CASE),
        Regex("wrong pin", RegexOption.IGNORE_CASE)
    )

    private val loanPatterns = listOf(
        Regex("flexloan", RegexOption.IGNORE_CASE),
        Regex("ready loan", RegexOption.IGNORE_CASE),
        Regex("sos loan", RegexOption.IGNORE_CASE),
        Regex("loan processing fee", RegexOption.IGNORE_CASE),
        Regex("xtratime loan", RegexOption.IGNORE_CASE),
        Regex("xtracash", RegexOption.IGNORE_CASE),
        Regex("repay your", RegexOption.IGNORE_CASE)
    )

    private val purchasePatterns = listOf(
        Regex("bundle\\s+purchase\\s+(?:request|of)", RegexOption.IGNORE_CASE),
        Regex("purchase\\s+request\\s+of", RegexOption.IGNORE_CASE),
        Regex("bought\\s+(?:a )?(?:GH[₵S]?|₵|GHS)?\\s*[\\d,]+\\s+of\\s+airtime", RegexOption.IGNORE_CASE),
        Regex("airtime\\s+for\\s+\\d+", RegexOption.IGNORE_CASE),
        Regex("paid\\s+off", RegexOption.IGNORE_CASE),
        Regex("service charge", RegexOption.IGNORE_CASE),
        Regex("principal", RegexOption.IGNORE_CASE),
        Regex("\\bairtime\\s+(?:of|purchase)\\b", RegexOption.IGNORE_CASE),
        // "You have received airtime of" is a purchase (airtime bought for someone)
        Regex("received\\s+airtime\\s+of", RegexOption.IGNORE_CASE),
        // "your bundle purchase request ... has been received" — a purchase confirmation
        Regex("bundle\\s+purchase.*has been received", RegexOption.IGNORE_CASE),
        Regex("data bundle", RegexOption.IGNORE_CASE)
    )

    private val withdrawnPatterns = listOf(
        Regex("withdrawn", RegexOption.IGNORE_CASE),
        Regex("withdrawal", RegexOption.IGNORE_CASE),
        Regex("cash out", RegexOption.IGNORE_CASE)
    )

    private val sentPatterns = listOf(
        Regex("sent\\s+to", RegexOption.IGNORE_CASE),
        Regex("transferred\\s+to", RegexOption.IGNORE_CASE)
    )

    private val receivedShape = Regex(
        "(?:have|has)\\s+received\\s+(?:GH[₵S]?|₵|GHS)\\s*[\\d,.]+\\s+from",
        RegexOption.IGNORE_CASE
    )
    private val creditedShape = Regex(
        "(?:credited|deposited)\\s+(?:GH[₵S]?|₵|GHS)\\s*[\\d,.]",
        RegexOption.IGNORE_CASE
    )
    private val paymentReceivedShape = Regex(
        "payment\\s+received\\s+for\\s+(?:GH[₵S]?|₵|GHS)",
        RegexOption.IGNORE_CASE
    )
    private val cashInShape = Regex(
        "cash\\s+in\\s+(?:received|of)\\s+(?:GH[₵S]?|₵|GHS)",
        RegexOption.IGNORE_CASE
    )

    fun classify(body: String): SmsAction {
        val lower = body.lowercase().trim()
        if (lower.isBlank()) return SmsAction.NON_TX

        val hasMoney = moneyPattern.containsMatchIn(body) ||
            bareAmountPattern.containsMatchIn(body) ||
            digitRunPattern.containsMatchIn(body)
        val isNonTx = nonTxPatterns.any { it.containsMatchIn(body) }
        val isLoan = loanPatterns.any { it.containsMatchIn(body) }
        val isPurchase = purchasePatterns.any { it.containsMatchIn(body) }
        val isWithdrawn = withdrawnPatterns.any { it.containsMatchIn(body) }
        val isSent = sentPatterns.any { it.containsMatchIn(body) }

        if (isNonTx && !hasMoney) return SmsAction.NON_TX

        if (isLoan) return SmsAction.LOAN

        if (isPurchase) return SmsAction.PURCHASE

        if (isWithdrawn) return SmsAction.WITHDRAWN

        if (isSent) return SmsAction.SENT

        if (receivedShape.containsMatchIn(body) ||
            creditedShape.containsMatchIn(body) ||
            paymentReceivedShape.containsMatchIn(body) ||
            cashInShape.containsMatchIn(body)
        ) return SmsAction.RECEIVED

        val hasReceivedWord = Regex(
            "\\b(?:received|credited|deposited)\\b", RegexOption.IGNORE_CASE
        ).containsMatchIn(body)
        val hasSentWord = Regex(
            "\\b(?:sent|transferred|paid)\\b", RegexOption.IGNORE_CASE
        ).containsMatchIn(body)

        if (hasReceivedWord && !hasSentWord && hasMoney) return SmsAction.RECEIVED

        return SmsAction.NON_TX
    }
}
