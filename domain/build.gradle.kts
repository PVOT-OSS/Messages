/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */

plugins {
    alias(libs.plugins.android.library)
    id("realm-android")   // must come before Kotlin plugins
    alias(libs.plugins.kotlin.android)
    id("kotlin-android-extensions")
    id("kotlin-kapt")
}

android {
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    defaultConfig {
        minSdk = 23
        targetSdk = 33
    }

    namespace = "dev.danascape.messages.domain"
}

dependencies {
    // androidx
    implementation(libs.androidx.documentfile)

    // dagger
    implementation(libs.dagger)
    kapt(libs.dagger.compiler)

    // Fix for javax annotations missing on jdk8+
    implementation(libs.jdk9.deps)
    implementation(libs.javax.annotation.api)

    // realm
    kapt(libs.realm.annotations)
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
}