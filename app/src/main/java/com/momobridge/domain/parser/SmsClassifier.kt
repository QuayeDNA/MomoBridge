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

    private val otpKeywords = listOf(
        "OTP", "one.?time.?pin", "DO NOT SHARE", "do not share",
        "enter code", "verification code", "login code"
    )
    private val promoKeywords = listOf(
        "congrats", "mashup", "pulse", "just4u", "xtratime",
        "free 300mb", "never run out", "stay connected",
        "dial \\*", "recharge now", "repay your"
    )
    private val systemKeywords = listOf(
        "system upgrade", "planned", "scheduled maintenance",
        "you entered the wrong pin", "wrong pin"
    )

    private val receivedKeywords = listOf(
        "received", "credited", "deposited", "cash in",
        "transfer from"
    )
    private val sentKeywords = listOf(
        "sent to", "transferred to"
    )
    private val withdrawnKeywords = listOf(
        "withdrawn", "withdrawal", "cash out"
    )
    private val purchaseKeywords = listOf(
        "bought", "purchased", "airtime", "bundle", "data bundle"
    )
    private val paidKeywords = listOf(
        "paid to", "paid off"
    )
    private val loanKeywords = listOf(
        "loan", "flexloan", "ready loan", "xtracash"
    )

    private val exclusionPhrases = listOf(
        "otp", "one time pin", "do not share", "verification",
        "login code", "enter code"
    )

    fun classify(body: String): SmsAction {
        val lower = body.lowercase().trim()
        if (lower.isBlank()) return SmsAction.NON_TX

        val hasMoney = moneyPattern.containsMatchIn(body) ||
            bareAmountPattern.containsMatchIn(body)

        val hasOtp = otpKeywords.any { lower.contains(it) }
        val hasPromo = promoKeywords.any { lower.contains(it) }
        val hasSystem = systemKeywords.any { lower.contains(it) }
        val hasReceived = receivedKeywords.any { lower.contains(it) }
        val hasSent = sentKeywords.any { lower.contains(it) }
        val hasWithdrawn = withdrawnKeywords.any { lower.contains(it) }
        val hasPurchase = purchaseKeywords.any { lower.contains(it) }
        val hasPaid = paidKeywords.any { lower.contains(it) }
        val hasLoan = loanKeywords.any { lower.contains(it) }
        val isExcluded = exclusionPhrases.any { lower.contains(it) }

        if (isExcluded && !hasMoney) return SmsAction.NON_TX
        if (hasOtp && !hasMoney) return SmsAction.NON_TX
        if (hasPromo && !hasMoney) return SmsAction.NON_TX
        if (hasSystem) return SmsAction.NON_TX

        if (!hasMoney) return SmsAction.NON_TX

        if (hasLoan) return SmsAction.LOAN

        if (hasReceived) return SmsAction.RECEIVED

        if (hasSent) return SmsAction.SENT
        if (hasWithdrawn) return SmsAction.WITHDRAWN
        if (hasPurchase) return SmsAction.PURCHASE
        if (hasPaid) return SmsAction.BILL_PAYMENT

        val hasSentContext = Regex("\\b(sent|transferred|paid)\\b", RegexOption.IGNORE_CASE)
            .containsMatchIn(body) &&
            !Regex("\\b(received|credited|deposited)\\b", RegexOption.IGNORE_CASE)
                .containsMatchIn(body)
        if (hasSentContext) return SmsAction.SENT

        return SmsAction.NON_TX
    }
}
