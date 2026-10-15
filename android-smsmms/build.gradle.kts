/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.klinker.android.send_message"
    compileSdk { version = release(36) }

    defaultConfig { minSdk = 23 }

    lint { abortOnError = false }
}

dependencies {
    implementation(libs.androidx.ktx)
    implementation(libs.timber)
    implementation(libs.kotlin.stdlib)

    implementation(project(":carrier-configs"))

    // testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.test.core)
}
