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
package org.prauga.messages.repository.impl

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.prauga.messages.model.ParcelCode
import org.prauga.messages.model.ParcelCodeIgnoreKeyword
import org.prauga.messages.model.ParcelCodeRule
import org.prauga.messages.repository.ParcelCodeRepository
import io.realm.Realm
import io.realm.Sort
import timber.log.Timber

/**
 * 取件码仓库实现类
 * 基于Realm直接访问模式
 */
class ParcelCodeRepositoryImpl : ParcelCodeRepository {
    
    override fun saveParcelCode(parcelCode: ParcelCode) {
        Realm.getDefaultInstance().use {
            it.executeTransaction {\ realm ->
                val nextId = realm.where(ParcelCode::class.java).max("id")?.toLong() ?: 0L + 1
                parcelCode.id = nextId
                realm.copyToRealmOrUpdate(parcelCode)
                Timber.d("Saved parcel code: ${parcelCode.code} for message ${parcelCode.messageId}")
            }
        }
    }

    override fun deleteParcelCode(id: Long) {
        Realm.getDefaultInstance().use {
            it.executeTransaction {\ realm ->
                realm.where(ParcelCode::class.java)
                    .equalTo("id", id)
                    .findAll()
                    .deleteAllFromRealm()
            }
        }
    }

    override fun getActiveParcelCodes(): Observable<List<ParcelCode>> {
        return Observable.create { emitter ->
            val realm = Realm.getDefaultInstance()
            val results = realm.where(ParcelCode::class.java)
                .equalTo("isActive", true)
                .sort("date", Sort.DESCENDING)
                .findAll()

            emitter.onNext(realm.copyFromRealm(results))

            // 添加监听，实时更新
            results.addChangeListener {\ newResults ->
                emitter.onNext(realm.copyFromRealm(newResults))
            }

            emitter.setCancellable {
                results.removeAllChangeListeners()
                realm.close()
            }
        }.subscribeOn(Schedulers.io())
    }

    override fun getParcelCodeByMessageId(messageId: Long): ParcelCode? {
        Realm.getDefaultInstance().use {
            val result = it.where(ParcelCode::class.java)
                .equalTo("messageId", messageId)
                .equalTo("isActive", true)
                .findFirst()
            return if (result != null) it.copyFromRealm(result) else null
        }
    }

    override fun saveParcelCodes(parcelCodes: List<ParcelCode>) {
        Realm.getDefaultInstance().use {
            it.executeTransaction {\ realm ->
                for (parcelCode in parcelCodes) {
                    val nextId = realm.where(ParcelCode::class.java).max("id")?.toLong() ?: 0L + 1
                    parcelCode.id = nextId
                    realm.copyToRealmOrUpdate(parcelCode)
                }
            }
        }
    }

    override fun deactivateParcelCode(id: Long) {
        Realm.getDefaultInstance().use {
            it.executeTransaction {\ realm ->
                realm.where(ParcelCode::class.java)
                    .equalTo("id", id)
                    .findFirst()?.let {\ parcelCode ->
                        parcelCode.isActive = false
                    }
            }
        }
    }

    override fun clearAllParcelCodes() {
        Realm.getDefaultInstance().use {
            it.executeTransaction {\ realm ->
                realm.where(ParcelCode::class.java).findAll().deleteAllFromRealm()
            }
        }
    }

    override fun addParcelCodeRule(rule: ParcelCodeRule) {
        Realm.getDefaultInstance().use {
            it.executeTransaction {\ realm ->
                val nextId = realm.where(ParcelCodeRule::class.java).max("id")?.toLong() ?: 0L + 1
                rule.id = nextId
                realm.copyToRealmOrUpdate(rule)
            }
        }
    }

    override fun updateParcelCodeRule(rule: ParcelCodeRule) {
        Realm.getDefaultInstance().use {
            it.executeTransaction {\ realm ->
                realm.copyToRealmOrUpdate(rule)
            }
        }
    }

    override fun deleteParcelCodeRule(id: Long) {
        Realm.getDefaultInstance().use {
            it.executeTransaction {\ realm ->
                realm.where(ParcelCodeRule::class.java)
                    .equalTo("id", id)
                    .findAll()
                    .deleteAllFromRealm()
            }
        }
    }

    override fun getActiveParcelCodeRules(): List<ParcelCodeRule> {
        Realm.getDefaultInstance().use {
            val results = it.where(ParcelCodeRule::class.java)
                .equalTo("isActive", true)
                .sort("priority", Sort.ASCENDING)
                .findAll()
            return it.copyFromRealm(results)
        }
    }

    override fun getParcelCodeRulesByType(type: String): List<ParcelCodeRule> {
        Realm.getDefaultInstance().use {
            val results = it.where(ParcelCodeRule::class.java)
                .equalTo("type", type)
                .equalTo("isActive", true)
                .sort("priority", Sort.ASCENDING)
                .findAll()
            return it.copyFromRealm(results)
        }
    }

    override fun addIgnoreKeyword(keyword: ParcelCodeIgnoreKeyword) {
        Realm.getDefaultInstance().use {
            it.executeTransaction {\ realm ->
                val nextId = realm.where(ParcelCodeIgnoreKeyword::class.java).max("id")?.toLong() ?: 0L + 1
                keyword.id = nextId
                realm.copyToRealmOrUpdate(keyword)
            }
        }
    }

    override fun updateIgnoreKeyword(keyword: ParcelCodeIgnoreKeyword) {
        Realm.getDefaultInstance().use {
            it.executeTransaction {\ realm ->
                realm.copyToRealmOrUpdate(keyword)
            }
        }
    }

    override fun deleteIgnoreKeyword(id: Long) {
        Realm.getDefaultInstance().use {
            it.executeTransaction {\ realm ->
                realm.where(ParcelCodeIgnoreKeyword::class.java)
                    .equalTo("id", id)
                    .findAll()
                    .deleteAllFromRealm()
            }
        }
    }

    override fun getActiveIgnoreKeywords(): List<ParcelCodeIgnoreKeyword> {
        Realm.getDefaultInstance().use {
            val results = it.where(ParcelCodeIgnoreKeyword::class.java)
                .equalTo("isActive", true)
                .findAll()
            return it.copyFromRealm(results)
        }
    }

    override fun containsIgnoreKeyword(keyword: String): Boolean {
        Realm.getDefaultInstance().use {
            val count = it.where(ParcelCodeIgnoreKeyword::class.java)
                .equalTo("keyword", keyword)
                .equalTo("isActive", true)
                .count()
            return count > 0
        }
    }
}
