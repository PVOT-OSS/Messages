import com.android.build.gradle.BaseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Needed until we upstream
buildscript {
    dependencies {
        classpath("io.realm:realm-gradle-plugin:10.19.0")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.5.2")
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
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "17"
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