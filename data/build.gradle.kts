/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "org.prauga.messages.data"
    compileSdk { version = release(36) }

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    // androidx
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.documentfile)

    // glide
    implementation(libs.glide)
    kapt(libs.compiler)

    // dagger
    implementation(libs.dagger)
    implementation(libs.dagger.android.support)
    kapt(libs.dagger.android.processor)
    kapt(libs.dagger.compiler)

    implementation(libs.javax.annotation.api)

    // rxjava
    implementation(libs.rxandroid)
    implementation(libs.rxjava)
    implementation(libs.rxkotlin)

    // testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.runner)
    testImplementation(libs.androidx.test.core)

    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)

    // coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.rx2)
    implementation(libs.kotlinx.coroutines.reactive)

    implementation(libs.datashare)
    implementation(libs.rx.preferences)
    implementation(libs.timber)
    implementation(libs.moshi)
    kapt(libs.moshi.kotlin.codegen)
    implementation(libs.okhttp3.okhttp)
    implementation(libs.libphonenumber.android)
    implementation(libs.kotlin.stdlib)

    // work manager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.work.rxjava2)

    implementation(project(":android-smsmms"))
    implementation(project(":carrier-configs"))
    implementation(project(":mms-pdu"))
    implementation(project(":gif-encoder"))
    implementation(project(":common"))
    implementation(project(":domain"))
}
