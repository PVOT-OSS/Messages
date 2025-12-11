/*
 * Copyright (C) 2025
 *
 * This file is part of Messages.
 *
 * Messages is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Messages is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Messages.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.prauga.messages.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey

/**
 * 取件码解析结果模型
 */
data class ParcelCodeParseResult(
    val address: String,
    val code: String,
    val success: Boolean
)

/**
 * 取件码数据模型
 */
open class ParcelCode : RealmObject() {
    @PrimaryKey var id: Long = 0
    @Index var messageId: Long = 0
    var address: String = ""
    var code: String = ""
    var date: Long = System.currentTimeMillis()
    var processed: Boolean = false
    var source: String = "sms" // sms or notification
    var provider: String = ""
    var isActive: Boolean = true
}

/**
 * 自定义取件码解析规则
 */
open class ParcelCodeRule : RealmObject() {
    @PrimaryKey var id: Long = 0
    var name: String = ""
    var type: String = "address" // address or code
    var pattern: String = ""
    var isActive: Boolean = true
    var priority: Int = 0
}

/**
 * 取件码忽略关键词
 */
open class ParcelCodeIgnoreKeyword : RealmObject() {
    @PrimaryKey var id: Long = 0
    var keyword: String = ""
    var isActive: Boolean = true
}
