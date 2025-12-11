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
package org.prauga.messages.repository

import io.reactivex.Observable
import org.prauga.messages.model.ParcelCode
import org.prauga.messages.model.ParcelCodeIgnoreKeyword
import org.prauga.messages.model.ParcelCodeRule

/**
 * 取件码相关数据访问接口
 */
interface ParcelCodeRepository {
    /**
     * 保存取件码
     */
    fun saveParcelCode(parcelCode: ParcelCode)

    /**
     * 删除取件码
     */
    fun deleteParcelCode(id: Long)

    /**
     * 获取所有活动状态的取件码
     */
    fun getActiveParcelCodes(): Observable<List<ParcelCode>>

    /**
     * 获取指定消息ID的取件码
     */
    fun getParcelCodeByMessageId(messageId: Long): ParcelCode?

    /**
     * 批量保存取件码
     */
    fun saveParcelCodes(parcelCodes: List<ParcelCode>)

    /**
     * 标记取件码为非活动状态
     */
    fun deactivateParcelCode(id: Long)

    /**
     * 清空所有取件码
     */
    fun clearAllParcelCodes()

    /**
     * 添加自定义解析规则
     */
    fun addParcelCodeRule(rule: ParcelCodeRule)

    /**
     * 更新自定义解析规则
     */
    fun updateParcelCodeRule(rule: ParcelCodeRule)

    /**
     * 删除自定义解析规则
     */
    fun deleteParcelCodeRule(id: Long)

    /**
     * 获取所有活动状态的自定义规则
     */
    fun getActiveParcelCodeRules(): List<ParcelCodeRule>

    /**
     * 获取指定类型的自定义规则
     */
    fun getParcelCodeRulesByType(type: String): List<ParcelCodeRule>

    /**
     * 添加忽略关键词
     */
    fun addIgnoreKeyword(keyword: ParcelCodeIgnoreKeyword)

    /**
     * 更新忽略关键词
     */
    fun updateIgnoreKeyword(keyword: ParcelCodeIgnoreKeyword)

    /**
     * 删除忽略关键词
     */
    fun deleteIgnoreKeyword(id: Long)

    /**
     * 获取所有活动状态的忽略关键词
     */
    fun getActiveIgnoreKeywords(): List<ParcelCodeIgnoreKeyword>

    /**
     * 检查是否包含忽略关键词
     */
    fun containsIgnoreKeyword(keyword: String): Boolean
}
