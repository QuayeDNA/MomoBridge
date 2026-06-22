package com.momobridge.domain.parser

import kotlin.math.abs
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

private data class Span(val start: Int, val end: Int, val text: String) {
    fun overlaps(other: Span): Boolean =
        start < other.end && other.start < end
}

object FieldExtractor {

    private val currencyPrefix = Regex(
        "(?:GH[₵S]|₵|GHS|GHc|GH¢|¢)",
        RegexOption.IGNORE_CASE
    )

    private val digitRun = Regex("\\b(\\d{8,22})\\b")
    private val currencyAmount = Regex(
        "(?:GH[₵S]?|₵|GHS|GHc|GH¢|¢)\\s*([\\d,]+(?:\\.\\d{1,2})?)",
        RegexOption.IGNORE_CASE
    )
    private val phoneRun = Regex("\\b(\\d{9,15})\\b")
    private val labeledRef = Regex(
        "(?:Ref(?:erence)?[\\s:#]*|Transaction\\s*(?:ID|number|ref|No)[\\s:#]*)(\\w+)",
        RegexOption.IGNORE_CASE
    )

    private val anchorWords = listOf(
        "from", "Transfer From:", "sender", "received", "credited",
        "deposited", "balance is", "balance:", "by"
    )
    private val nameDenylist = setOf(
        "MTN", "MOBILE", "MONEY", "TELECEL", "CASH", "AIRTIME",
        "GHANA", "LOAN", "BALANCE", "PIN", "OTP", "REFERENCE",
        "TRANSACTION", "ID", "CONFIRMED", "YOU", "YOUR", "HAVE",
        "HAS", "FOR", "THIS", "WAS", "THE", "NEW", "CURRENT",
        "PAYMENT", "RECEIVED", "CREDITED", "FROM", "TRANSFER",
        "WITH", "AT", "ON", "IS", "OF", "TO", "BY"
    )

    fun extract(body: String): ExtractedFields {
        val refCandidate = pickBestReference(body)
        val amtCandidate = pickBestAmount(body)
        val phoneCandidates = generatePhoneCandidates(body)
        val nameCandidates = generateNameCandidates(body)

        val chosenPhone = pickBestPhone(phoneCandidates, nameCandidates, refCandidate?.span, amtCandidate?.span)
        val chosenName = pickBestName(nameCandidates, chosenPhone?.span)

        val balance = extractBalance(body)
        val keyword = detectKeyword(body)

        val confidence = computeConfidence(
            body = body,
            reference = refCandidate?.value,
            amount = amtCandidate?.value,
            phone = chosenPhone?.value,
            name = chosenName?.value
        )

        return ExtractedFields(
            reference = refCandidate?.value,
            amount = amtCandidate?.value,
            senderPhone = chosenPhone?.value,
            senderName = chosenName?.value,
            balanceAfter = balance,
            keyword = keyword,
            confidence = confidence
        )
    }

    // ── Reference ────────────────────────────────────────────────────────────

    private data class RefCandidate(val value: String, val span: Span, val score: Double)

    private fun pickBestReference(body: String): RefCandidate? {
        val allLabeled = labeledRef.findAll(body).toList()

        // Priority 1: Transaction ID label (e.g. "Transaction ID: 72581798824")
        val txnLabeled = allLabeled.firstOrNull {
            it.value.lowercase().startsWith("transaction")
        }
        if (txnLabeled != null) {
            val v = txnLabeled.groupValues[1]
            val s = Span(txnLabeled.range.first, txnLabeled.range.last + 1, v)
            return RefCandidate(v, s, 2.0)
        }

        // Priority 2: Long digit run at start of text (T-CASH leading Transaction ID)
        val digitCandidates = digitRun.findAll(body).map { m ->
            val v = m.groupValues[1]
            val s = Span(m.range.first, m.range.last + 1, v)
            val posScore = 1.0 - (m.range.first.toDouble() / body.length.coerceAtLeast(1))
            RefCandidate(v, s, posScore)
        }.toList()
        if (digitCandidates.isNotEmpty()) {
            return digitCandidates.maxByOrNull { it.score }
        }

        // Priority 3: Ref/Reference label (customer memo, lowest priority)
        val refLabeled = allLabeled.firstOrNull()
        if (refLabeled != null) {
            val v = refLabeled.groupValues[1]
            val s = Span(refLabeled.range.first, refLabeled.range.last + 1, v)
            return RefCandidate(v, s, 1.0)
        }

        return null
    }

    // ── Amount ───────────────────────────────────────────────────────────────

    private data class AmtCandidate(val value: Double, val span: Span, val score: Double)

    private fun pickBestAmount(body: String): AmtCandidate? {
        val all = currencyAmount.findAll(body).map { m ->
            val raw = m.groupValues[1].replace(",", "")
            val parsed = raw.toDoubleOrNull()
            if (parsed == null || parsed <= 0) return@map null

            val s = Span(m.range.first, m.range.last + 1, m.value)
            val contextScore = scoreByContext(body, m.range.first, m.range.last)
            AmtCandidate(parsed, s, contextScore)
        }.filterNotNull().toList()

        if (all.isEmpty()) return null

        val best = all.maxByOrNull { it.score }!!

        val bodyLower = body.lowercase()
        val isCreditContext = listOf("received", "credited", "deposited", "payment received")
            .any { bodyLower.contains(it) }

        if (!isCreditContext && best.score < 0.3) return null

        return best
    }

    // ── Phone ────────────────────────────────────────────────────────────────

    private data class PhoneCandidate(val value: String, val span: Span, val score: Double)

