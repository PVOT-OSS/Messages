/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

plugins { alias(libs.plugins.pvot.android.library) }

android {
    namespace = "com.google.android.mms"

    defaultConfig { minSdk = 24 }

    testFixtures { enable = true }

    lint { abortOnError = false }
}

dependencies {
    implementation(libs.timber)
    implementation(libs.kotlin.stdlib)
    testFixturesImplementation(libs.kotlin.stdlib)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
}
