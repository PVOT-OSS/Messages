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

import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("realm-android")   // must come before Kotlin plugins
    alias(libs.plugins.kotlin.android)
    id("kotlin-android-extensions")
    id("kotlin-kapt")
}

android {
    namespace = "dev.danascape.messages"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.danascape.messages"
        minSdk = 23
        targetSdk = 33

        versionCode = 1
        versionName = "1.0.0"

        setProperty("archivesBaseName", "Messages-v${versionName}")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // use the existing debug signing config (default created by Android Gradle plugin)
        // create release signing config
        create("release") {
            val keystoreProps = Properties()
            val keystorePropsFile = rootProject.file("./.gradle/.gradlerc")
            storeFile = file("./my-release-key.keystore")
            keyAlias = "quik_release"
            if (keystorePropsFile.exists()) {
                keystoreProps.load(FileInputStream(keystorePropsFile))
            }
            storePassword = keystoreProps["storePassword"]?.toString()
            keyPassword = keystoreProps["keyPassword"]?.toString()
        }
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs and AABs.
        includeInApk = false
        includeInBundle = false
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // kotlinOptions inside android block (Kotlin DSL)
    kotlinOptions {
        jvmTarget = "1.8"
    }

    lint {
        abortOnError = false
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    // override signing credentials when running on CI
    if (System.getenv("CI") == "true") {
        signingConfigs.getByName("release").storePassword = System.getenv("keystore_password")
        signingConfigs.getByName("release").keyAlias = System.getenv("key_alias")
        signingConfigs.getByName("release").keyPassword = System.getenv("key_password")
    }
}

androidExtensions {
    isExperimental = true
}

configurations {
    create("debug")
    create("release")
}

dependencies {
    // lifecycle
    implementation("androidx.lifecycle:lifecycle-extensions:2.1.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.1.0")

    // androidx
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.emoji2:emoji2-bundled:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation(libs.androidx.ktx)
    implementation("androidx.viewpager2:viewpager2:1.0.0-beta05")
    implementation("com.google.android.material:material:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.8.0")

    // conductor
    implementation("com.bluelinelabs:conductor:2.1.5")
    implementation("com.bluelinelabs:conductor-archlifecycle:2.1.5")

    // glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // exoplayer
    implementation("com.github.google.ExoPlayer:exoplayer-core:r2.9.0")
    implementation("com.github.google.ExoPlayer:exoplayer-ui:r2.9.0") {
        // Kotlin-safe exclude
        exclude(mapOf("group" to "com.android.support", "module" to "support-media-compat"))
    }

    // rxbinding
    implementation("com.jakewharton.rxbinding2:rxbinding-kotlin:2.0.0")
    implementation("com.jakewharton.rxbinding2:rxbinding-support-v4-kotlin:2.0.0")

    // autodispose
    implementation("com.uber.autodispose:autodispose-android-archcomponents:1.3.0")
    implementation("com.uber.autodispose:autodispose-android-archcomponents-test:1.3.0")
    implementation("com.uber.autodispose:autodispose-android:1.3.0")
    implementation("com.uber.autodispose:autodispose:1.3.0")
    implementation("com.uber.autodispose:autodispose-lifecycle:1.3.0")

    // dagger
    implementation("com.google.dagger:dagger:2.18")
    implementation("com.google.dagger:dagger-android-support:2.18")
    kapt("com.google.dagger:dagger-compiler:2.18")
    kapt("com.google.dagger:dagger-android-processor:2.18")
    // Resolve jdk8+ Generation Annotations - javax annotation does not exist
    implementation("com.github.pengrad:jdk9-deps:1ffe84c468")
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    // ezvcard (multiple excludes)
    implementation("com.googlecode.ez-vcard:ez-vcard:0.10.4") {
        exclude(mapOf("group" to "org.jsoup", "module" to "jsoup"))
        exclude(mapOf("group" to "org.freemarker", "module" to "freemarker"))
        exclude(mapOf("group" to "com.fasterxml.jackson.core", "module" to "jackson-core"))
    }

    // realm
    implementation("com.github.realm:realm-android-adapters:3.1.0")
    kapt("io.realm:realm-annotations:10.15.0")
    kapt("io.realm:realm-annotations-processor:10.15.0")

    // rxjava
    implementation("io.reactivex.rxjava2:rxandroid:2.0.1")
    implementation("io.reactivex.rxjava2:rxjava:2.1.4")
    implementation("io.reactivex.rxjava2:rxkotlin:2.1.0")
    implementation("com.uber.rxdogtag:rxdogtag:0.2.0")
    implementation("com.uber.rxdogtag:rxdogtag-autodispose:0.2.0")

    // testing
    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.0-alpha3") {
        exclude(mapOf("group" to "com.android.support", "module" to "support-annotations"))
    }
    androidTestImplementation("org.mockito:mockito-android:2.18.3")
    testImplementation("androidx.test:runner:1.1.0-alpha3")
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:2.18.3")

    // moshi
    implementation("com.squareup.moshi:moshi:1.8.0")
    debugImplementation("com.squareup.moshi:moshi-kotlin:1.8.0")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.8.0")

    // coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:1.4.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.4.3")

    implementation("com.github.chrisbanes:photoview:2.1.4")
    implementation("com.f2prateek.rx.preferences2:rx-preferences:2.0.0-RC3")
    implementation("com.github.google:flexbox-layout:0.3.1")
    implementation(libs.timber)
    implementation("com.squareup.moshi:moshi-kotlin:1.8.0")
    implementation("me.leolin:ShortcutBadger:1.1.22")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.21")
    implementation(project(":android-smsmms"))
    implementation(project(":common"))
    implementation(project(":data"))
    implementation(project(":domain"))
}