/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.prauga.messages.common

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import com.moez.QKSMS.manager.SpeakManager
import com.uber.rxdogtag.RxDogTag
import com.uber.rxdogtag.autodispose.AutoDisposeConfigurer
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.plugins.RxJavaPlugins
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.prauga.messages.R
import org.prauga.messages.app.utils.AppUtil.applyEdgeToEdgeInsets
import org.prauga.messages.common.util.FileLoggingTree
import org.prauga.messages.injection.AppComponentManager
import org.prauga.messages.injection.appComponent
import org.prauga.messages.interactor.SpeakThreads
import org.prauga.messages.manager.BillingManager
import org.prauga.messages.manager.ReferralManager
import org.prauga.messages.migration.QkMigration
import org.prauga.messages.migration.QkRealmMigration
import org.prauga.messages.util.NightModeManager
import org.prauga.messages.worker.HousekeepingWorker
import timber.log.Timber
import javax.inject.Inject

class QKApplication : Application(), HasAndroidInjector {

    /**
     * Inject these so that they are forced to initialize
     */
    @Suppress("unused")
    @Inject
    lateinit var qkMigration: QkMigration

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var fileLoggingTree: FileLoggingTree

    @Inject
    lateinit var nightModeManager: NightModeManager

    @Inject
    lateinit var realmMigration: QkRealmMigration

    @Inject
    lateinit var referralManager: ReferralManager

    @Inject
    lateinit var workerFactory: WorkerFactory

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycle()

        // set application context for SpeakManager
        SpeakManager.setContext(this)

        // set translated "no messages" string for speakThreads interactor
        SpeakThreads.setNoMessagesString(getString(R.string.speak_no_messages))

        AppComponentManager.init(this)
        appComponent.inject(this)

        Realm.init(this)
        Realm.setDefaultConfiguration(
            RealmConfiguration.Builder()
                .compactOnLaunch()
                .migration(realmMigration)
                .schemaVersion(QkRealmMigration.SchemaVersion)
                .build()
        )

        qkMigration.performMigration()

        GlobalScope.launch(Dispatchers.IO) {
            referralManager.trackReferrer()
            billingManager.checkForPurchases()
            billingManager.queryProducts()
        }

        nightModeManager.updateCurrentTheme()

        // configure timber logging
        Timber.plant(Timber.DebugTree(), fileLoggingTree)

        // configure emoji compatibility with bundled package
        // (bundled library works with no play-services/gsm os versions)
        EmojiCompat.init(
            BundledEmojiCompatConfig(this)
                .registerInitCallback(object : EmojiCompat.InitCallback() {
                    override fun onInitialized() {
                        super.onInitialized()
                        Timber.v("bundled emojicompat initialized")
                    }

                    override fun onFailed(throwable: Throwable?) {
                        super.onFailed(throwable)
                        Timber.e("bundled emojicompat initialization failed")
                    }
                })
        )

        // Handle undeliverable RxJava exceptions
        RxJavaPlugins.setErrorHandler { e ->
            Timber.w(e, "Undeliverable RxJava exception")
        }

        // rxdogtag provides 'look-back' for exceptions in rxjava2 'chains'
        RxDogTag.builder()
            .configureWith(AutoDisposeConfigurer::configure)
            .install()

        // init work manager with custom factory supporting dagger/injection capability
        WorkManager.initialize(
            this,
            Configuration.Builder().setWorkerFactory(workerFactory).build()
        )

        // register, or re-register, housekeeping work manager
        HousekeepingWorker.register(applicationContext)
    }

    private fun registerActivityLifecycle() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(
                activity: Activity,
                savedInstanceState: Bundle?
            ) {
                activity.applyEdgeToEdgeInsets()
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return dispatchingAndroidInjector
    }
}