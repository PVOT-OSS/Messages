/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.conversationinfo.injection

import dagger.Subcomponent
import org.prauga.messages.feature.conversationinfo.ConversationInfoController
import org.prauga.messages.injection.scope.ControllerScope

@ControllerScope
@Subcomponent(modules = [ConversationInfoModule::class])
interface ConversationInfoComponent {

    fun inject(controller: ConversationInfoController)

    @Subcomponent.Builder
    interface Builder {
        fun conversationInfoModule(module: ConversationInfoModule): Builder

        fun build(): ConversationInfoComponent
    }
}
