/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

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
    alias(libs.plugins.legacy.kapt) apply false
    alias(libs.plugins.google.services) apply false
}

tasks.register<Delete>("clean") { delete(rootProject.layout.buildDirectory) }
