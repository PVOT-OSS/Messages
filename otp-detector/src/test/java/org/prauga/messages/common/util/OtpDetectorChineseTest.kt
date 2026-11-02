/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.util

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class OtpDetectorChineseTest {

    private lateinit var detector: OtpDetector

    @Before
    fun setUp() {
        detector = OtpDetector(OtpResourceProviderImpl(RuntimeEnvironment.getApplication()))
    }

    private fun assertOtp(message: String, code: String) {
        val result = detector.detect(message)
        assertTrue("Should detect an OTP in '$message': ${result.reason}", result.isOtp)
        assertEquals(code, result.code)
    }

    private fun assertNotOtp(message: String) {
        val result = detector.detect(message)
        assertFalse("Should not detect an OTP in '$message': ${result.reason}", result.isOtp)
    }

    @Test
    fun givenSimplifiedChineseOtp_whenDetected_thenExtractsCode() {
        assertOtp("【某银行】您的验证码是123456，5分钟内有效，请勿泄露。", "123456")
    }

    @Test
    fun givenCodeAfterFullWidthColon_whenDetected_thenExtractsCode() {
        assertOtp("验证码：482913，用于登录。", "482913")
    }

    @Test
    fun givenCodeAfterWei_whenDetected_thenExtractsCode() {
        assertOtp("您的登录验证码为 7391，请勿告诉他人", "7391")
    }

    @Test
    fun givenTraditionalChineseOtp_whenDetected_thenExtractsCode() {
        assertOtp("您的驗證碼為 556677，請勿分享", "556677")
    }

    @Test
    fun givenPaymentOtpWithAmount_whenDetected_thenExtractsCodeNotAmount() {
        assertOtp("您正在支付100.00元，验证码864209，请勿泄露", "864209")
    }

    @Test
    fun givenEnglishSystemLanguage_whenChineseOtpDetected_thenExtractsCode() {
        assertOtp("动态码 902114，10分钟内有效", "902114")
    }

    @Test
    @Config(qualifiers = "zh-rCN")
    fun givenChineseSystemLanguage_whenEnglishOtpDetected_thenExtractsCode() {
        detector = OtpDetector(OtpResourceProviderImpl(RuntimeEnvironment.getApplication()))
        assertOtp("Your verification code is 314159. Do not share it.", "314159")
    }

    @Test
    fun givenChineseOrderNumber_whenDetected_thenNotOtp() {
        assertNotOtp("您的快递已发出，订单号12345678")
    }

    @Test
    fun givenChineseSpendingNotice_whenDetected_thenNotOtp() {
        assertNotOtp("您尾号1234的账户消费100元")
    }

    @Test
    fun givenCodeTouchingAsciiLetters_whenDetected_thenNotSplitOut() {
        assertNotOtp("Ticket REF20481 confirmed")
    }
}
