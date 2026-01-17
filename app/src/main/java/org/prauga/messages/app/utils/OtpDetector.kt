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

class OtpDetector {

    private val otpKeywords = listOf(
        "otp",
        "one time password",
        "one-time password",
        "verification code",
        "verification number",
        "login code",
        "login otp",
        "security code",
        "2-step code",
        "2 factor code",
        "2fa code",
        "mfa code",
        "auth code",
        "passcode",
        "access code",
        "reset code",
        "transaction code",
        "confirm code",
        "confirmation code",
        "code",
        "验证码",
        "登录码",
        "安全码",
        "校验码",
        "密码",
        "动态码",
        "一次性密码",
        "授权码",
        "访问码",
        "重置码",
        "交易码",
        "确认码",
        "认证码",
        "OTP",
        "2FA",
        "MFA"
    ).map { it.lowercase() }

    private val safetyKeywords = listOf(
        "do not share",
        "don't share",
        "never share",
        "do not disclose",
        "do not forward",
        "keep this code secret",
        "valid for",
        "expires in",
        "expires within",
        "expires after",
        "请勿分享",
        "不要分享",
        "切勿分享",
        "请勿透露",
        "请勿转发",
        "保密",
        "有效时间",
        "有效期为",
        "将在",
        "内过期",
        "后过期"
    ).map { it.lowercase() }

    private val moneyIndicators = listOf(
        "rs", "inr", "usd", "eur", "gbp", "₹", "$", "€", "£", "balance",
        "amount", "debited", "credited", "txn", "transaction id", "order id",
        "人民币", "元", "¥", "金额", "余额", "转入", "转出", "交易", "订单", "交易号", "订单号",
        "已扣除", "已入账", "支付", "收款"
    ).map { it.lowercase() }

    fun detect(rawMessage: String): OtpDetectionResult {
        val message = rawMessage.trim()
        if (message.isEmpty()) {
            return OtpDetectionResult(false, null, 0.0, "Empty message")
        }

        val normalized = normalizeWhitespace(message)
        val lower = normalized.lowercase()

        val hasOtpKeyword = otpKeywords.any { lower.contains(it) }
        val hasSafetyKeyword = safetyKeywords.any { lower.contains(it) }

        val candidates = extractCandidates(normalized)

        if (candidates.isEmpty()) {
            val reason = if (hasOtpKeyword) {
                "Contains OTP-like keywords but no numeric/alphanumeric candidate code found"
            } else {
                "No OTP-like keywords and no candidate code found"
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
        input.replace(Regex("\\s+"), " ").trim()

    private fun extractCandidates(message: String): List<Candidate> {
        val candidates = mutableMapOf<String, Candidate>()

        val patterns = listOf(
            PatternInfo(
                Regex("(\\p{IsHan}+)(\\d{3,10})"),
                2,
                true,
                1.5
            ),
            PatternInfo(
                Regex("(^|\\s|\\p{P}|\\p{IsHan})(\\d{3,10})(\\p{P}|\\s|$|\\p{IsHan})"),
                2,
                true,
                1.0
            ),
            PatternInfo(
                Regex("(^|\\s|\\p{P}|\\p{IsHan})(\\d{2,4}[\\s-]\\d{2,4})([\\s-]\\d{2,4})*(\\p{P}|\\s|$|\\p{IsHan})"),
                2,
                true,
                2.0
            ),
            PatternInfo(
                Regex("(^|\\s|\\p{P}|\\p{IsHan})([0-9A-Za-z]{4,10})(\\p{P}|\\s|$|\\p{IsHan})"),
                2,
                false,
                0.8
            )
        )

        for (patternInfo in patterns) {
            patternInfo.regex.findAll(message).forEach { match ->
                val code = match.groupValues[patternInfo.groupIndex]
                val normalizedCode = code.replace("[\\s-]".toRegex(), "")

                if (isValidCandidate(normalizedCode, patternInfo.isNumeric)) {
                    val startIndex = match.range.first + match.groupValues[1].length
                    val endIndex = startIndex + code.length - 1

                    val existing = candidates[normalizedCode]
                    if (existing == null || patternInfo.priority > existing.score) {
                        candidates[normalizedCode] = Candidate(
                            code = normalizedCode,
                            startIndex = startIndex,
                            endIndex = endIndex,
                            isNumeric = patternInfo.isNumeric,
                            score = patternInfo.priority
                        )
                    }
                }
            }
        }

        return candidates.values.toList()
    }

    private data class PatternInfo(
        val regex: Regex,
        val groupIndex: Int,
        val isNumeric: Boolean,
        val priority: Double
    )

    private fun isValidCandidate(code: String, isNumeric: Boolean): Boolean {
        val length = code.length

        if (isNumeric) {
            return length in 3..10
        } else {
            return length in 4..10 &&
                    code.any { it.isDigit() } &&
                    code.count { it.isDigit() } >= 2 &&
                    !code.all { it.isDigit() }
        }
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
        val lineLower = lineInfo.line.lowercase()

        // If the line is mostly just the code -> strong hint
        val trimmedLine = lineInfo.line.trim()
        if (trimmedLine == candidate.code) {
            score += 2.5
        }

        // Typical OTP line patterns - support both English and Chinese
        if (Regex(
                "(otp|code|password|passcode|验证码|登录码|安全码|校验码|动态码|密码|一次性密码|授权码|认证码)",
                RegexOption.IGNORE_CASE
            ).containsMatchIn(lineInfo.line)
        ) {
            score += 2.0
        }

        // Support both English and Chinese separators
        if (Regex("(:|is|=|是|为|：)\\s*${Regex.escape(candidate.code)}").containsMatchIn(lineInfo.line)) {
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
