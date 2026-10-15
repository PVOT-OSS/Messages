/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class ScheduledMessage(
    @PrimaryKey var id: Long = 0,
    var date: Long = 0,
    var subId: Int = -1,
    var recipients: RealmList<String> = RealmList(),
    var sendAsGroup: Boolean = true,
    var body: String = "",
    var attachments: RealmList<String> = RealmList(),
    var conversationId: Long = 0,
) : RealmObject() {

    fun copy(
        id: Long = this.id,
        date: Long = this.date,
        subId: Int = this.subId,
        recipients: RealmList<String> = this.recipients,
        sendAsGroup: Boolean = this.sendAsGroup,
        body: String = this.body,
        attachments: RealmList<String> = this.attachments,
        conversationId: Long = this.conversationId,
    ): ScheduledMessage {

        return ScheduledMessage(
            id,
            date,
            subId,
            recipients,
            sendAsGroup,
            body,
            attachments,
            conversationId,
        )
    }
}
