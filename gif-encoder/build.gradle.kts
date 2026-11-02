/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

plugins { alias(libs.plugins.pvot.android.library) }

android {
    namespace = "com.bumptech.glide.gifencoder"

    defaultConfig { minSdk = 24 }

    lint { abortOnError = false }
}

dependencies {
    implementation(libs.androidx.ktx)
    implementation(libs.timber)
}
