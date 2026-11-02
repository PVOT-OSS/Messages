/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

plugins {
    alias(libs.plugins.pvot.android.library)
}

android {
    namespace = "org.prauga.messages.app"
    resourcePrefix = null

    buildFeatures { viewBinding = true }

    defaultConfig {
        minSdk = 24
    }

    testOptions { unitTests.isIncludeAndroidResources = true }
}

dependencies {
    implementation(libs.androidx.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.timber)

    // Test dependencies
    testImplementation(libs.junit)
    testImplementation(libs.androidx.runner)
    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.mockito.android)
}
