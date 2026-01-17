/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * Realm test helper for instrumented tests.
 */
package org.prauga.messages.testutil

import android.content.Context
import io.realm.Realm
import io.realm.RealmConfiguration
import java.util.UUID

/**
 * Helper class for setting up and tearing down Realm for instrumented tests.
 */
object RealmTestHelper {

    private var testConfiguration: RealmConfiguration? = null

    /**
     * Initializes Realm with an in-memory configuration for testing.
     * Each test run gets a unique database name to ensure isolation.
     */
    fun init(context: Context) {
        Realm.init(context)
        testConfiguration = RealmConfiguration.Builder()
            .inMemory()
            .name("test-realm-${UUID.randomUUID()}")
            .allowQueriesOnUiThread(true)
            .allowWritesOnUiThread(true)
            .build()
        Realm.setDefaultConfiguration(testConfiguration!!)
    }

    /**
     * Gets a Realm instance for testing.
     */
    fun getRealm(): Realm {
        return Realm.getDefaultInstance()
    }

    /**
     * Clears all data from the test Realm.
     */
    fun clearAll() {
        val realm = getRealm()
        realm.executeTransaction { r ->
            r.deleteAll()
        }
        realm.close()
    }

    /**
     * Closes all Realm instances and deletes the test database.
     */
    fun tearDown() {
        testConfiguration?.let { config ->
            Realm.deleteRealm(config)
        }
        testConfiguration = null
    }
}
