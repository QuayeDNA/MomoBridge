package com.momobridge.domain.usecase

import com.momobridge.domain.model.ParsingRule
import com.momobridge.domain.parser.SmsParser
import javax.inject.Inject
import javax.inject.Singleton

data class RuleValidationResult(
    val totalTested: Int,
    val succeeded: Int,
    val failed: Int,
    val failures: List<RuleFailure>
)

data class RuleFailure(
    val messageBody: String,
    val reason: String
)

@Singleton
class ValidateParsingRuleUseCase @Inject constructor() {

    fun validate(
        rule: ParsingRule,
        sampleMessages: List<String>,
        sender: String = ""
    ): RuleValidationResult {
        val failures = mutableListOf<RuleFailure>()
        var succeeded = 0

        for (body in sampleMessages) {
            val parsed = SmsParser.parse(body, rule, System.currentTimeMillis(), sender)
            if (parsed == null) {
                val result = SmsParser.parseHeuristic(body, System.currentTimeMillis(), sender)
                val reason = if (result == null) {
                    "Not classified as a received payment. Check sender type or keywords."
                } else {
                    "Regex patterns did not match. Check reference and amount patterns."
                }
                failures.add(RuleFailure(body.take(100), reason))
            } else {
                succeeded++
            }
        }

        return RuleValidationResult(
            totalTested = sampleMessages.size,
            succeeded = succeeded,
            failed = failures.size,
            failures = failures
        )
    }
}
