/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

plugins {
    alias(libs.plugins.pvot.android.library)
}

android {
    namespace = "org.prauga.messages.otp"
    resourcePrefix = null

    defaultConfig { minSdk = 24 }

    testOptions { unitTests.isIncludeAndroidResources = true }

    lint { abortOnError = false }
}

dependencies {
    implementation(libs.androidx.ktx)
    implementation(libs.timber)
    implementation(libs.kotlin.stdlib)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
