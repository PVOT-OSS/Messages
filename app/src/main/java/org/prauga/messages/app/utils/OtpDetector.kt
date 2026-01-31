/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.app.utils

import java.util.regex.Matcher
import java.util.regex.Pattern

data class ParcelDetectionResult(
    val address: String,
    val code: String,
    val success: Boolean,
    val reason: String
)

class ParcelDetector {
    // 使用正则表达式来匹配地址和取件码（1个或多个取件码）优先匹配快递柜
    private val lockerPattern: Pattern =
        Pattern.compile("""(?i)([0-9]+)号(?:柜|快递柜|丰巢柜|蜂巢柜|熊猫柜|兔喜快递柜)""")
    private val addressPattern: Pattern =
        Pattern.compile("""(?i)(地址|收货地址|送货地址|位于|放至|已到达|到达|已到|送达|到|已放入|已存放至|已存放|放入)[\s\S]*?([\w\s-]+?(?:门牌|驿站|,|，|。|$)\d*)""")
    private val codePattern: Pattern = Pattern.compile(
        """(?i)(取件码为|提货号为|取货码为|提货码为|取件码（|提货号（|取货码（|提货码（|取件码『|提货号『|取货码『|提货码『|取件码【|提货号【|取货码【|提货码【|取件码\(|提货号\(|取货码\(|提货码\(|取件码\[|提货号\[|取货码\[|提货码\[|取件码|提货号|取货码|提货码|凭|快递|京东|天猫|中通|顺丰|韵达|德邦|菜鸟|拼多多|EMS|闪送|美团|饿了么|盒马|叮咚买菜|UU跑腿|签收码|签收编号|操作码|提货编码|收货编码|签收编码|取件編號|提貨號碼|運單碼|快遞碼|快件碼|包裹碼|貨品碼)\s*[A-Za-z0-9\s-]{2,}(?:[，,、][A-Za-z0-9\s-]{2,})*"""
    )



    fun detectParcel(sms: String): ParcelDetectionResult {
        var foundAddress = ""
        var foundCode = ""

        // 优先匹配柜号地址，其次默认规则
        val lockerMatcher: Matcher = lockerPattern.matcher(sms)
        foundAddress = if (lockerMatcher.find()) lockerMatcher.group().toString() ?: "" else ""

        if (foundAddress.isEmpty()) {
            val addressMatcher: Matcher = addressPattern.matcher(sms)
            var longestAddress = ""
            while (addressMatcher.find()) {
                val currentAddress = addressMatcher.group(2)?.toString() ?: ""
                if (currentAddress.length > longestAddress.length) {
                    longestAddress = currentAddress
                }
            }
            foundAddress = longestAddress
        }

        val codeMatcher: Matcher = codePattern.matcher(sms)
        while (codeMatcher.find()) {
            val match = codeMatcher.group(0)
            // 进一步将匹配到的内容按分隔符拆分成单个取件码
            val codes = match?.split(Regex("[，,、]"))
            foundCode = codes?.joinToString(", ") { it.trim() } ?: ""
            foundCode = foundCode.replace(Regex("[^A-Za-z0-9-, ]"), "")
        }
        foundAddress = foundAddress.replace(Regex("[,，。]"), "")  // 移除所有标点和符号
        foundAddress = foundAddress.replace("取件", "")  // 移除"取件"

        val success = foundAddress.isNotEmpty() && foundCode.isNotEmpty()
        val reason = if (success) {
            "Successfully detected parcel: address='$foundAddress', code='$foundCode'"
        } else {
            when {
                foundAddress.isEmpty() && foundCode.isEmpty() -> "No address and no code found"
                foundAddress.isEmpty() -> "No address found"
                else -> "No code found"
            }
        }

        return ParcelDetectionResult(foundAddress, foundCode, success, reason)
    }


}
