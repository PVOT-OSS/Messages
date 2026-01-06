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
    alias(libs.plugins.android.application)
    id("realm-android")   // must come before Kotlin plugins
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "org.prauga.messages"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "org.prauga.messages"
        minSdk = 23
        targetSdk = 36

        versionCode = 7
        versionName = "1.0.6"

        setProperty("archivesBaseName", "Messages-v${versionName}")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    signingConfigs {
        create("platform") {
            storeFile = file("$rootDir/keystore/platform.jks")
            storePassword = "platform"
            keyAlias = "platform"
            keyPassword = "platform"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("platform")
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("platform")
        }
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs and AABs.
        includeInApk = false
        includeInBundle = false
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    // lifecycle
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.common.java8)
    implementation(libs.lifecycle.extensions)

    // androidx
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.emoji2.bundled)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.ktx)
    implementation(libs.androidx.viewpager2)
    implementation(libs.material)
    implementation(libs.androidx.work.runtime.ktx)

    // conductor
    implementation(libs.conductor)
    implementation(libs.conductor.archlifecycle)

    // glide
    implementation(libs.glide)
    kapt(libs.compiler)

    // exoplayer
    implementation(libs.exoplayer.core)
    implementation("com.github.google.ExoPlayer:exoplayer-ui:r2.9.0") {
        exclude(mapOf("group" to "com.android.support", "module" to "support-media-compat"))
    }

    // rxbinding
    implementation(libs.rxbinding.kotlin)
    implementation(libs.rxbinding.support.v4.kotlin)

    // autodispose
    implementation(libs.autodispose.android.archcomponents)
    implementation(libs.autodispose.android.archcomponents.test)
    implementation(libs.autodispose.android)
    implementation(libs.autodispose)
    implementation(libs.autodispose.lifecycle)

    // dagger
    implementation(libs.dagger)
    implementation(libs.dagger.android.support)
    kapt(libs.dagger.compiler)
    kapt(libs.dagger.android.processor)

    implementation(libs.javax.annotation.api)

    // ezvcard (multiple excludes)
    implementation("com.googlecode.ez-vcard:ez-vcard:0.10.4") {
        exclude(mapOf("group" to "org.jsoup", "module" to "jsoup"))
        exclude(mapOf("group" to "org.freemarker", "module" to "freemarker"))
        exclude(mapOf("group" to "com.fasterxml.jackson.core", "module" to "jackson-core"))
    }

    // realm
    implementation(libs.realm.android.adapters)
    implementation(libs.realm.android.library)
    kapt(libs.realm.annotations.processor)

    // rxjava
    implementation(libs.rxandroid)
    implementation(libs.rxjava)
    implementation(libs.rxkotlin)
    implementation(libs.rxdogtag)
    implementation(libs.rxdogtag.autodispose)

    // testing
    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.0-alpha3") {
        exclude(mapOf("group" to "com.android.support", "module" to "support-annotations"))
    }
    androidTestImplementation(libs.mockito.android)
    testImplementation(libs.androidx.runner)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)

    // moshi
    implementation(libs.moshi)
    debugImplementation(libs.moshi.kotlin)
    kapt(libs.moshi.kotlin.codegen)

    // coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.rx2)
    implementation(libs.kotlinx.coroutines.reactive)

    implementation(libs.photoview)
    implementation(libs.rx.preferences)
    implementation(libs.flexbox)
    implementation(libs.timber)
    implementation(libs.moshi.kotlin)
    implementation(libs.shortcutbadger)
    implementation(libs.kotlin.stdlib.jdk7)
    implementation(project(":android-smsmms"))
    implementation(project(":common"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":app"))
}
