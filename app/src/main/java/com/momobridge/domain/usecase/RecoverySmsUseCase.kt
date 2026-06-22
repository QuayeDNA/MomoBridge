package com.momobridge.domain.usecase

import android.content.ContentResolver
import android.net.Uri
import android.provider.Telephony
import com.momobridge.domain.model.ParsedTransaction
import com.momobridge.domain.model.SmsSource
import com.momobridge.domain.parser.SmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecoverySmsUseCase @Inject constructor() {

    suspend fun recoverRecent(
        contentResolver: ContentResolver,
        sources: List<SmsSource>,
        referenceExists: suspend (String) -> Boolean,
        saveTransaction: suspend (ParsedTransaction) -> Boolean
    ): Int = withContext(Dispatchers.IO) {
        var savedCount = 0
        val lookbackMs = 5 * 60 * 1000L
        val cutoff = System.currentTimeMillis() - lookbackMs

        for (source in sources) {
            if (!source.enabled) continue
            try {
                val uri = Uri.parse("content://sms/inbox")
                val projection = arrayOf(
                    Telephony.TextBasedSmsColumns.BODY,
                    Telephony.TextBasedSmsColumns.DATE
                )
                val selection = "${Telephony.TextBasedSmsColumns.ADDRESS} = ? AND ${Telephony.TextBasedSmsColumns.DATE} >= ?"
                val selectionArgs = arrayOf(source.senderAddress, cutoff.toString())
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

                        if (parsed?.reference == null) continue

                        if (referenceExists(parsed.reference!!)) continue

                        val saved = saveTransaction(parsed)
                        if (saved) savedCount++
                    }
                }
            } catch (_: Exception) { }
        }

        savedCount
    }
}
