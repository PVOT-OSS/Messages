/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.conversationinfo.injection

import dagger.Module
import dagger.Provides
import javax.inject.Named
import org.prauga.messages.feature.conversationinfo.ConversationInfoController
import org.prauga.messages.injection.scope.ControllerScope

@Module
class ConversationInfoModule(private val controller: ConversationInfoController) {

    @Provides @ControllerScope @Named("threadId") fun provideThreadId(): Long = controller.threadId
}
