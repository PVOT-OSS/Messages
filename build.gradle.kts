/*
 * SPDX-FileCopyrightText: 2025-2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import com.android.build.gradle.BaseExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Needed until we upstream
buildscript {
    dependencies {
        classpath(libs.realm.gradle.plugin)
        classpath(libs.firebase.crashlytics.gradle)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.services) apply false
}

tasks.register<Delete>("clean") { delete(rootProject.layout.buildDirectory) }

subprojects {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    plugins.withId("com.android.application") {
        extensions.configure(BaseExtension::class.java) {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
    plugins.withId("com.android.library") {
        extensions.configure(BaseExtension::class.java) {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}
