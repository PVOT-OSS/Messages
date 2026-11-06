/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

plugins { alias(libs.plugins.pvot.android.library) }

android {
    namespace = "com.klinker.android.send_message"
    resourcePrefix = null

    defaultConfig { minSdk = 24 }

    lint { abortOnError = false }
}

dependencies {
    implementation(libs.androidx.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.timber)
    implementation(libs.kotlin.stdlib)

    implementation(project(":carrier-configs"))
    implementation(project(":mms-pdu"))
    implementation(project(":telephony"))

    // testing
    testImplementation(libs.junit)
    testImplementation(testFixtures(project(":mms-pdu")))
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.test.core)
}
