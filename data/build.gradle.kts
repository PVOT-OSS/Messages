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
    alias(libs.plugins.kotlin.android)
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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    namespace = "dev.danascape.messages.data"
}

dependencies {
    // androidx
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(libs.androidx.ktx)
    implementation("androidx.exifinterface:exifinterface:1.0.0")
    implementation("androidx.documentfile:documentfile:1.0.1")

    // glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // dagger
    implementation("com.google.dagger:dagger:2.18")
    implementation("com.google.dagger:dagger-android-support:2.18")
    kapt("com.google.dagger:dagger-android-processor:2.18")
    kapt("com.google.dagger:dagger-compiler:2.18")

    // Resolve jdk8+ Generation Annotations - javax annotation does not exist
    implementation("com.github.pengrad:jdk9-deps:1ffe84c468")
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    // rxjava
    implementation("io.reactivex.rxjava2:rxandroid:2.0.1")
    implementation("io.reactivex.rxjava2:rxjava:2.1.4")
    implementation("io.reactivex.rxjava2:rxkotlin:2.1.0")

    // testing
    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.0-alpha3") {
        exclude(group = "com.android.support", module = "support-annotations")
    }

    androidTestImplementation("org.mockito:mockito-android:2.18.3")
    testImplementation("androidx.test:runner:1.1.0-alpha3")
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:2.18.3")

    // coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:1.4.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.4.3")

    implementation("com.callcontrol:datashare:1.3.0")
    implementation("com.f2prateek.rx.preferences2:rx-preferences:2.0.0-RC3")
    implementation(libs.timber)
    implementation("com.squareup.moshi:moshi:1.8.0")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.8.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("io.michaelrocks:libphonenumber-android:8.13.47")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.21")

    // work manager
    implementation("androidx.work:work-runtime-ktx:2.8.0")
    implementation("androidx.work:work-rxjava2:2.8.0")

    implementation(project(":android-smsmms"))
    implementation(project(":common"))
    implementation(project(":domain"))
}