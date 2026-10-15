/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.compose

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import javax.inject.Named
import org.prauga.messages.injection.ViewModelKey
import org.prauga.messages.model.Attachment

@Module
class ComposeActivityModule {

    @Provides
    @Named("query")
    fun provideQuery(activity: ComposeActivity): String =
        activity.intent.extras?.getString("query") ?: ""

    @Provides
    @Named("threadId")
    fun provideThreadId(activity: ComposeActivity): Long =
        activity.intent.extras?.getLong("threadId") ?: 0L

    @Provides
    @Named("messageId")
    fun provideMessageId(activity: ComposeActivity): Long =
        activity.intent.extras?.getLong("messageId") ?: 0L

    @Provides
    @Named("addresses")
    fun provideAddresses(activity: ComposeActivity): List<String> =
        if ((activity.intent?.data?.scheme == "sms") || (activity.intent?.data?.scheme == "smsto"))
            activity.intent
                ?.data
                ?.schemeSpecificPart
                ?.removeSuffix("?${activity.intent?.data?.query}")
                ?.split(",", ";")
                ?.filter { it.isNotEmpty() } ?: listOf()
        else listOf()

    @Provides
    @Named("text")
    fun provideSharedText(activity: ComposeActivity): String {
        var retVal = StringBuilder()

        // from subject, if passed in intent
        retVal.append(activity.intent?.getStringExtra(Intent.EXTRA_SUBJECT) ?: "")
        if (retVal.isNotEmpty()) retVal.append("\n")

        // from extra_text or sms_body extras, if passed in intent
        retVal.append(
            activity.intent?.extras?.getString(Intent.EXTRA_TEXT)
                ?: activity.intent?.extras?.getString("sms_body")
                ?: ""
        )

        // from body param value(s) if intent data uri is like
        // sms:12345678?body=hello%20there&body=goodbye
        if ((activity.intent?.data?.scheme == "sms") || (activity.intent?.data?.scheme == "smsto"))
            retVal.append(
                activity.intent
                    ?.data
                    ?.query
                    ?.split("&")
                    ?.filter { it.startsWith("body=") }
                    ?.joinToString("\n") { it.removePrefix("body=") } ?: ""
            )

        return retVal.toString()
    }

    @Provides
    @Named("attachments")
    fun provideSharedAttachments(activity: ComposeActivity): List<Attachment> {
        val uris = mutableListOf<Uri>()
        activity.intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.run(uris::add)
        activity.intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.run(uris::addAll)

        return uris.mapNotNull { Attachment(activity, it) }
    }

    @Provides
    @Named("mode")
    fun provideSharedAction(activity: ComposeActivity): String =
        activity.intent.getStringExtra("mode") ?: ""

    @Provides
    @Named("subscriptionId")
    fun provideSubscriptionId(activity: ComposeActivity): Int =
        activity.intent.getIntExtra("subscriptionId", -1)

    @Provides
    @Named("sendAsGroup")
    fun provideSendAsGroup(activity: ComposeActivity): Boolean? =
        if (!activity.intent.hasExtra("sendAsGroup")) null
        else activity.intent.getBooleanExtra("sendAsGroup", false)

    @Provides
    @Named("scheduleDateTime")
    fun provideSharedScheduleDateTime(activity: ComposeActivity): Long =
        activity.intent.getLongExtra("scheduleDateTime", 0L)

    @Provides
    @IntoMap
    @ViewModelKey(ComposeViewModel::class)
    fun provideComposeViewModel(viewModel: ComposeViewModel): ViewModel = viewModel
}
