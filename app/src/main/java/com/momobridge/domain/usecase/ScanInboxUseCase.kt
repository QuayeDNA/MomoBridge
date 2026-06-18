package com.momobridge.domain.usecase

import android.content.ContentResolver
import android.net.Uri
import android.provider.Telephony
import com.momobridge.domain.model.ParsedTransaction
import com.momobridge.domain.model.SmsSource
import com.momobridge.domain.parser.SmsAction
import com.momobridge.domain.parser.SmsClassifier
import com.momobridge.domain.parser.SmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SenderScanResult(
    val senderAddress: String,
    val label: String,
    val totalMessages: Int,
    val receivedCount: Int,
    val sentCount: Int,
    val withdrawnCount: Int,
    val purchaseCount: Int,
    val loanCount: Int,
    val nonTxCount: Int
)

data class HistoricalParseResult(
    val found: Int,
    val skipped: Int,
    val heuristicCount: Int
)

@Singleton
class ScanInboxUseCase @Inject constructor() {

    fun scanSenders(
        contentResolver: ContentResolver,
        limit: Int = 50
    ): List<SenderScanResult> {
        val senderMap = mutableMapOf<String, SenderCounts>()

        try {
            val uri = Uri.parse("content://sms/inbox")
            val projection = arrayOf(
                Telephony.TextBasedSmsColumns.ADDRESS,
                Telephony.TextBasedSmsColumns.BODY,
                Telephony.TextBasedSmsColumns.DATE
            )
            val cursor = contentResolver.query(
                uri, projection, null, null,
                "${Telephony.TextBasedSmsColumns.DATE} DESC"
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val address = it.getString(0) ?: continue
                    val body = it.getString(1) ?: continue

                    val counts = senderMap.getOrPut(address) {
                        SenderCounts(address = address)
                    }
                    counts.totalMessages++

                    val action = SmsClassifier.classify(body)
                    when (action) {
                        SmsAction.RECEIVED -> counts.receivedCount++
                        SmsAction.SENT -> counts.sentCount++
                        SmsAction.WITHDRAWN -> counts.withdrawnCount++
                        SmsAction.PURCHASE, SmsAction.BILL_PAYMENT -> counts.purchaseCount++
                        SmsAction.LOAN -> counts.loanCount++
                        SmsAction.NON_TX -> counts.nonTxCount++
                    }
                }
            }
        } catch (_: Exception) { }

        return senderMap.values
            .sortedByDescending { it.totalMessages }
            .take(limit)
            .map { it.toResult() }
    }

    suspend fun scanHistoricalTransactions(
        contentResolver: ContentResolver,
        sources: List<SmsSource>,
        saveTransaction: suspend (ParsedTransaction) -> Boolean
    ): HistoricalParseResult = withContext(Dispatchers.IO) {
        val twoMonthsAgo = System.currentTimeMillis() - (62L * 24 * 60 * 60 * 1000)
        var found = 0
        var skipped = 0
        var heuristicCount = 0
        val alreadySeen = mutableSetOf<String>()

        for (source in sources) {
            if (!source.enabled) continue
            try {
                val uri = Uri.parse("content://sms/inbox")
                val projection = arrayOf(
                    Telephony.TextBasedSmsColumns.BODY,
                    Telephony.TextBasedSmsColumns.DATE
                )
                val selection = "${Telephony.TextBasedSmsColumns.ADDRESS} = ? " +
                    "AND ${Telephony.TextBasedSmsColumns.DATE} >= ?"
                val selectionArgs = arrayOf(source.senderAddress, twoMonthsAgo.toString())
                val cursor = contentResolver.query(
                    uri, projection, selection, selectionArgs,
                    "${Telephony.TextBasedSmsColumns.DATE} ASC"
                )
                cursor?.use {
                    while (it.moveToNext()) {
                        val body = it.getString(0) ?: continue
                        val timestamp = it.getLong(1)

                        val parsed = if (source.parsingRule != null) {
                            SmsParser.parse(body, source.parsingRule, timestamp, source.senderAddress)
                        } else {
                            SmsParser.parseHeuristic(body, timestamp, source.senderAddress)
                        }

                        if (parsed == null) {
                            skipped++
                            continue
                        }

                        if (parsed.reference == null) {
                            skipped++
                            continue
                        }
                        val ref = parsed.reference!!
                        if (ref in alreadySeen) {
                            skipped++
                            continue
                        }
                        alreadySeen.add(ref)

                        val saved = saveTransaction(parsed)
                        if (saved) {
                            found++
                            if (parsed.parsedBy == "heuristic") heuristicCount++
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        HistoricalParseResult(
            found = found,
            skipped = skipped,
            heuristicCount = heuristicCount
        )
    }

    private data class SenderCounts(
        val address: String,
        var totalMessages: Int = 0,
        var receivedCount: Int = 0,
        var sentCount: Int = 0,
        var withdrawnCount: Int = 0,
        var purchaseCount: Int = 0,
        var loanCount: Int = 0,
        var nonTxCount: Int = 0
    ) {
        fun toResult() = SenderScanResult(
            senderAddress = address,
            label = address,
            totalMessages = totalMessages,
            receivedCount = receivedCount,
            sentCount = sentCount,
            withdrawnCount = withdrawnCount,
            purchaseCount = purchaseCount,
            loanCount = loanCount,
            nonTxCount = nonTxCount
        )
    }
}