    private fun generatePhoneCandidates(body: String): List<PhoneCandidate> {
        return phoneRun.findAll(body).map { m ->
            val v = m.groupValues[1]
            val s = Span(m.range.first, m.range.last + 1, v)
            val ctxScore = scoreByContext(body, m.range.first, m.range.last)
            PhoneCandidate(v, s, ctxScore)
        }.toList()
    }

    private fun pickBestPhone(
        candidates: List<PhoneCandidate>,
        nameCandidates: List<NameCandidate>,
        refSpan: Span?,
        amtSpan: Span?
    ): PhoneCandidate? {
        val eligible = candidates.filter { c ->
            (refSpan == null || !c.span.overlaps(refSpan)) &&
                (amtSpan == null || !c.span.overlaps(amtSpan))
        }

        val scored = eligible.map { pc ->
            val nameAdjScore = nameCandidates.maxOfOrNull { nc ->
                val gap = abs(pc.span.start - nc.span.end).coerceAtMost(abs(nc.span.start - pc.span.end))
                if (gap < 30) 0.3 * (1.0 - gap / 30.0) else 0.0
            } ?: 0.0
            pc.copy(score = pc.score + nameAdjScore)
        }

        return scored.maxByOrNull { it.score }
    }

    // ── Name ─────────────────────────────────────────────────────────────────

    private data class NameCandidate(val value: String, val span: Span, val score: Double)

    private val wordPattern = Regex("[A-Za-z]{2,}")

    private fun generateNameCandidates(body: String): List<NameCandidate> {
        val words = wordPattern.findAll(body).toList()
        val denylistLower = nameDenylist.map { it.lowercase() }.toSet()

        val merged = mutableListOf<NameCandidate>()
        var i = 0
        while (i < words.size) {
            val w = words[i].value.lowercase()
            if (w in denylistLower || w.length < 2) {
                i++
                continue
            }

            val start = words[i].range.first
            val nameWords = mutableListOf(words[i].value)
            var j = i + 1
            while (j < words.size) {
                val nw = words[j].value.lowercase()
                if (nw in denylistLower || nw.length < 2) break
                if (words[j].range.first - words[j - 1].range.last > 2) break
                nameWords.add(words[j].value)
                j++
            }

            if (nameWords.size >= 2) {
                val name = nameWords.joinToString(" ")
                val end = words[j - 1].range.last + 1
                val s = Span(start, end, name)
                val ctxScore = scoreByContext(body, start, end)
                merged.add(NameCandidate(name, s, ctxScore))
            }

            i = j
        }
        return merged
    }

    private fun pickBestName(
        candidates: List<NameCandidate>,
        phoneSpan: Span?
    ): NameCandidate? {
        val scored = candidates.map { nc ->
            val phoneAdjScore = if (phoneSpan != null) {
                val gap = abs(nc.span.start - phoneSpan.end).coerceAtMost(abs(phoneSpan.start - nc.span.end))
                if (gap < 30) 0.3 * (1.0 - gap / 30.0) else 0.0
            } else 0.0
            nc.copy(score = nc.score + phoneAdjScore)
        }
        return scored.maxByOrNull { it.score }?.takeIf { it.score >= 0.1 }
    }

    // ── Shared ───────────────────────────────────────────────────────────────

    private fun scoreByContext(body: String, spanStart: Int, spanEnd: Int): Double {
        val pre = body.substring(maxOf(0, spanStart - 60), spanStart)
        val preLower = pre.lowercase()
        var score = 0.0

        for (anchor in anchorWords) {
            val anchorLower = anchor.lowercase()
            if (preLower.contains(anchorLower)) {
                val idx = preLower.lastIndexOf(anchorLower)
                val distance = pre.length - idx
                score = maxOf(score, 0.6 * (1.0 - distance.toDouble() / 60.0))
            }
        }

        val post = body.substring(spanEnd, minOf(body.length, spanEnd + 40))
        val postLower = post.lowercase()
        if (postLower.contains("on") || postLower.contains("balance")) {
            score += 0.2
        }

        val bodyLower = body.lowercase()
        if (bodyLower.contains("transfer from:")) {
            score += 0.15
        }

        return min(1.0, score)
    }

    // ── Balance ──────────────────────────────────────────────────────────────

    private val balancePattern = Regex(
        "(?:balance|your\\s+balance|new\\s+balance|current\\s+balance|Telecel\\s+Cash\\s+balance)" +
            "[\\s:]*.*?(?:GH[₵S]?|₵|GHS|¢)?\\s*([\\d,]+\\.?\\d*)",
        RegexOption.IGNORE_CASE
    )

    private fun extractBalance(body: String): Double? {
        val match = balancePattern.find(body)?.groupValues?.getOrNull(1)
        if (match != null) {
            val parsed = match.replace(",", "").toDoubleOrNull()
            if (parsed != null && parsed > 0) return parsed
        }
        return null
    }

    // ── Keyword ──────────────────────────────────────────────────────────────

    private fun detectKeyword(body: String): String {
        val lower = body.lowercase()
        val candidates = listOf("received", "credited", "deposited", "cash in", "transferred")
        return candidates.firstOrNull { lower.contains(it) } ?: "received"
    }

    // ── Confidence ───────────────────────────────────────────────────────────

    private fun computeConfidence(
        body: String,
        reference: String?,
        amount: Double?,
        phone: String?,
        name: String?
    ): Double {
        var score = 0.0

        if (reference != null) score += 0.45
        if (amount != null && amount > 0) score += 0.45
        if (phone != null) score += 0.05
        if (name != null && name.isNotBlank()) score += 0.05

        return min(1.0, score)
    }
}
