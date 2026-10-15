/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

plugins {
    alias(libs.plugins.android.application)
    id("realm-android") // must come before Kotlin plugins
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "org.prauga.messages"
    compileSdk { version = release(36) }

    defaultConfig {
        applicationId = "org.prauga.messages"
        minSdk = 23
        targetSdk = 36

        versionCode = 9
        versionName = "1.0.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    extensions
        .getByType(BasePluginExtension::class.java)
        .archivesName
        .set("Messages-v${defaultConfig.versionName}")

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
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("platform")
        }
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs and AABs.
        includeInApk = false
        includeInBundle = false
    }

    lint { abortOnError = false }
}

dependencies {
    // lifecycle
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.common.java8)

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

    // media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.common)

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
    implementation(project(":carrier-configs"))
    implementation(project(":common"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":app"))
    implementation(project(":otp-detector"))
}
