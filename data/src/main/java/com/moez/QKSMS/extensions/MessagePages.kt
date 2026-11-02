/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.extensions

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.Sort
import org.prauga.messages.model.Message

const val MESSAGE_PAGE_SIZE = 2_000L

/**
 * Runs [block] in its own transaction on the first page of [query] until nothing matches, then
 * [afterEach] once that page is committed. [block] must make every message it gets stop matching,
 * by changing or deleting it.
 */
fun Realm.drainMessages(
    query: Realm.() -> RealmQuery<Message>,
    afterEach: () -> Unit = {},
    block: Realm.(RealmResults<Message>) -> Unit,
) {
    while (true) {
        var size = 0
        executeTransaction { realm ->
            val page = realm.query().limit(MESSAGE_PAGE_SIZE).findAll()
            size = page.size
            if (size > 0) realm.block(page)
        }
        if (size == 0) return
        afterEach()
    }
}

/** Calls [block] on the matches of [query], oldest first, one page at a time. */
fun Realm.forEachMessagePage(
    query: Realm.() -> RealmQuery<Message>,
    block: (RealmResults<Message>) -> Unit,
) {
    var lastDate = 0L
    var lastId = 0L
    var first = true
    while (true) {
        val page =
            query()
                .apply {
                    if (!first) {
                        beginGroup()
                            .greaterThan("date", lastDate)
                            .or()
                            .beginGroup()
                            .equalTo("date", lastDate)
                            .greaterThan("id", lastId)
                            .endGroup()
                            .endGroup()
                    }
                }
                .sort(arrayOf("date", "id"), arrayOf(Sort.ASCENDING, Sort.ASCENDING))
                .limit(MESSAGE_PAGE_SIZE)
                .findAll()
        if (page.isEmpty()) return

        val last = page.last()!!
        lastDate = last.date
        lastId = last.id
        first = false
        block(page)
        if (page.size < MESSAGE_PAGE_SIZE) return
    }
}
