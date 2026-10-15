/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.injection.android

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.prauga.messages.feature.backup.RestoreBackupService
import org.prauga.messages.injection.scope.ActivityScope
import org.prauga.messages.receiver.SendSmsReceiver
import org.prauga.messages.service.AutoDeleteService
import org.prauga.messages.service.HeadlessSmsSendService

@Module
abstract class ServiceBuilderModule {

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindAutoDeleteService(): AutoDeleteService

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindHeadlessSmsSendService(): HeadlessSmsSendService

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindRestoreBackupService(): RestoreBackupService

    @ActivityScope @ContributesAndroidInjector() abstract fun bindSendSmsReceiver(): SendSmsReceiver
}
