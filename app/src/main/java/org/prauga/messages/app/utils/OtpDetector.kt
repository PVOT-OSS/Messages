/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */

package org.prauga.messages.common.util

data class OtpDetectionResult(
    val isOtp: Boolean,
    val code: String?,
    val confidence: Double,
    val reason: String
)

class OtpDetector(private val resourceProvider: OtpResourceProvider) {
    private val otpKeywords = resourceProvider.getOtpKeywords()
    private val safetyKeywords = resourceProvider.getSafetyKeywords()
    private val moneyIndicators = resourceProvider.getMoneyIndicators()

    fun detect(rawMessage: String): OtpDetectionResult {
        val message = rawMessage.trim()
        if (message.isEmpty()) {
            return OtpDetectionResult(
                false,
                null,
                0.0,
                resourceProvider.getErrorMessage(OtpErrorType.EMPTY_MESSAGE)
            )
        }

        val normalized = normalizeWhitespace(message)
        val lower = normalized.lowercase()

        val hasOtpKeyword = otpKeywords.any { lower.contains(it) }
        val hasSafetyKeyword = safetyKeywords.any { lower.contains(it) }

        val candidates = extractCandidates(normalized)

        if (candidates.isEmpty()) {
            val reason = if (hasOtpKeyword) {
                resourceProvider.getErrorMessage(OtpErrorType.KEYWORD_BUT_NO_CODE)
            } else {
                resourceProvider.getErrorMessage(OtpErrorType.NO_OTP_KEYWORD)
            }
            return OtpDetectionResult(false, null, 0.1, reason)
        }

        // Compute a score for each candidate
        val scored = candidates.map { candidate ->
            val score =
                scoreCandidate(candidate, normalized, lower, hasOtpKeyword, hasSafetyKeyword)
            candidate.copy(score = score)
        }

        val best = scored.maxByOrNull { it.score }!!

        val globalConfidence = computeGlobalConfidence(best, hasOtpKeyword, hasSafetyKeyword)

        val isOtp = globalConfidence >= 0.6

        val reason = buildString {
            append(
                "Best candidate: '${best.code}' (len=${best.code.length}), score=${
                    "%.2f".format(
                        best.score
                    )
                }. "
            )
            append(
                "HasOtpKeyword=$hasOtpKeyword, HasSafetyKeyword=$hasSafetyKeyword, GlobalConfidence=${
                    "%.2f".format(
                        globalConfidence
                    )
                }."
            )
        }

        return OtpDetectionResult(
            isOtp = isOtp,
            code = if (isOtp) best.code else null,
            confidence = globalConfidence,
            reason = reason
        )
    }

    private data class Candidate(
        val code: String,
        val startIndex: Int,
        val endIndex: Int,
        val isNumeric: Boolean,
        val score: Double = 0.0
    )

    private fun normalizeWhitespace(input: String): String =
        input.replace(Regex(OtpPatternConfig.WHITESPACE_REGEX), " ").trim()

    private fun extractCandidates(message: String): List<Candidate> {
        val candidates = mutableListOf<Candidate>()

        // 1) Pure numeric chunks 3–10 digits
        val numericRegex = Regex(OtpPatternConfig.NUMERIC_REGEX)
        numericRegex.findAll(message).forEach { match ->
            val code = match.value
            candidates += Candidate(
                code = code,
                startIndex = match.range.first,
                endIndex = match.range.last,
                isNumeric = true
            )
        }

        // 2) Numeric with a single space or dash (e.g., "123 456", "12-34-56")
        val spacedRegex = Regex(OtpPatternConfig.SPACED_NUMERIC_REGEX)
        spacedRegex.findAll(message).forEach { match ->
            val raw = match.value
            val normalizedCode = raw.replace("[\\s-]".toRegex(), "")
            // Avoid duplicating codes we already saw as a plain numeric chunk
            if (normalizedCode.length in 4..8 && candidates.none { it.code == normalizedCode }) {
                candidates += Candidate(
                    code = normalizedCode,
                    startIndex = match.range.first,
                    endIndex = match.range.last,
                    isNumeric = true
                )
            }
        }

        // 3) Alphanumeric tokens 4–10 chars, at least 2 digits
        val alnumRegex = Regex(OtpPatternConfig.ALPHANUMERIC_REGEX)
        alnumRegex.findAll(message).forEach { match ->
            val token = match.value
            if (token.any { it.isDigit() } && token.count { it.isDigit() } >= 2) {
                // Skip if it's purely numeric; we already captured those
                if (!token.all { it.isDigit() }) {
                    candidates += Candidate(
                        code = token,
                        startIndex = match.range.first,
                        endIndex = match.range.last,
                        isNumeric = false
                    )
                }
            }
        }

        return candidates
    }

    private fun scoreCandidate(
        candidate: Candidate,
        original: String,
        lower: String,
        hasOtpKeyword: Boolean,
        hasSafetyKeyword: Boolean
    ): Double {
        var score = 0.0

        val len = candidate.code.length

        // Length preference: 6 is king, but 4–8 is common.
        score += when {
            len == 6 -> 3.0
            len in 4..8 -> 2.0
            len in 3..10 -> 1.0
            else -> -1.0
        }

        // Numeric gets a slight boost over alphanumeric (based on current SMS trends)
        if (candidate.isNumeric) {
            score += 0.5
        }

        // Penalize "weird" lengths that look like phone numbers or ids
        if (candidate.code.length >= 9 && candidate.isNumeric) {
            score -= 1.5
        }

        // Local context: the line containing the candidate
        val lineInfo = extractLineContext(original, candidate.startIndex, candidate.endIndex)

        // If the line is mostly just the code -> strong hint
        val trimmedLine = lineInfo.line.trim()
        if (trimmedLine == candidate.code) {
            score += 2.5
        }

        // Typical OTP line patterns
        if (Regex(
                OtpPatternConfig.OTP_LINE_PATTERN,
                RegexOption.IGNORE_CASE
            ).containsMatchIn(lineInfo.line)
        ) {
            score += 2.0
        }

        if (Regex("(:|is|=)\\s*${Regex.escape(candidate.code)}").containsMatchIn(lineInfo.line)) {
            score += 1.5
        }

        // Distance to OTP keywords (global)
        val minKeywordDistance = minKeywordDistance(lower, candidate)
        if (minKeywordDistance != null) {
            when {
                minKeywordDistance <= 20 -> score += 2.0
                minKeywordDistance <= 40 -> score += 1.0
                minKeywordDistance <= 80 -> score += 0.5
            }
        }

        // Safety keywords in the whole message => boost
        if (hasSafetyKeyword) {
            score += 1.0
        }

        // Money / transaction amount heuristics
        val hasMoneyContextNear = hasIndicatorNear(
            text = lower,
            indicators = moneyIndicators,
            index = candidate.startIndex,
            radius = 25
        )
        if (hasMoneyContextNear) {
            score -= 2.0 // looks like an amount, not an OTP
        }

        // Phone number shape (country code, leading +, etc.)
        if (looksLikePhoneNumber(candidate, original)) {
            score -= 2.5
        }

        return score
    }

    private data class LineContext(val line: String, val from: Int, val to: Int)

    private fun extractLineContext(text: String, start: Int, end: Int): LineContext {
        var lineStart = start
        var lineEnd = end

        while (lineStart > 0 && text[lineStart - 1] != '\n') {
            lineStart--
        }
        while (lineEnd < text.lastIndex && text[lineEnd + 1] != '\n') {
            lineEnd++
        }

        return LineContext(
            line = text.substring(lineStart..lineEnd),
            from = lineStart,
            to = lineEnd
        )
    }

    private fun minKeywordDistance(lower: String, candidate: Candidate): Int? {
        val candidateCenter = (candidate.startIndex + candidate.endIndex) / 2
        var best: Int? = null

        otpKeywords.forEach { keyword ->
            var index = lower.indexOf(keyword)
            while (index >= 0) {
                val keywordCenter = index + keyword.length / 2
                val distance = kotlin.math.abs(candidateCenter - keywordCenter)
                if (best == null || distance < best!!) {
                    best = distance
                }
                index = lower.indexOf(keyword, startIndex = index + keyword.length)
            }
        }

        return best
    }

    private fun hasIndicatorNear(
        text: String,
        indicators: List<String>,
        index: Int,
        radius: Int
    ): Boolean {
        val start = (index - radius).coerceAtLeast(0)
        val end = (index + radius).coerceAtMost(text.length)
        if (start >= end) return false
        val window = text.substring(start, end)
        return indicators.any { window.contains(it) }
    }

    private fun looksLikePhoneNumber(candidate: Candidate, original: String): Boolean {
        // If the candidate is preceded by + or "tel"/"call", it's probably a phone.
        val start = candidate.startIndex
        val prefixStart = (start - 5).coerceAtLeast(0)
        val prefix = original.substring(prefixStart, start)

        if (prefix.contains("+")) return true
        if (prefix.lowercase().contains("tel") || prefix.lowercase().contains("call")) return true

        // 9+ digits and starting with 1 or typical mobile prefixes could be phone number
        if (candidate.isNumeric && candidate.code.length >= 9) {
            return true
        }

        return false
    }

    private fun computeGlobalConfidence(
        best: Candidate,
        hasOtpKeyword: Boolean,
        hasSafetyKeyword: Boolean
    ): Double {
        var confidence = 0.0

        // Base on score; tuned experimentally
        confidence += (best.score / 8.0).coerceIn(0.0, 1.0)

        if (hasOtpKeyword) confidence += 0.15
        if (hasSafetyKeyword) confidence += 0.15

        return confidence.coerceIn(0.0, 1.0)
    }
}
