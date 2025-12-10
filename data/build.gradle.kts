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
    namespace = "org.prauga.messages.data"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    // androidx
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.documentfile)

    // glide
    implementation(libs.glide)
    kapt(libs.compiler)

    // dagger
    implementation(libs.dagger)
    implementation(libs.dagger.android.support)
    kapt(libs.dagger.android.processor)
    kapt(libs.dagger.compiler)

    implementation(libs.javax.annotation.api)

    // rxjava
    implementation(libs.rxandroid)
    implementation(libs.rxjava)
    implementation(libs.rxkotlin)

    // testing
    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.0-alpha3") {
        exclude(group = "com.android.support", module = "support-annotations")
    }

    androidTestImplementation(libs.mockito.android)
    testImplementation(libs.androidx.runner)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)

    // coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.rx2)
    implementation(libs.kotlinx.coroutines.reactive)

    implementation(libs.datashare)
    implementation(libs.rx.preferences)
    implementation(libs.timber)
    implementation(libs.moshi)
    kapt(libs.moshi.kotlin.codegen)
    implementation(libs.okhttp3.okhttp)
    implementation(libs.libphonenumber.android)
    implementation(libs.kotlin.stdlib)

    // work manager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.work.rxjava2)

    implementation(project(":android-smsmms"))
    implementation(project(":common"))
    implementation(project(":domain"))
}
