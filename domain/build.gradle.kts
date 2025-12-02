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
    compileSdk = 34

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
    implementation("androidx.documentfile:documentfile:1.0.1")

    // dagger
    implementation("com.google.dagger:dagger:2.18")
    kapt("com.google.dagger:dagger-compiler:2.18")

    // Fix for javax annotations missing on jdk8+
    implementation("com.github.pengrad:jdk9-deps:1ffe84c468")
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    // realm
    kapt("io.realm:realm-annotations:10.15.0")
    kapt("io.realm:realm-annotations-processor:10.15.0")

    // rxjava
    implementation("io.reactivex.rxjava2:rxandroid:2.0.1")
    implementation("io.reactivex.rxjava2:rxjava:2.1.4")
    implementation("io.reactivex.rxjava2:rxkotlin:2.1.0")

    // coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:1.4.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.4.3")

    implementation(libs.androidx.ktx)
    implementation("com.f2prateek.rx.preferences2:rx-preferences:2.0.0-RC3")
    implementation(libs.timber)
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.21")

    implementation(project(":common"))
}