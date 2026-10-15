/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "org.prauga.messages.carrier"
    compileSdk { version = release(36) }

    defaultConfig { minSdk = 23 }

    lint { abortOnError = false }
}

dependencies {
    implementation(libs.androidx.ktx)
    implementation(libs.timber)
    implementation(libs.kotlin.stdlib)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.test.core)
}
