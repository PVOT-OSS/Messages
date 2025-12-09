package org.prauga.messages.util

import android.content.SharedPreferences
import io.reactivex.rxjava3.core.Observable

/**
 * Lightweight replacement for rx-preferences2 that exposes imperative get/set plus an Observable
 * stream of changes for a single key.
 */
class Preference<T : Any>(
    private val prefs: SharedPreferences,
    private val key: String,
    private val defaultValue: T,
    private val serializer: Serializer<T>? = null
) {
    interface Serializer<T> {
        fun serialize(value: T): String
        fun deserialize(serialized: String): T
    }

    val isSet: Boolean
        get() = prefs.contains(key)

    fun get(): T {
        @Suppress("UNCHECKED_CAST")
        return when (defaultValue) {
            is Boolean -> prefs.getBoolean(key, defaultValue) as T
            is Int -> prefs.getInt(key, defaultValue) as T
            is Long -> prefs.getLong(key, defaultValue) as T
            is Float -> prefs.getFloat(key, defaultValue) as T
            is String -> prefs.getString(key, defaultValue) as T
            is Set<*> -> prefs.getStringSet(key, defaultValue as Set<String>) as T
            else -> {
                val ser = serializer
                    ?: error("Serializer required for type ${defaultValue!!::class.java}")
                val stored = prefs.getString(key, null)
                if (stored == null) defaultValue else ser.deserialize(stored)
            }
        }
    }

    fun set(value: T) {
        with(prefs.edit()) {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is String -> putString(key, value)
                is Set<*> -> @Suppress("UNCHECKED_CAST") putStringSet(key, value as Set<String>)
                else -> {
                    val ser = serializer
                        ?: error("Serializer required for type ${value!!::class.java}")
                    putString(key, ser.serialize(value))
                }
            }
            apply()
        }
    }

    fun delete() {
        prefs.edit().remove(key).apply()
    }

    fun asObservable(): Observable<T> = Observable.create { emitter ->
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == null || changedKey == key) {
                emitter.onNext(get())
            }
        }

        emitter.setCancellable { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        emitter.onNext(get())
    }.share()
}
