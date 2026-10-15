/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven {
            name = "glide-snapshot"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }
        maven("https://maven.google.com")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven {
            name = "glide-snapshot"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }
        maven("https://maven.google.com")
    }
}

rootProject.name = "Messages"

include(":presentation")

include(":android-smsmms")

include(":domain")

include(":data")

include(":common")

include(":app")

include(":carrier-configs")

include(":gif-encoder")

include(":otp-detector")
