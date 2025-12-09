import org.jetbrains.kotlin.gradle.plugin.KaptExtension

// Needed until we upstream
buildscript {
    dependencies {
        classpath("io.realm:realm-gradle-plugin:10.19.0")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.8.1")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.services) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    afterEvaluate {
        val android = extensions.findByType(com.android.build.gradle.BaseExtension::class.java)
        if (android != null) {
            android.compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}
