/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "org.prauga.messages.telephony"
    compileSdk { version = release(36) }

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    lint { abortOnError = false }
}

dependencies {
    implementation(libs.androidx.ktx)
    implementation(libs.timber)
    implementation(libs.kotlin.stdlib)
    implementation(libs.libphonenumber.android)

    implementation(libs.dagger)
    kapt(libs.dagger.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.runner)
}
