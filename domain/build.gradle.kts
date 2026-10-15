/*
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

plugins {
    alias(libs.plugins.android.library)
    id("realm-android") // must come before Kotlin plugins
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "org.prauga.messages.domain"
    compileSdk { version = release(36) }

    defaultConfig { minSdk = 23 }
}

dependencies {
    // androidx
    implementation(libs.androidx.documentfile)

    // dagger
    implementation(libs.dagger)
    kapt(libs.dagger.compiler)

    implementation(libs.javax.annotation.api)

    // realm
    implementation(libs.realm.android.library)
    kapt(libs.realm.annotations.processor)

    // rxjava
    implementation(libs.rxandroid)
    implementation(libs.rxjava)
    implementation(libs.rxkotlin)

    // coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.rx2)
    implementation(libs.kotlinx.coroutines.reactive)

    implementation(libs.androidx.ktx)
    implementation(libs.rx.preferences)
    implementation(libs.timber)
    implementation(libs.kotlin.stdlib)

    implementation(project(":common"))

    // testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)
}
